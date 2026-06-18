package com.giftgpt.recommendation.dto;

import lombok.Data;

@Data
public class ChatRecommendRequest {

    private String message;
    private Long recipientId;
    private String sessionId;
}
