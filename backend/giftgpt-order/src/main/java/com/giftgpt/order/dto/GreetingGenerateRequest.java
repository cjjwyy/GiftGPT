package com.giftgpt.order.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class GreetingGenerateRequest {

    @NotBlank(message = "收礼人姓名不能为空")
    @Size(max = 50)
    private String recipientName;
    @Size(max = 50)
    private String relation;
    @Size(max = 50)
    private String occasion;
    @NotBlank(message = "送礼人姓名不能为空")
    @Size(max = 50)
    private String senderName;
}
