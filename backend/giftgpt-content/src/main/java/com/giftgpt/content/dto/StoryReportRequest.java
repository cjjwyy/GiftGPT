package com.giftgpt.content.dto;
import lombok.Data;
import javax.validation.constraints.*;
@Data
public class StoryReportRequest {
    @NotBlank @Pattern(regexp = "spam|privacy|abuse|other")
    private String reason;
    @NotBlank @Size(max = 500)
    private String detail;
}
