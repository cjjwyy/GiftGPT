package com.giftgpt.recommendation.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftgpt.common.ai.DeepseekClient;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.goods.entity.Product;
import com.giftgpt.goods.mapper.ProductMapper;
import com.giftgpt.goods.service.CommerceService;
import com.giftgpt.recommendation.dto.AiGift;
import com.giftgpt.recommendation.dto.AiGiftsResponse;
import com.giftgpt.recommendation.dto.MatchRequest;
import com.giftgpt.recommendation.dto.PersonalitySnapshot;
import com.giftgpt.recommendation.dto.RecommendFeedbackRequest;
import com.giftgpt.recommendation.dto.RecommendEventRequest;
import com.giftgpt.recommendation.dto.RecommendItem;
import com.giftgpt.recommendation.dto.RecommendRequest;
import com.giftgpt.recommendation.dto.RecommendResponse;
import com.giftgpt.recommendation.entity.RecommendationHistory;
import com.giftgpt.recommendation.entity.RecommendEvent;
import com.giftgpt.recommendation.mapper.RecommendEventMapper;
import com.giftgpt.recommendation.mapper.RecommendationHistoryMapper;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.entity.RecipientTag;
import com.giftgpt.user.mapper.RecipientMapper;
import com.giftgpt.user.mapper.RecipientTagMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecipientMapper recipientMapper;
    private final RecipientTagMapper recipientTagMapper;
    private final RecommendationHistoryMapper historyMapper;
    private final ProductMapper productMapper;
    private final CommerceService commerceService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final KeywordGraphService keywordGraphService;
    private final DeepseekClient deepseekClient;

    @Autowired(required = false)
    private RecommendEventMapper recommendEventMapper;

    private static final AtomicInteger SEARCH_THREAD_ID = new AtomicInteger();
    private final ExecutorService productSearchExecutor = Executors.newFixedThreadPool(6, runnable -> {
        Thread thread = new Thread(runnable, "gift-search-" + SEARCH_THREAD_ID.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = "你是一位温暖细腻的礼物推荐AI助手。你总是以JSON格式回复，不添加任何额外的解释或markdown标记。你推荐的礼物贴近生活、实用且有情感价值，语言柔和温暖，善于用收礼人的称谓让每份推荐都更有温度。";

    @PreDestroy
    public void shutdownSearchExecutor() {
        productSearchExecutor.shutdownNow();
    }

    /** 哪些标签带补充项由画像标注数据决定，后端不预设固定标签集合。 */

    // ---------- AI Gift Result DTO ----------

    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    static class AiGiftResult {
        private List<AiGift> gifts;
        private String summary;
    }

    // ---------- Core Logic ----------

    // ---------- Core Logic (3 steps) ----------

    /** Step 1: analyze the recipient's personality from their profile + tags. */
    public PersonalitySnapshot analyze(Long recipientId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Recipient recipient = loadOwnRecipient(recipientId, userId);
        List<RecipientTag> tags = recipientTagMapper.selectList(
                new LambdaQueryWrapper<RecipientTag>().eq(RecipientTag::getRecipientId, recipient.getId()));
        List<String> tagNames = tags.stream().map(RecipientTag::getTagName).collect(Collectors.toList());
        Map<String, List<String>> tagSupplements = loadTagSupplements(recipient.getId());

        PersonalitySnapshot snapshot = new PersonalitySnapshot();
        snapshot.setRecipientId(recipient.getId());
        snapshot.setName(recipient.getName());
        snapshot.setRelation(recipient.getRelation());
        snapshot.setGender(recipient.getGender());
        snapshot.setAgeRange(recipient.getAgeRange());
        snapshot.setMbti(recipient.getMbti());
        snapshot.setPersonality(recipient.getPersonality());
        snapshot.setTags(tagNames);
        snapshot.setTagSupplements(tagSupplements);
        snapshot.setRecentPurchases(recipient.getRecentPurchases());
        snapshot.setNote(recipient.getNote());
        snapshot.setAnalysis(buildAnalysis(recipient, tagNames, tagSupplements));
        return snapshot;
    }

    /** Step 2: 根据收礼人画像生成按权重排序的搜索关键词（LLM 优先，失败回退关键词图谱）。 */
    public AiGiftsResponse generateAiGifts(RecommendRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Recipient recipient = loadOwnRecipient(request.getRecipientId(), userId);
        List<RecipientTag> tags = recipientTagMapper.selectList(
                new LambdaQueryWrapper<RecipientTag>().eq(RecipientTag::getRecipientId, recipient.getId()));
        List<String> tagNames = tags.stream().map(RecipientTag::getTagName).collect(Collectors.toList());
        Map<String, List<String>> tagSupplements = loadTagSupplements(recipient.getId());

        AiGiftsResponse resp = new AiGiftsResponse();
        List<KeywordGraphService.KeywordHit> graphHits = keywordGraphService.pickKeywords(
                recipient, tagNames, request.getOccasion());
        try {
            // KG 上下文注入：标签 → 品类，约束 LLM 联想范围
            Map<String, List<String>> tagToCat = knowledgeGraphService.getTagCategoryNames(tagNames);
            String prompt = buildPrompt(recipient, tagNames, tagSupplements,
                    request.getOccasion(), request.getBudget(), request.getExtraNote(), tagToCat);
            prompt += keywordGraphService.promptContext(graphHits);
            String content = deepseekClient.chat(SYSTEM_PROMPT, prompt, 4096);
            AiGiftResult aiResult = parseAiGiftsJson(content);
            List<AiGift> normalized = normalizeAiGifts(aiResult.getGifts(), request.getBudget());
            normalized = filterAiGiftsBySupplements(normalized, tagSupplements);
            for (AiGift gift : normalized) {
                KeywordGraphService.KeywordHit hit = keywordGraphService.match(gift.getName(), graphHits);
                gift.setWeight(hit == null ? 0 : hit.getWeight());
                if (hit != null && !hit.explanation().isBlank()) gift.setReason(abbreviate(hit.explanation(), 120));
            }
            normalized.sort(Comparator.comparingDouble(AiGift::getWeight).reversed());
            if (!normalized.isEmpty()) {
                resp.setGifts(normalized);
                resp.setSummary(safeSummary(aiResult.getSummary(), "已结合画像、预算与知识图谱生成候选礼物"));
                resp.setAiGenerated(true);
                resp.setFallbackUsed(false);
                return resp;
            }
        } catch (Exception e) {
            log.warn("Deepseek recommendation unavailable, fallback to keyword graph: {}", e.getMessage());
        }
        // 兜底：关键词图谱（LLM 未配置/超时/解析失败时），场景也参与关键词推导
        List<KeywordGraphService.KeywordHit> hits = graphHits;
        List<AiGift> gifts = new ArrayList<>();
        for (KeywordGraphService.KeywordHit hit : hits.stream().limit(8).collect(Collectors.toList())) {
            AiGift g = new AiGift();
            g.setName(hit.getKeyword());
            g.setPrice(0);
            g.setReason(abbreviate(hit.explanation().isBlank() ? "按关键词「" + hit.getKeyword() + "」搜索商品" : hit.explanation(), 120));
            g.setTags(new ArrayList<>());
            g.setPlatform("拼多多");
            g.setWeight(hit.getWeight());
            gifts.add(g);
        }
        if (gifts.isEmpty()) {
            gifts.addAll(fallbackAiGifts(request, tagNames));
        }
        resp.setGifts(gifts);
        resp.setSummary(gifts.isEmpty()
                ? "当前预算内没有可用候选，请适当提高预算或补充画像"
                : "AI 服务暂不可用，已降级为本地画像与关键词图谱推荐");
        resp.setAiGenerated(false);
        resp.setFallbackUsed(true);
        return resp;
    }

    /** Step 3: search shopping platforms for each AI gift and assemble the final recommendation. */
    public RecommendResponse matchAndSearch(MatchRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Recipient recipient = loadOwnRecipient(request.getRecipientId(), userId);
        String occasionLabel = translateOccasion(request.getOccasion());
        Map<String, List<String>> tagSupplements = loadTagSupplements(recipient.getId());

        List<RecommendItem> items = new ArrayList<>();
        List<AiGift> gifts = request.getGifts() != null ? request.getGifts() : new ArrayList<>();

        // 关键词搜索：每个关键词独立去拼多多搜索，按关键词权重排序
        List<CompletableFuture<RecommendItem>> futures = gifts.stream()
                .limit(8)
                .map(gift -> CompletableFuture
                        .supplyAsync(() -> buildItemFromAiGift(gift, request.getBudget()), productSearchExecutor)
                        .completeOnTimeout(null, 15, TimeUnit.SECONDS)
                        .exceptionally(error -> {
                            log.warn("Build item failed: {}", error.getMessage());
                            return null;
                        }))
                .collect(Collectors.toList());
        if (!futures.isEmpty()) {
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(16, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Product search batch timed out: {}", e.getMessage());
            }
            futures.stream().filter(CompletableFuture::isDone)
                    .map(future -> future.getNow(null))
                    .filter(item -> item != null)
                    .forEach(items::add);
        }

        // 排序：关键词权重高者优先，其次按匹配分数
        items.sort((a, b) -> {
            int cw = Double.compare(b.getKeywordWeight(), a.getKeywordWeight());
            if (cw != 0) return cw;
            double sa = a.getScore() != null ? a.getScore() : 0;
            double sb = b.getScore() != null ? b.getScore() : 0;
            return Double.compare(sb, sa);
        });

        // KG 增强通道：Neo4j 双通道结果合并（按 productId 去重，LLM 结果优先）
        List<RecommendItem> kgItems = knowledgeGraphService.queryRecommendations(
                recipient.getId(), request.getOccasion(), request.getBudget());
        boolean kgEnhanced = !kgItems.isEmpty();
        if (!kgItems.isEmpty()) {
            Set<Long> seen = items.stream()
                    .map(RecommendItem::getProductId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet());
            for (RecommendItem ki : kgItems) {
                if (ki.getProductId() == null || seen.add(ki.getProductId())) {
                    items.add(ki);
                }
            }
        }

        List<String> profileTags = recipientTagMapper.selectList(new LambdaQueryWrapper<RecipientTag>()
                .eq(RecipientTag::getRecipientId, recipient.getId())).stream()
                .map(RecipientTag::getTagName).collect(Collectors.toList());
        List<KeywordGraphService.KeywordHit> evidence = keywordGraphService.pickKeywords(
                recipient, profileTags, request.getOccasion());
        for (RecommendItem item : items) {
            KeywordGraphService.KeywordHit hit = keywordGraphService.match(item.getProductName(), evidence);
            // Ignore caller-supplied graph weight; evidence is recomputed from the owned profile.
            item.setKeywordWeight(hit == null ? 0 : hit.getWeight());
            double graphScore = hit == null ? 0 : Math.min(1, hit.getWeight() / 300.0);
            double baseScore = item.getScore() == null ? 0 : item.getScore();
            item.setScore(.75 * baseScore + .25 * graphScore);
            List<RecommendItem.ScoreFactor> factors = new ArrayList<>();
            factors.add(buildScoreFactor("商品基础匹配", .75, baseScore));
            factors.add(buildScoreFactor("知识图谱关联", .25, graphScore));
            item.setScoreFactors(factors);
            if (hit != null && !hit.explanation().isBlank()) {
                item.setReason(hit.explanation());
                item.setReasoningChain(hit.evidenceChain());
                kgEnhanced = true;
            }
        }
        items = postProcessItems(items, tagSupplements, request.getBudget());
        kgEnhanced = items.stream().anyMatch(item -> item.getKeywordWeight() > 0 || "kg".equals(item.getSource()));

        RecommendResponse response = new RecommendResponse();
        response.setRecipientId(recipient.getId());
        response.setRecipientName(recipient.getName());
        response.setOccasion(request.getOccasion());
        response.setBudget(request.getBudget());
        response.setItems(items);
        response.setFallbackUsed(Boolean.TRUE.equals(request.getFallbackUsed()));
        response.setKgEnhanced(kgEnhanced);
        response.setSummary(request.getSummary() != null && !request.getSummary().isBlank() ? request.getSummary()
                : "根据" + recipient.getName() + "的特征，在" + occasionLabel + "场景下为您推荐以下礼物");

        saveHistory(userId, recipient.getId(), request.getOccasion(), request.getBudget(), response);
        return response;
    }

    /** Backward-compatible single call: runs all 3 steps. */
    public RecommendResponse search(RecommendRequest request) {
        AiGiftsResponse ai = generateAiGifts(request);
        MatchRequest matchReq = new MatchRequest();
        matchReq.setRecipientId(request.getRecipientId());
        matchReq.setOccasion(request.getOccasion());
        matchReq.setBudget(request.getBudget());
        matchReq.setExtraNote(request.getExtraNote());
        matchReq.setGifts(ai.getGifts());
        matchReq.setSummary(ai.getSummary());
        matchReq.setFallbackUsed(ai.getFallbackUsed());
        return matchAndSearch(matchReq);
    }

    private Recipient loadOwnRecipient(Long recipientId, Long userId) {
        Recipient recipient = recipientMapper.selectById(recipientId);
        if (recipient == null || !recipient.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RECIPIENT_NOT_FOUND);
        }
        return recipient;
    }

    private String buildAnalysis(Recipient recipient, List<String> tags, Map<String, List<String>> tagSupplements) {
        StringBuilder sb = new StringBuilder();
        if (recipient.getMbti() != null && !recipient.getMbti().isBlank()) {
            sb.append("MBTI ").append(recipient.getMbti()).append("，");
        }
        if (recipient.getPersonality() != null && !recipient.getPersonality().isBlank()) {
            sb.append(recipient.getPersonality()).append("；");
        }
        if (!tags.isEmpty()) {
            sb.append("兴趣标签：").append(String.join("、", tags)).append("。");
        }
        String supplementText = formatTagSupplements(tagSupplements);
        if (!supplementText.isEmpty()) {
            sb.append("标签补充项：").append(supplementText).append("。");
        }
        if (recipient.getRelation() != null) {
            sb.append("关系：").append(recipient.getRelation()).append("。");
        }
        if (sb.length() == 0) sb.append("画像信息较少，将基于场景与预算综合推荐。");
        return sb.toString();
    }

    /** Build a RecommendItem from an AI gift: try live platform search, then local DB, then AI hint. */
    private RecommendItem buildItemFromAiGift(AiGift gi, BigDecimal budget) {
        RecommendItem item = new RecommendItem();
        item.setProductName(gi.getName());
        item.setPrice(BigDecimal.valueOf(gi.getPrice()));
        String reason = gi.getReason();
        if (reason == null || reason.isBlank()) {
            reason = "送TA这份精选好物";
        }
        item.setReason(reason);
        item.setMatchTags(gi.getTags());
        item.setKeywordWeight(gi.getWeight());

        Product matched = searchPlatformForGift(gi);
        boolean liveMatch = matched != null;
        if (matched == null) {
            matched = matchToRealProduct(gi);
        }
        if (matched != null) {
            item.setProductId(matched.getId());
            item.setProductName(matched.getName());
            item.setImageUrl(matched.getImageUrl() != null ? matched.getImageUrl() : "");
            item.setPlatform(matched.getPlatform() != null ? matched.getPlatform() : "拼多多");
            String url = matched.getPlatformUrl();
            if (url == null || url.isBlank()) {
                url = "https://mobile.yangkeduo.com/search_result.html?search_key="
                        + URLEncoder.encode(gi.getName(), StandardCharsets.UTF_8);
            }
            item.setPlatformUrl(url);
            if (matched.getPrice() != null) {
                item.setPrice(matched.getPrice());
            }
            double kw = keywordScore(matched.getName(), gi.getName());
            double fit = priceFit(matched.getPrice(), budget);
            double sales = salesScore(matched);
            double score = 0.50 * kw + 0.30 * fit + 0.20 * sales;
            item.setScore(Math.min(1.0, Math.max(0.0, score)));
            item.setSource(liveMatch ? "pdd" : "local");
            item.setScoreFactors(List.of(
                    buildScoreFactor("兴趣/关键词命中", 0.50, kw),
                    buildScoreFactor("价格贴近预算", 0.30, fit),
                    buildScoreFactor("销量热度", 0.20, sales)));
        } else {
            if (gi.getPrice() <= 0) return null;
            item.setProductId(-1L);
            item.setImageUrl("");
            item.setPlatform("拼多多");
            item.setPlatformUrl("https://mobile.yangkeduo.com/search_result.html?search_key="
                    + URLEncoder.encode(gi.getName(), StandardCharsets.UTF_8));
            item.setScore(0.50);
            item.setSource("ai_fallback");
        }
        return item;
    }

    private RecommendItem.ScoreFactor buildScoreFactor(String label, double weight, double score) {
        RecommendItem.ScoreFactor factor = new RecommendItem.ScoreFactor();
        factor.setLabel(label);
        factor.setWeight(weight);
        factor.setScore(Math.min(1.0, Math.max(0.0, score)));
        return factor;
    }

    private String stripParentheses(String text) {
        return text == null ? "" : text.replaceAll("[（(].*?[）)]", "").trim();
    }

    private double keywordScore(String productName, String giftName) {
        String name = productName == null ? "" : productName.toLowerCase();
        String gift = stripParentheses(giftName).toLowerCase();
        String[] kws = gift.split("[\\s,，、]+");
        int hit = 0;
        for (String k : kws) {
            if (k.length() >= 2 && name.contains(k)) {
                hit++;
            }
        }
        return kws.length == 0 ? 0.0 : Math.min(1.0, (double) hit / kws.length);
    }

    private double priceFit(BigDecimal productPrice, BigDecimal budget) {
        if (productPrice == null || budget == null || budget.doubleValue() <= 0) {
            return 0.5;
        }
        double diff = Math.abs(productPrice.doubleValue() - budget.doubleValue());
        return Math.max(0.0, 1.0 - diff / budget.doubleValue());
    }

    private double salesScore(Product p) {
        int sales = p == null || p.getSalesCount() == null ? 0 : p.getSalesCount();
        return Math.min(1.0, sales / 10000.0);
    }

    /** Live platform search: look up the AI gift name on Pinduoduo. */
    private Product searchPlatformForGift(AiGift gi) {
        String keyword = gi.getName().replaceAll("[（(].*?[）)]", "").trim();
        if (keyword.isBlank()) return null;
        List<Product> found = commerceService.searchAcrossPlatforms(keyword, 1, 5);
        if (found == null || found.isEmpty()) return null;
        Product best = pickBestMatch(found, keyword);
        if (best != null) {
            log.info("Platform match '{}' → '{}' on {}", gi.getName(), best.getName(), best.getPlatform());
        }
        return best;
    }

    private Product pickBestMatch(List<Product> candidates, String keyword) {
        String[] kws = keyword.toLowerCase().split("[\\s,，、]+");
        Product best = null;
        int bestScore = -1;
        for (Product p : candidates) {
            String name = p.getName() == null ? "" : p.getName().toLowerCase();
            int score = 0;
            for (String k : kws) {
                if (k.length() >= 2 && name.contains(k)) score += k.length();
            }
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return bestScore > 0 ? best : (candidates.isEmpty() ? null : candidates.get(0));
    }

    private List<AiGift> fallbackAiGifts(RecommendRequest request, List<String> tags) {
        List<AiGift> gifts = new ArrayList<>();
        if (tags.contains("文艺") || tags.contains("文学")) gifts.add(mockGift("手写羊皮卷情书定制礼盒", 129, "结合TA的文艺气质，手写体+复古羊皮纸营造仪式感", "拼多多"));
        if (tags.contains("摄影") || tags.contains("户外")) gifts.add(mockGift("富士拍立得instax mini 12", 459, "即时记录旅行瞬间，与TA的摄影+户外属性完美契合", "拼多多"));
        if (tags.contains("极客") || tags.contains("科技")) gifts.add(mockGift("机械键盘定制键帽套装", 299, "极客属性标配，可自定义配色方案", "拼多多"));
        if (tags.contains("养生") || tags.contains("健康")) gifts.add(mockGift("智能温控泡脚桶", 199, "养生派首选，智能恒温+多档按摩", "拼多多"));
        if (tags.contains("音乐") || tags.contains("艺术")) gifts.add(mockGift("黑胶唱片装饰灯", 168, "音乐美学二合一", "拼多多"));
        gifts.add(mockGift("永生花音乐盒礼盒", 239, "经典浪漫之选", "拼多多"));
        gifts.add(mockGift("北欧极简香薰蜡烛礼盒", 89, "营造温馨氛围", "拼多多"));
        return gifts.stream()
                .filter(g -> BigDecimal.valueOf(g.getPrice()).compareTo(request.getBudget()) <= 0)
                .collect(Collectors.toList());
    }

    private AiGift mockGift(String name, double price, String reason, String platform) {
        AiGift g = new AiGift();
        g.setName(name);
        g.setPrice(price);
        g.setReason(reason);
        g.setTags(new ArrayList<>());
        g.setPlatform(platform);
        return g;
    }

    /** 从 recipient_tag.supplement 读取有补充项标签的具体补充项 */
    private Map<String, List<String>> loadTagSupplements(Long recipientId) {
        List<RecipientTag> tags = recipientTagMapper.selectList(
                new LambdaQueryWrapper<RecipientTag>().eq(RecipientTag::getRecipientId, recipientId));
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (RecipientTag tag : tags) {
            if (tag.getSupplement() == null || tag.getSupplement().isBlank()) continue;
            List<String> items = Arrays.stream(tag.getSupplement().split("[、,，;；]"))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .collect(Collectors.toList());
            if (!items.isEmpty()) result.put(tag.getTagName(), items);
        }
        return result;
    }

    private String formatTagSupplements(Map<String, List<String>> tagSupplements) {
        if (tagSupplements == null || tagSupplements.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        tagSupplements.forEach((tag, items) ->
                parts.add(tag + "：" + String.join("、", items)));
        return String.join("；", parts);
    }

    private List<String> flattenSupplementValues(Map<String, List<String>> tagSupplements) {
        List<String> values = new ArrayList<>();
        if (tagSupplements == null) return values;
        for (List<String> items : tagSupplements.values()) {
            if (items != null) values.addAll(items);
        }
        return values;
    }

    private boolean containsAnyKeyword(String text, List<String> keywords) {
        if (text == null || text.isBlank() || keywords == null || keywords.isEmpty()) return false;
        String lower = text.toLowerCase();
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private List<AiGift> normalizeAiGifts(List<AiGift> gifts, BigDecimal budget) {
        if (gifts == null || gifts.isEmpty()) return new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        List<AiGift> normalized = new ArrayList<>();
        for (AiGift gift : gifts) {
            if (gift == null || gift.getName() == null || gift.getName().isBlank()
                    || gift.getPrice() <= 0 || !Double.isFinite(gift.getPrice())) {
                continue;
            }
            BigDecimal price = BigDecimal.valueOf(gift.getPrice());
            if (budget != null && price.compareTo(budget) > 0) continue;
            String name = abbreviate(gift.getName().trim(), 120);
            String key = name.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) continue;
            gift.setName(name);
            gift.setReason(abbreviate(gift.getReason() == null ? "" : gift.getReason().trim(), 120));
            gift.setPlatform("拼多多");
            gift.setWeight(gift.getWeight() > 0 && Double.isFinite(gift.getWeight()) ? gift.getWeight() : 1.0);
            if (gift.getTags() == null) {
                gift.setTags(new ArrayList<>());
            } else {
                gift.setTags(gift.getTags().stream()
                        .filter(tag -> tag != null && !tag.isBlank())
                        .map(String::trim)
                        .distinct()
                        .limit(8)
                        .collect(Collectors.toList()));
            }
            normalized.add(gift);
            if (normalized.size() == 8) break;
        }
        return normalized;
    }

    private String safeSummary(String summary, String fallback) {
        if (summary == null || summary.isBlank()) return fallback;
        return abbreviate(summary.trim(), 200);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    /** AI 候选礼物按补充项过滤：只保留名称/理由/匹配点中出现补充项的礼物 */
    private List<AiGift> filterAiGiftsBySupplements(List<AiGift> gifts, Map<String, List<String>> tagSupplements) {
        List<String> keywords = flattenSupplementValues(tagSupplements);
        if (gifts == null) return new ArrayList<>();
        if (keywords.isEmpty()) return gifts;
        return gifts.stream().filter(g -> {
            String text = String.join(" ",
                    g.getName() == null ? "" : g.getName(),
                    g.getReason() == null ? "" : g.getReason(),
                    g.getTags() == null ? "" : String.join(" ", g.getTags()));
            return containsAnyKeyword(text, keywords);
        }).collect(Collectors.toList());
    }

    /** 最终推荐商品按补充项过滤：只保留商品名/推荐理由/匹配标签中出现补充项的商品 */
    private List<RecommendItem> filterItemsBySupplements(List<RecommendItem> items, Map<String, List<String>> tagSupplements) {
        List<String> keywords = flattenSupplementValues(tagSupplements);
        if (items == null) return new ArrayList<>();
        if (keywords.isEmpty()) return items;
        return items.stream().filter(item -> {
            String text = String.join(" ",
                    item.getProductName() == null ? "" : item.getProductName(),
                    item.getReason() == null ? "" : item.getReason(),
                    item.getMatchTags() == null ? "" : String.join(" ", item.getMatchTags()));
            return containsAnyKeyword(text, keywords);
        }).collect(Collectors.toList());
    }

    private List<RecommendItem> postProcessItems(List<RecommendItem> items,
                                                 Map<String, List<String>> tagSupplements,
                                                 BigDecimal budget) {
        List<RecommendItem> filtered = filterItemsBySupplements(items, tagSupplements);
        Map<String, RecommendItem> unique = new LinkedHashMap<>();
        for (RecommendItem item : filtered) {
            if (item == null || item.getProductName() == null || item.getProductName().isBlank()
                    || item.getPrice() == null || item.getPrice().signum() <= 0) {
                continue;
            }
            if (budget != null && item.getPrice().compareTo(budget) > 0) continue;
            String key = item.getProductId() != null && item.getProductId() > 0
                    ? "id:" + item.getProductId()
                    : "name:" + (item.getPlatform() == null ? "" : item.getPlatform()) + ":"
                    + item.getProductName().trim().toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, item);
        }
        return unique.values().stream()
                .sorted(Comparator
                        .comparing(RecommendItem::getScore,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RecommendItem::getKeywordWeight, Comparator.reverseOrder())
                        .thenComparing(RecommendItem::getProductName))
                .limit(8)
                .collect(Collectors.toList());
    }

    private void saveHistory(Long userId, Long recipientId, String occasion,
                             BigDecimal budget, RecommendResponse response) {
        RecommendationHistory history = new RecommendationHistory();
        history.setUserId(userId);
        history.setRecipientId(recipientId);
        history.setScene(occasion);
        history.setBudget(budget);
        history.setResult(compress(JSONUtil.toJsonStr(response)));
        historyMapper.insert(history);
    }

    private static String compress(String data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data.getBytes(StandardCharsets.UTF_8));
            gzip.finish();
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            log.warn("Compression failed, storing raw JSON", e);
            return data;
        }
    }

    private static String decompress(String compressed) {
        if (compressed == null || compressed.isEmpty()) return null;
        // If not Base64-encoded GZIP (old data), return as-is
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(compressed);
        } catch (IllegalArgumentException e) {
            return compressed;
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8);
             StringWriter writer = new StringWriter()) {
            reader.transferTo(writer);
            return writer.toString();
        } catch (IOException e) {
            log.warn("Decompression failed", e);
            return compressed;
        }
    }

    // ---------- Prompt Engineering ----------

    private String buildPrompt(Recipient recipient, List<String> tags, Map<String, List<String>> tagSupplements,
                               String occasion, BigDecimal budget, String extraNote,
                               Map<String, List<String>> tagToCat) {
        String tagStr = tags.isEmpty() ? "暂无标签" : String.join("、", tags);
        String relationStr = recipient.getRelation() != null ? recipient.getRelation() : "未指定";
        String genderStr;
        if (recipient.getGender() == null || recipient.getGender() == 0) genderStr = "未知";
        else if (recipient.getGender() == 1) genderStr = "男";
        else genderStr = "女";
        String ageStr = recipient.getAgeRange() != null ? recipient.getAgeRange() : "未知";
        String mbtiStr = recipient.getMbti() != null && !recipient.getMbti().isBlank() ? recipient.getMbti() : "未填写";
        String personalityStr = recipient.getPersonality() != null && !recipient.getPersonality().isBlank() ? recipient.getPersonality() : "未填写";
        String purchasesStr = recipient.getRecentPurchases() != null && !recipient.getRecentPurchases().isBlank() ? recipient.getRecentPurchases() : "未填写";
        String noteStr = recipient.getNote() != null ? recipient.getNote() : "";
        String supplementStr = formatTagSupplements(tagSupplements);
        String extraStr = extraNote != null && !extraNote.isBlank() ? extraNote : "";
        String kgStr = "";
        if (tagToCat != null && !tagToCat.isEmpty()) {
            StringBuilder sb = new StringBuilder("\n【知识图谱品类约束】收礼人兴趣对应的推荐品类如下，礼物应优先从这些品类中选择：\n");
            tagToCat.forEach((t, cats) -> sb.append("- ").append(t).append(" → ").append(String.join("、", cats)).append("\n"));
            kgStr = sb.toString();
        }

        return String.format(
            "你是一位温暖细腻的礼物推荐顾问，擅长从收礼人的全部画像出发，挑选既有情感温度又贴合适用的礼物。\n" +
            "请基于下方收礼人的完整画像，在指定场景和预算内，推荐 5-8 件最合适的礼物。\n" +
            "\n" +
            "【收礼人画像】\n" +
            "- 姓名：%s\n" +
            "- 关系：%s\n" +
            "- 性别：%s\n" +
            "- 年龄段：%s\n" +
            "- MBTI人格：%s\n" +
            "- 性格特点：%s\n" +
            "- 兴趣标签：%s\n" +
            "- 标签补充项：%s\n" +
            "- 最近购买/关注：%s\n" +
            "- 备注：%s\n" +
            "%s" +
            "\n" +
            "【送礼场景】%s\n" +
            "【预算】¥%s（推荐价格应在预算的60%%-100%%之间，不要远低于预算）\n" +
            "%s" +
            "\n" +
            "【挑选原则】\n" +
            "1. 综合考量收礼人的关系、性别、年龄段、MBTI、性格特点、兴趣标签、最近购买/关注、备注等全部画像信息，每件礼物至少与其中 3 项深度契合，避免泛泛之物；\n" +
            "2. 兼顾情感价值与实用性，优先能体现“用心”的礼物，可包含定制款、小众款；\n" +
            "3. 价格应尽量接近预算（在预算的60%%-100%%之间），不要推荐远低于预算的廉价品，也不要超出预算；\n" +
            "4. 参考最近购买/关注，避免重复品类，可做有益补充；\n" +
            "5. 每件标注购买平台为拼多多；\n" +
            "6. 若有 MBTI，按人格特质匹配（如 INTJ 偏好实用工具/高质感，ENFP 偏好创意/体验，ISFJ 偏好温馨实用）；\n" +
            "7. 若某个兴趣标签带有补充项，只推荐与该补充项强相关的礼物，不得推荐不符合补充项的商品（例如音乐-吉他只能推吉他/贝斯相关，运动-羽毛球只能推羽毛球相关）；\n" +
            "8. 推荐理由在100字以内，自然说明性格/兴趣、关系与场景的契合点。只有图谱证据支持的维度才能称为图谱结论；缺失维度不编造。\n" +
            "\n" +
            "严格按以下 JSON 返回（不要 markdown 代码块、不要多余文字）：\n" +
            "{\n" +
            "  \"gifts\": [\n" +
            "    {\n" +
            "      \"name\": \"礼物名称（含品牌/型号，便于搜索）\",\n" +
            "      \"price\": 价格数字,\n" +
            "      \"reason\": \"100字以内，结合有依据的偏好、关系与场景说明适合原因\",\n" +
            "      \"tags\": [\"匹配点1\", \"匹配点2\"],\n" +
            "      \"platform\": \"拼多多\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"summary\": \"一句话总结推荐策略，50 字以内\"\n" +
            "}",
            recipient.getName(), relationStr, genderStr, ageStr, mbtiStr, personalityStr, tagStr,
            supplementStr.isEmpty() ? "暂无" : supplementStr, purchasesStr, noteStr,
            kgStr,
            occasion, budget,
            extraStr.isEmpty() ? "" : "【额外说明】" + extraStr + "\n"
        );
    }

    private String translateOccasion(String occasion) {
        switch (occasion) {
            case "birthday": return "生日";
            case "anniversary": return "纪念日";
            case "valentines": return "情人节";
            case "festival": return "节庆送礼";
            case "graduation": return "毕业";
            case "proposal": return "求婚";
            case "thank_you": return "感谢";
            default: return occasion;
        }
    }

    // ---------- AI Response Parsing ----------

    private AiGiftResult parseAiGiftsJson(String content) throws IOException {
        String json = DeepseekClient.stripMarkdown(content);
        try {
            return objectMapper.readValue(json, AiGiftResult.class);
        } catch (Exception e) {
            log.warn("Direct JSON parse failed, trying to extract first JSON object: {}", e.getMessage());
            int start = json.indexOf('{');
            int endIdx = json.lastIndexOf('}');
            if (start >= 0 && endIdx > start) {
                return objectMapper.readValue(json.substring(start, endIdx + 1), AiGiftResult.class);
            }
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Failed to parse AI gift JSON", e);
        }
    }

    // ---------- History & Feedback ----------

    public Page<RecommendationHistory> history(int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<RecommendationHistory> p = new Page<>(Math.max(1, page), Math.max(1, Math.min(size, 100)));
        Page<RecommendationHistory> result = historyMapper.selectPage(p,
                new LambdaQueryWrapper<RecommendationHistory>()
                        .eq(RecommendationHistory::getUserId, userId)
                        .orderByDesc(RecommendationHistory::getCreateTime));
        // Strip the heavy result field for list view
        result.getRecords().forEach(h -> h.setResult(null));
        return result;
    }

    public RecommendResponse getHistoryDetail(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        RecommendationHistory history = historyMapper.selectById(id);
        if (history == null || !history.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        String json = decompress(history.getResult());
        if (json == null) return null;
        try {
            return JSONUtil.toBean(json, RecommendResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse history detail", e);
            return null;
        }
    }

    public void feedback(Long id, RecommendFeedbackRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        RecommendationHistory history = historyMapper.selectById(id);
        if (history == null || !history.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        history.setFeedback(request.getFeedback());
        historyMapper.updateById(history);
    }

    public void deleteHistories(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        Long userId = StpUtil.getLoginIdAsLong();
        for (Long id : ids.stream().filter(java.util.Objects::nonNull).distinct().limit(100).collect(Collectors.toList())) {
            RecommendationHistory history = historyMapper.selectById(id);
            if (history != null && history.getUserId().equals(userId)) {
                historyMapper.deleteById(id);
            }
        }
    }

    public void trackEvent(RecommendEventRequest request) {
        if (recommendEventMapper == null) return;
        RecommendEvent event = new RecommendEvent();
        event.setUserId(StpUtil.getLoginIdAsLong());
        event.setRecipientId(request.getRecipientId());
        event.setOccasion(abbreviate(request.getOccasion(), 50));
        event.setProductId(request.getProductId());
        event.setProductName(abbreviate(request.getProductName(), 200));
        event.setEventType(request.getEventType());
        event.setCreateTime(java.time.LocalDateTime.now());
        recommendEventMapper.insert(event);
    }

    // ---------- Fallback Mock (used when AI is unavailable) ----------

    private List<RecommendItem> generateMockItems(RecommendRequest request, List<String> tags) {
        List<RecommendItem> items = new ArrayList<>();

        if (tags.contains("文艺") || tags.contains("文学")) {
            items.add(buildItem(1L, "手写羊皮卷情书定制礼盒", new BigDecimal("129.00"),
                    "结合TA的文艺气质，手写体+复古羊皮纸营造仪式感"));
        }
        if (tags.contains("摄影") || tags.contains("户外")) {
            items.add(buildItem(2L, "富士拍立得instax mini 12", new BigDecimal("459.00"),
                    "即时记录旅行瞬间，与TA的摄影+户外属性完美契合"));
        }
        if (tags.contains("极客") || tags.contains("科技")) {
            items.add(buildItem(3L, "机械键盘定制键帽套装", new BigDecimal("299.00"),
                    "极客属性标配，可自定义配色方案"));
        }
        if (tags.contains("养生") || tags.contains("健康")) {
            items.add(buildItem(4L, "智能温控泡脚桶 + 草本足浴包礼盒", new BigDecimal("199.00"),
                    "养生派首选，智能恒温+多档按摩"));
        }
        if (tags.contains("音乐") || tags.contains("艺术")) {
            items.add(buildItem(5L, "黑胶唱片装饰灯 + 定制歌单二维码", new BigDecimal("168.00"),
                    "音乐美学二合一，可扫码听你为TA精选的歌单"));
        }

        items.add(buildItem(10L, "永生花音乐盒礼盒", new BigDecimal("239.00"), "经典浪漫之选"));
        items.add(buildItem(11L, "定制名字925银项链", new BigDecimal("189.00"), "个性化定制"));
        items.add(buildItem(12L, "北欧极简香薰蜡烛礼盒", new BigDecimal("89.00"), "营造温馨氛围"));
        return items.stream()
                .filter(item -> item.getPrice().compareTo(request.getBudget()) <= 0)
                .peek(item -> item.setScore(0.80))
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(8)
                .collect(Collectors.toList());
    }

    private RecommendItem buildItem(Long id, String name, BigDecimal price, String reason) {
        RecommendItem item = new RecommendItem();
        item.setProductId(id);
        item.setProductName(name);
        item.setPrice(price);
        item.setPlatform("拼多多");
        item.setPlatformUrl("https://mobile.yangkeduo.com/search_result.html?search_key=" + name);
        item.setReason(reason);
        item.setImageUrl("");
        return item;
    }

    // ---------- AI-to-Real-Product Matching ----------

    /**
     * Match an AI-suggested gift to the best real product in the local database.
     * Splits the gift name into keywords and searches via LIKE.
     */
    private Product matchToRealProduct(AiGift gi) {
        String keywords = gi.getName().replaceAll("[（(].*?[）)]", "").trim();
        String[] kwArr = keywords.split("[\\s,，、]+");

        // Try all keyword combinations, descending specificity
        for (int len = Math.min(kwArr.length, 4); len >= 2; len--) {
            for (int start = 0; start <= kwArr.length - len; start++) {
                String searchKw = String.join(" ", java.util.Arrays.copyOfRange(kwArr, start, start + len));
                if (searchKw.length() < 3) continue;

                LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .like(Product::getName, searchKw);

                if (gi.getPlatform() != null && !gi.getPlatform().isBlank()) {
                    wrapper.eq(Product::getPlatform, gi.getPlatform());
                }

                wrapper.orderByDesc(Product::getSalesCount);
                List<Product> matches = productMapper.selectList(wrapper);
                if (!matches.isEmpty()) {
                    log.info("Matched '{}' → product '{}' on {}", gi.getName(),
                            matches.get(0).getName(), matches.get(0).getPlatform());
                    return matches.get(0);
                }
            }
        }

        // Broader search: single longest keyword
        String longestKw = java.util.Arrays.stream(kwArr)
                .filter(k -> k.length() >= 2)
                .max(java.util.Comparator.comparingInt(String::length))
                .orElse(keywords);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .like(Product::getName, longestKw);
        if (gi.getPlatform() != null && !gi.getPlatform().isBlank()) {
            wrapper.eq(Product::getPlatform, gi.getPlatform());
        }
        wrapper.orderByDesc(Product::getSalesCount);
        List<Product> matches = productMapper.selectList(wrapper);
        if (!matches.isEmpty()) {
            log.info("Broad match '{}' → product '{}'", gi.getName(), matches.get(0).getName());
            return matches.get(0);
        }

        log.info("No product match found for: {}", gi.getName());
        return null;
    }
}
