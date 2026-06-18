package com.giftgpt.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recipient")
public class Recipient extends BaseEntity {

    private Long userId;
    private String name;
    private String relation;
    private Integer gender;
    private String ageRange;
    private String note;
}
