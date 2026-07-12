package com.giftgpt.recommendation.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.giftgpt.recommendation.dto.RecommendItem;
import com.giftgpt.recommendation.dto.RecommendRequest;
import com.giftgpt.recommendation.dto.RecommendResponse;
import com.giftgpt.recommendation.entity.RecommendationHistory;
import com.giftgpt.recommendation.mapper.RecommendationHistoryMapper;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.entity.RecipientTag;
import com.giftgpt.user.mapper.RecipientMapper;
import com.giftgpt.user.mapper.RecipientTagMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

    @Value("${giftgpt.ai.deepseek.api-key}")
    private String apiKey;

    @Value("${giftgpt.ai.deepseek.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${giftgpt.ai.deepseek.model:deepseek-chat}")
    private String model;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ---------- DTOs for Deepseek API ----------

    @Data
    static class DeepseekMessage {
        private String role;
        private String content;
    }

    @Data
    static class DeepseekRequest {
        private String model;
        private List<DeepseekMessage> messages;
        private double temperature = 0.7;
        @JsonProperty("max_tokens")
        private int maxTokens = 4096;
    }

    @Data
    static class DeepseekChoice {
        private int index;
        private DeepseekMessage message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    static class DeepseekUsage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }

    @Data
    static class DeepseekResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private List<DeepseekChoice> choices;
        private DeepseekUsage usage;
    }

    // ---------- AI Gift Result DTO ----------

    @Data
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

        PersonalitySnapshot snapshot = new PersonalitySnapshot();
        snapshot.setRecipientId(recipient.getId());
        snapshot.setName(recipient.getName());
        snapshot.setRelation(recipient.getRelation());
        snapshot.setGender(recipient.getGender());
        snapshot.setAgeRange(recipient.getAgeRange());
        snapshot.setMbti(recipient.getMbti());
        snapshot.setPersonality(recipient.getPersonality());
        snapshot.setTags(tagNames);
        snapshot.setRecentPurchases(recipient.getRecentPurchases());
        snapshot.setNote(recipient.getNote());
        snapshot.setAnalysis(buildAnalysis(recipient, tagNames));
        return snapshot;
    }

    /** Step 2: ask the LLM to judge suitable gifts for the recipient. */
    public AiGiftsResponse generateAiGifts(RecommendRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Recipient recipient = loadOwnRecipient(request.getRecipientId(), userId);
        List<RecipientTag> tags = recipientTagMapper.selectList(
                new LambdaQueryWrapper<RecipientTag>().eq(RecipientTag::getRecipientId, recipient.getId()));
        List<String> tagNames = tags.stream().map(RecipientTag::getTagName).collect(Collectors.toList());

        String occasionLabel = translateOccasion(request.getOccasion());
        String prompt = buildPrompt(recipient, tagNames, occasionLabel, request.getBudget(), request.getExtraNote());

        AiGiftsResponse resp = new AiGiftsResponse();
        try {
            AiGiftResult aiResult = callDeepseek(prompt);
            resp.setGifts(aiResult.getGifts());
            resp.setSummary(aiResult.getSummary());
        } catch (Exception e) {
            log.error("Deepseek API call failed, using tag-based fallback", e);
            resp.setGifts(fallbackAiGifts(request, tagNames));
            resp.setSummary("AI 服务暂时不可用，以下为基于标签的推荐结果");
        }
        return resp;
    }

    /** Step 3: search shopping platforms for each AI gift and assemble the final recommendation. */
    public RecommendResponse matchAndSearch(MatchRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Recipient recipient = loadOwnRecipient(request.getRecipientId(), userId);
        String occasionLabel = translateOccasion(request.getOccasion());

        List<RecommendItem> items = new ArrayList<>();
        List<AiGift> gifts = request.getGifts() != null ? request.getGifts() : new ArrayList<>();

        // Search platforms for each gift in parallel, then build items
        List<CompletableFuture<RecommendItem>> futures = new ArrayList<>();
        for (AiGift gi : gifts) {
            futures.add(CompletableFuture.supplyAsync(() -> buildItemFromAiGift(gi)));
        }
        for (CompletableFuture<RecommendItem> f : futures) {
            try {
                RecommendItem item = f.get(20, TimeUnit.SECONDS);
                if (item != null) items.add(item);
            } catch (Exception e) {
                log.warn("Build item failed: {}", e.getMessage());
            }
        }

        // KG enhancement: independent path, merge and deduplicate
        // Main chain stays as-is (DeepSeek 3-step), KG only adds reason-backed items
        if (knowledgeGraphService.isEnabled()) {
            List<RecommendItem> kgItems = knowledgeGraphService.queryRecommendations(
                    request.getRecipientId(), request.getOccasion(), request.getBudget());
            for (RecommendItem kgItem : kgItems) {
                // Fill platformUrl from local DB product
                if (kgItem.getProductId() != null && kgItem.getProductId() > 0) {
                    Product prod = productMapper.selectById(kgItem.getProductId());
                    if (prod != null) {
                        kgItem.setPlatformUrl(prod.getPlatformUrl() != null ? prod.getPlatformUrl() : "");
                        if (prod.getImageUrl() != null && !prod.getImageUrl().isEmpty()) {
                            kgItem.setImageUrl(prod.getImageUrl());
                        }
                    }
                }
                // Avoid duplicate: skip if an AI item has the same productId
                boolean dup = items.stream().anyMatch(ai ->
                        ai.getProductId() != null && ai.getProductId().equals(kgItem.getProductId()));
                if (!dup) items.add(kgItem);
            }
            log.info("KG enhancement added {} items (total now {})", kgItems.size(), items.size());
        }

        // Sort: KG items (with reasoningChain) last, or by score descending
        items.sort((a, b) -> {
            double sa = a.getScore() != null ? a.getScore() : 0;
            double sb = b.getScore() != null ? b.getScore() : 0;
            return Double.compare(sb, sa);
        });

        RecommendResponse response = new RecommendResponse();
        response.setRecipientId(recipient.getId());
        response.setRecipientName(recipient.getName());
        response.setOccasion(request.getOccasion());
        response.setBudget(request.getBudget());
        response.setItems(items);
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
        return matchAndSearch(matchReq);
    }

    private Recipient loadOwnRecipient(Long recipientId, Long userId) {
        Recipient recipient = recipientMapper.selectById(recipientId);
        if (recipient == null || !recipient.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RECIPIENT_NOT_FOUND);
        }
        return recipient;
    }

    private String buildAnalysis(Recipient recipient, List<String> tags) {
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
        if (recipient.getRelation() != null) {
            sb.append("关系：").append(recipient.getRelation()).append("。");
        }
        if (sb.length() == 0) sb.append("画像信息较少，将基于场景与预算综合推荐。");
        return sb.toString();
    }

    /** Build a RecommendItem from an AI gift: try live platform search, then local DB, then AI hint. */
    private RecommendItem buildItemFromAiGift(AiGift gi) {
        RecommendItem item = new RecommendItem();
        item.setProductName(gi.getName());
        item.setPrice(BigDecimal.valueOf(gi.getPrice()));
        String reason = gi.getReason();
        if (reason == null || reason.isBlank()) {
            reason = "送TA这份精选好物";
        }
        item.setReason(reason);
        item.setMatchTags(gi.getTags());
        item.setScore(0.90 + Math.random() * 0.10);

        Product matched = searchPlatformForGift(gi);
        if (matched == null) {
            matched = matchToRealProduct(gi);
        }
        if (matched != null) {
            item.setProductId(matched.getId());
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
        } else {
            item.setProductId(-1L);
            item.setImageUrl("");
            item.setPlatform("拼多多");
            item.setPlatformUrl("https://mobile.yangkeduo.com/search_result.html?search_key="
                    + URLEncoder.encode(gi.getName(), StandardCharsets.UTF_8));
        }
        return item;
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

    private String buildPrompt(Recipient recipient, List<String> tags, String occasion,
                               BigDecimal budget, String extraNote) {
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
        String extraStr = extraNote != null && !extraNote.isBlank() ? extraNote : "";

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
            "- 最近购买/关注：%s\n" +
            "- 备注：%s\n" +
            "\n" +
            "【送礼场景】%s\n" +
            "【预算】¥%s（推荐价格应在预算的60%%-100%%之间，不要远低于预算）\n" +
            "%s" +
            "\n" +
            "【挑选原则】\n" +
            "1. 综合考量收礼人的关系、性别、年龄段、MBTI、性格特点、兴趣标签、最近购买/关注、备注等全部画像信息，每件礼物至少与其中 3 项深度契合，避免泛泛之物；\n" +
            "2. 兼顾情感价值与实用性，优先能体现“用心”的礼物，可包含定制款、小众款；\n" +
            "3. 价格应尽量接近预算（在预算的60%-100%之间），不要推荐远低于预算的廉价品，也不要超出预算；\n" +
            "4. 参考最近购买/关注，避免重复品类，可做有益补充；\n" +
            "5. 每件标注购买平台为拼多多；\n" +
            "6. 若有 MBTI，按人格特质匹配（如 INTJ 偏好实用工具/高质感，ENFP 偏好创意/体验，ISFJ 偏好温馨实用）；\n" +
            "7. 推荐理由须在25字以内，以\"动词+称谓\"开头（如\"让妈妈\"\"给朋友\"\"送TA\"），语言柔和温暖，让收礼人感受到被理解与珍视。\n" +
            "\n" +
            "严格按以下 JSON 返回（不要 markdown 代码块、不要多余文字）：\n" +
            "{\n" +
            "  \"gifts\": [\n" +
            "    {\n" +
            "      \"name\": \"礼物名称（含品牌/型号，便于搜索）\",\n" +
            "      \"price\": 价格数字,\n" +
            "      \"reason\": \"25字以内推荐理由，以动词+称谓开头，如：送妈妈一束永生花\",\n" +
            "      \"tags\": [\"匹配点1\", \"匹配点2\"],\n" +
            "      \"platform\": \"拼多多\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"summary\": \"一句话总结推荐策略，50 字以内\"\n" +
            "}",
            recipient.getName(), relationStr, genderStr, ageStr, mbtiStr, personalityStr, tagStr, purchasesStr, noteStr,
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

    // ---------- Deepseek API Call ----------

    private AiGiftResult callDeepseek(String prompt) throws IOException {
        DeepseekRequest req = new DeepseekRequest();
        req.setModel(model);
        req.setTemperature(0.7);
        req.setMaxTokens(4096);

        List<DeepseekMessage> messages = new ArrayList<>();
        DeepseekMessage sysMsg = new DeepseekMessage();
        sysMsg.setRole("system");
        sysMsg.setContent("你是一位温暖细腻的礼物推荐AI助手。你总是以JSON格式回复，不添加任何额外的解释或markdown标记。你推荐的礼物贴近生活、实用且有情感价值，语言柔和温暖，善于用收礼人的称谓让每份推荐都更有温度。");
        messages.add(sysMsg);

        DeepseekMessage userMsg = new DeepseekMessage();
        userMsg.setRole("user");
        userMsg.setContent(prompt);
        messages.add(userMsg);

        req.setMessages(messages);

        // Configure ObjectMapper to ignore unknown properties
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String jsonBody = objectMapper.writeValueAsString(req);
        log.info("Calling Deepseek API: model={}, prompt length={}", model, prompt.length());

        URL url = new URL(baseUrl + "/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(120000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String respBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        if (code != 200) {
            log.error("Deepseek API error: HTTP {} body={}", code, respBody);
            throw new IOException("Deepseek API returned " + code + ": " + respBody);
        }

        log.info("Deepseek API response received, length={}", respBody.length());
        DeepseekResponse dsResp = objectMapper.readValue(respBody, DeepseekResponse.class);

        String content = dsResp.getChoices().get(0).getMessage().getContent();
        log.info("Deepseek response content length={}", content.length());

        // Strip markdown code blocks if present
        String jsonContent = content.trim();
        if (jsonContent.startsWith("```")) {
            int start = jsonContent.indexOf("\n") + 1;
            int end = jsonContent.lastIndexOf("```");
            if (end > start) jsonContent = jsonContent.substring(start, end).trim();
        }

        return objectMapper.readValue(jsonContent, AiGiftResult.class);
    }

    // ---------- History & Feedback ----------

    public Page<RecommendationHistory> history(int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<RecommendationHistory> p = new Page<>(page, size);
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
        Long userId = StpUtil.getLoginIdAsLong();
        for (Long id : ids) {
            RecommendationHistory history = historyMapper.selectById(id);
            if (history != null && history.getUserId().equals(userId)) {
                historyMapper.deleteById(id);
            }
        }
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
                .peek(item -> item.setScore(0.80 + Math.random() * 0.20))
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
