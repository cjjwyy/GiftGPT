package com.giftgpt.enterprise.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class BatchOrderRequest {

    @NotBlank(message = "企业ID不能为空")
    private Long enterpriseId;

    private List<EmployeeGift> employees;

    @Data
    public static class EmployeeGift {
        private String employeeName;
        private Long recipientId;
        private String occasion;
        private String budget;
    }
}
