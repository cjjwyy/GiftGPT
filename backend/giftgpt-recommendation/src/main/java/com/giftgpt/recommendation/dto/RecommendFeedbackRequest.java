package com.giftgpt.recommendation.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class RecommendFeedbackRequest {

    @NotBlank(message = "反馈不能为空")
    @Pattern(regexp = "useful|not_useful|purchased|ignored", message = "反馈值不合法")
    private String feedback;
}
