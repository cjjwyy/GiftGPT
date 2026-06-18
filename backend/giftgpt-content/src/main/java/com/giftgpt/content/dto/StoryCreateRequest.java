package com.giftgpt.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StoryCreateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private Long giftRecordId;
    private String images;
    private Integer isAnonymous;
}
