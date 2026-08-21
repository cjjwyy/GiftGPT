package com.giftgpt.user.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

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
    private Map<String, List<String>> tagSupplements;
    private String personalityDesc;
    private String hobbyList;
    private String socialAnalysis;
}
