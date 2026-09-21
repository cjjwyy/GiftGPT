package com.giftgpt.recommendation.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AnalyzeRequest {
    @NotNull(message = "收礼人ID不能为空")
    private Long recipientId;
}
