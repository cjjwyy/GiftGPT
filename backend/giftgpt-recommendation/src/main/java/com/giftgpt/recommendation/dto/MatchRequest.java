package com.giftgpt.recommendation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MatchRequest {

    private Long recipientId;
    private String occasion;
    private BigDecimal budget;
    private String extraNote;
    private List<AiGift> gifts;
    private String summary;
}
