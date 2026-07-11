package com.giftgpt.recommendation.dto;

import lombok.Data;

import java.util.List;

@Data
public class PersonalitySnapshot {

    private Long recipientId;
    private String name;
    private String relation;
    private Integer gender;
    private String ageRange;
    private String mbti;
    private String personality;
    private List<String> tags;
    private String recentPurchases;
    private String note;
    private String analysis;
}
