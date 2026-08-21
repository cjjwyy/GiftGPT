package com.giftgpt.user.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RecipientCreateRequest {

    private String name;
    private String relation;
    private Integer gender;
    private String ageRange;
    private String mbti;
    private String personality;
    private String recentPurchases;
    private String note;
    private List<String> tags;
    /** 有补充项的标签 -> 补充项内容（如：音乐 -> 吉他、贝斯） */
    private Map<String, List<String>> tagSupplements;
}
