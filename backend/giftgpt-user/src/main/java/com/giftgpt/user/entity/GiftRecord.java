package com.giftgpt.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gift_record")
public class GiftRecord extends BaseEntity {

    private Long userId;
    private Long recipientId;
    private String occasion;
    private BigDecimal budget;
    private Long productId;
    private Long greetingCardId;
    private String status;
}
