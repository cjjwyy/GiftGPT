package com.giftgpt.order.dto;
import lombok.Data;
@Data
public class GreetingResponse {
    private String content;
    private String styleTemplate;
    private boolean aiGenerated;
}
