package com.giftgpt.order.dto;

import lombok.Data;

@Data
public class GreetingGenerateRequest {

    private String recipientName;
    private String relation;
    private String occasion;
    private String senderName;
}
