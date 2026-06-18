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
}
