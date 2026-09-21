package com.giftgpt.order.dto;

import lombok.Data;

@Data
public class PublicFeedbackGiftResponse {
    private String recipientName;
    private String occasion;
    private String greetingContent;
    private String greetingVoiceUrl;
    private String productName;
    private Boolean submitted;
}
