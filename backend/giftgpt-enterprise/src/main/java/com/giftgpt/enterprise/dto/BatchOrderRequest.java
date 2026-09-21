package com.giftgpt.enterprise.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BatchOrderRequest {

    @NotNull(message = "企业ID不能为空")
    @Positive(message = "企业ID不合法")
    private Long enterpriseId;

    @Valid
    @NotEmpty(message = "员工礼物列表不能为空")
    @Size(max = 50, message = "一次最多创建50份礼物")
    private List<EmployeeGift> employees;

    @Data
    public static class EmployeeGift {
        @Size(max = 50)
        private String employeeName;

        @NotNull(message = "收礼人ID不能为空")
        @Positive(message = "收礼人ID不合法")
        private Long recipientId;

        @Pattern(regexp = "birthday|anniversary|valentines|festival|graduation|proposal|mothers_day|fathers_day|teachers_day|christmas|thank_you|other",
                message = "场景值不合法")
        private String occasion;

        @NotNull(message = "预算不能为空")
        @DecimalMin(value = "0.01", message = "预算必须大于0")
        private BigDecimal budget;
    }
}
