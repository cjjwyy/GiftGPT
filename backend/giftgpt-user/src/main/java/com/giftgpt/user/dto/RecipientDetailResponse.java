package com.giftgpt.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecipientDetailResponse {

    private Long id;
    private String name;
    private String relation;
    private Integer gender;
    private String ageRange;
    private String mbti;
    private String personality;
    private String recentPurchases;
    private String note;
    private List<String> tags;
    private String personalityDesc;
    private String hobbyList;
    private String socialAnalysis;
}
