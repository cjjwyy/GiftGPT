package com.giftgpt.order.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedbackLinkResponse {
    private String url;
    private String qrCodeUrl;
    private LocalDateTime expiresAt;
}
