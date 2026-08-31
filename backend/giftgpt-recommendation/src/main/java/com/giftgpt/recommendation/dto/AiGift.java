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
    /** 关键词权重（关键词推荐模型）：权重越高越优先搜索 */
    private double weight = 1.0;
}
