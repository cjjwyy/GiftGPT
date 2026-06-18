package com.giftgpt.order.dto;

import lombok.Data;

@Data
public class OrderDetailResponse {

    private Long orderId;
    private String orderNo;
    private String status;
    private String logisticsNo;
    private String logisticsCompany;
    private String recipientName;
    private String productName;
    private String packagingTheme;
    private String greetingContent;
    private String greetingVoiceUrl;
    private String greetingQrCodeUrl;
}
