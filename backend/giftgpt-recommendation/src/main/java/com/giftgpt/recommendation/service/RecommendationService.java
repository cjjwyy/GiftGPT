package com.giftgpt.recommendation.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
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
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecipientMapper recipientMapper;
    private final RecipientTagMapper recipientTagMapper;
    private final RecommendationHistoryMapper historyMapper;

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
        private List<AiGiftItem> gifts;
        private String summary;
    }

    @Data
    static class AiGiftItem {
        private String name;
        private double price;
        private String reason;
        private List<String> tags;
        private String platform;
    }

    // ---------- Core Logic ----------

    public RecommendResponse search(RecommendRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Recipient recipient = recipientMapper.selectById(request.getRecipientId());
        if (recipient == null || !recipient.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RECIPIENT_NOT_FOUND);
        }

        List<RecipientTag> tags = recipientTagMapper.selectList(
                new LambdaQueryWrapper<RecipientTag>().eq(RecipientTag::getRecipientId, recipient.getId()));
        List<String> tagNames = tags.stream().map(RecipientTag::getTagName).collect(Collectors.toList());

        String occasionLabel = translateOccasion(request.getOccasion());
        String prompt = buildPrompt(recipient, tagNames, occasionLabel, request.getBudget(), request.getExtraNote());

        AiGiftResult aiResult;
        try {
            aiResult = callDeepseek(prompt);
        } catch (Exception e) {
            log.error("Deepseek API call failed, falling back to mock", e);
            // Fallback: use old mock logic if AI call fails
            RecommendResponse fallback = new RecommendResponse();
            fallback.setRecipientId(recipient.getId());
            fallback.setRecipientName(recipient.getName());
            fallback.setOccasion(request.getOccasion());
            fallback.setBudget(request.getBudget());
            fallback.setItems(generateMockItems(request, tagNames));
            fallback.setSummary("AI 服务暂时不可用，以下为基于标签的推荐结果");
            saveHistory(userId, recipient.getId(), request.getOccasion(), request.getBudget(), fallback);
            return fallback;
        }

        List<RecommendItem> items = new ArrayList<>();
        if (aiResult.getGifts() != null) {
            long idx = 1;
            for (AiGiftItem gi : aiResult.getGifts()) {
                RecommendItem item = new RecommendItem();
                item.setProductId(idx++);
                item.setProductName(gi.getName());
                item.setPrice(BigDecimal.valueOf(gi.getPrice()));
                item.setImageUrl("");
                item.setPlatform(gi.getPlatform() != null ? gi.getPlatform() : "综合电商");
                item.setPlatformUrl("https://www.jd.com");
                item.setReason(gi.getReason());
                item.setMatchTags(gi.getTags());
                item.setScore(0.90 + Math.random() * 0.10);
                items.add(item);
            }
        }

        RecommendResponse response = new RecommendResponse();
        response.setRecipientId(recipient.getId());
        response.setRecipientName(recipient.getName());
        response.setOccasion(request.getOccasion());
        response.setBudget(request.getBudget());
        response.setItems(items);
        response.setSummary(aiResult.getSummary() != null ? aiResult.getSummary() :
                "根据" + recipient.getName() + "的特征，在" + occasionLabel + "场景下为您推荐以下礼物");

        saveHistory(userId, recipient.getId(), request.getOccasion(), request.getBudget(), response);
        return response;
    }

    private void saveHistory(Long userId, Long recipientId, String occasion,
                             BigDecimal budget, RecommendResponse response) {
        RecommendationHistory history = new RecommendationHistory();
        history.setUserId(userId);
        history.setRecipientId(recipientId);
        history.setScene(occasion);
        history.setBudget(budget);
        history.setResult(JSONUtil.toJsonStr(response));
        historyMapper.insert(history);
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
            "你是一个专业的礼物推荐顾问，拥有丰富的礼物挑选经验和深厚的人际关系洞察力。\n" +
            "请根据以下收礼人画像，在该场景和预算范围内，推荐5-8个最合适的礼物。\n" +
            "\n" +
            "【收礼人画像】\n" +
            "- 姓名：%s\n" +
            "- 关系：%s\n" +
            "- 性别：%s\n" +
            "- 年龄段：%s\n" +
            "- MBTI人格：%s\n" +
            "- 性格特点：%s\n" +
            "- 性格标签：%s\n" +
            "- 最近购买/关注：%s\n" +
            "- 备注：%s\n" +
            "\n" +
            "【送礼场景】%s\n" +
            "【预算范围】¥%s 以内\n" +
            "%s%s" +
            "\n" +
            "【推荐要求】\n" +
            "1. 礼物要与收礼人的MBTI人格、性格特点、兴趣标签、年龄、关系高度匹配，每件都应有独特的推荐理由\n" +
            "2. 价格要在预算范围内，并标注具体金额（人民币）\n" +
            "3. 优先推荐实用、有情感价值、能体现用心程度的礼物\n" +
            "4. 参考收礼人最近购买/关注的商品，避免推荐重复品类\n" +
            "5. 标注每件礼物适合的购买平台（如：京东、淘宝、拼多多、小红书、得物等）\n" +
            "6. 如有MBTI信息，根据人格类型推荐契合的礼物（如INTJ推荐实用工具，ENFP推荐创意礼物）\n" +
            "\n" +
            "请严格按照以下JSON格式返回（不要包含markdown代码块标记）：\n" +
            "{\n" +
            "  \"gifts\": [\n" +
            "    {\n" +
            "      \"name\": \"礼物名称\",\n" +
            "      \"price\": 价格数字(元),\n" +
            "      \"reason\": \"详细的推荐理由，80-150字，结合MBTI、性格、场景和礼物特点，温暖走心\",\n" +
            "      \"tags\": [\"匹配标签1\", \"匹配标签2\"],\n" +
            "      \"platform\": \"推荐购买平台\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"summary\": \"一句话总结推荐策略，50字以内\"\n" +
            "}",
            recipient.getName(), relationStr, genderStr, ageStr, mbtiStr, personalityStr, tagStr, purchasesStr, noteStr,
            occasion, budget,
            extraStr.isEmpty() ? "" : "【额外说明】" + extraStr + "\n",
            ""
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
        sysMsg.setContent("你是一个专业的礼物推荐AI助手。你总是以JSON格式回复，不添加任何额外的解释或markdown标记。你推荐的礼物贴近生活、实用且有情感价值。");
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
        return historyMapper.selectPage(p,
                new LambdaQueryWrapper<RecommendationHistory>()
                        .eq(RecommendationHistory::getUserId, userId)
                        .orderByDesc(RecommendationHistory::getCreateTime));
    }

    public void feedback(Long id, RecommendFeedbackRequest request) {
        RecommendationHistory history = historyMapper.selectById(id);
        if (history != null) {
            history.setFeedback(request.getFeedback());
            historyMapper.updateById(history);
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
        item.setPlatform("京东");
        item.setPlatformUrl("https://www.jd.com");
        item.setReason(reason);
        return item;
    }
}
