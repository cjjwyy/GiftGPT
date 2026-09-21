package com.giftgpt.order.dto;

import javax.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {

    private Long giftRecordId;

    private Long packagingThemeId;

    private String greetingStyle;

    @Size(max = 200, message = "定制文案不能超过200个字符")
    private String customMessage;
}
