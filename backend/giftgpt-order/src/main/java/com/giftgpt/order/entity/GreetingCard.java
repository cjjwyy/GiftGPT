package com.giftgpt.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("greeting_card")
public class GreetingCard extends BaseEntity {

    private String content;
    private String voiceUrl;
    private String qrCodeUrl;
    private String styleTemplate;
}
