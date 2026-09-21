package com.giftgpt.recommendation.dto;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecommendRequest {

    @NotNull(message = "收礼人ID不能为空")
    private Long recipientId;

    @NotBlank(message = "场景不能为空")
    @Pattern(regexp = "birthday|anniversary|valentines|festival|graduation|proposal|mothers_day|fathers_day|teachers_day|christmas|thank_you|daily|visit_patient|family_visit|other",
            message = "场景值不合法")
    private String occasion;

    @NotNull(message = "预算不能为空")
    @DecimalMin(value = "1", message = "预算不能小于1元")
    @DecimalMax(value = "100000", message = "预算不能超过100000元")
    private BigDecimal budget;

    @Size(max = 500, message = "额外说明不能超过500个字符")
    private String extraNote;
}
