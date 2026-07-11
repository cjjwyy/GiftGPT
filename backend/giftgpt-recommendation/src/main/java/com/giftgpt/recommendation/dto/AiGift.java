package com.giftgpt.recommendation.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiGift {

    private String name;
    private double price;
    private String reason;
    private List<String> tags;
    private String platform;
}
