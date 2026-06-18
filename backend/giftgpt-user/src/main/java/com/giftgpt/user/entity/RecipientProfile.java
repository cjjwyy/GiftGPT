package com.giftgpt.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recipient_profile")
public class RecipientProfile extends BaseEntity {

    private Long recipientId;
    private String personalityDesc;
    private String hobbyList;
    private String socialAnalysis;
}
