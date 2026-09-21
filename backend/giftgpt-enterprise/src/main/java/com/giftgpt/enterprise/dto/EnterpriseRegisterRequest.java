package com.giftgpt.enterprise.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class EnterpriseRegisterRequest {
    @NotBlank(message = "企业名称不能为空")
    @Size(max = 200)
    private String companyName;

    @Size(max = 100)
    private String licenseNo;

    @Size(max = 50)
    private String contactName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;
}
