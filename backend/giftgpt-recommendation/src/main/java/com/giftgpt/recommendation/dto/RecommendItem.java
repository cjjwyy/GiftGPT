package com.giftgpt.recommendation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RecommendItem {

    private Long productId;
    private String productName;
    private BigDecimal price;
    private String imageUrl;
    private String platform;
    private String platformUrl;
    private Double score;
    private String reason;
    private List<String> matchTags;
    private String reasoningChain;
    /** pdd / local / kg / ai_fallback，便于界面明确展示来源。 */
    private String source;
    private List<ScoreFactor> scoreFactors;
    /** 命中关键词的权重，用于排序（高权重优先） */
    private double keywordWeight = 1.0;

    @Data
    public static class ScoreFactor {
        private String label;
        private Double weight;
        private Double score;
    }
}
