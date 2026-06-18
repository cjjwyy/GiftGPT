package com.giftgpt.enterprise.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("enterprise")
public class Enterprise extends BaseEntity {

    private Long userId;
    private String companyName;
    private String licenseNo;
    private String contactName;
    private String contactPhone;
    private String status;
    private String subscription;
}
