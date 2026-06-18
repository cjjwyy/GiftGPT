package com.giftgpt.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recipient_tag")
public class RecipientTag extends BaseEntity {

    private Long recipientId;
    private String tagCode;
    private String tagName;
}
