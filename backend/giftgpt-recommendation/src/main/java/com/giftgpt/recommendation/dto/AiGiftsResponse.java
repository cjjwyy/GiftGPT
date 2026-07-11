package com.giftgpt.recommendation.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiGiftsResponse {

    private List<AiGift> gifts;
    private String summary;
}
