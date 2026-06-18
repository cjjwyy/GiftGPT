package com.giftgpt.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_consume_profile")
public class UserConsumeProfile extends BaseEntity {

    private Long userId;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private String categoryPrefs;
    private String brandPrefs;
    private String consumeLevel;
    private String tasteCircle;
}
