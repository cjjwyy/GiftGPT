package com.giftgpt.content.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ReplyRequest {
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复内容不能超过500个字符")
    private String content;
}
