package com.giftgpt.recommendation.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecommendRequest {

    @NotNull(message = "收礼人ID不能为空")
    private Long recipientId;

    @NotBlank(message = "场景不能为空")
    private String occasion;

    @NotNull(message = "预算不能为空")
    private BigDecimal budget;

    private String extraNote;
}
