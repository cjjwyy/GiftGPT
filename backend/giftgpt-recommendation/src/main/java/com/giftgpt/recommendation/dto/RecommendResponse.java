package com.giftgpt.recommendation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RecommendResponse {

    private Long recipientId;
    private String recipientName;
    private String occasion;
    private BigDecimal budget;
    private List<RecommendItem> items;
    private String summary;
}
