package com.giftgpt.recommendation.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class RecommendEventRequest {
    private Long recipientId;
    @Size(max = 50)
    private String occasion;
    private Long productId;
    @Size(max = 200)
    private String productName;
    @NotBlank(message = "事件类型不能为空")
    @Pattern(regexp = "detail|buy|packaging", message = "事件类型不合法")
    private String eventType;
}
