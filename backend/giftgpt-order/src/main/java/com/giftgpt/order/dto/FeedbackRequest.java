package com.giftgpt.order.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class FeedbackRequest {
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 1000, message = "反馈内容不能超过1000个字符")
    private String content;
    @Size(max = 20)
    private String type;
    private Integer isPublic;
}
