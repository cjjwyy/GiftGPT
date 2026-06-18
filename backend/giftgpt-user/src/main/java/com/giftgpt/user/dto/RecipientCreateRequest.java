package com.giftgpt.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecipientCreateRequest {

    private String name;
    private String relation;
    private Integer gender;
    private String ageRange;
    private String note;
    private List<String> tags;
}
