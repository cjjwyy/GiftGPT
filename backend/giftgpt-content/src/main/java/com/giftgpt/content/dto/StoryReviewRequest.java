package com.giftgpt.content.dto;
import lombok.Data;
import javax.validation.constraints.*;
@Data
public class StoryReviewRequest {
    @NotBlank @Pattern(regexp = "hide|dismiss")
    private String action;
    @NotBlank @Size(max = 500)
    private String note;
}
