package com.giftgpt.order.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {

    @NotNull(message = "送礼记录ID不能为空")
    private Long giftRecordId;

    private Long packagingThemeId;

    private String greetingStyle;

    private String customMessage;
}
