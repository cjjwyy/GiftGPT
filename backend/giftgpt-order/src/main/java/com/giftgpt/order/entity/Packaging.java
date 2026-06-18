package com.giftgpt.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("packaging")
public class Packaging extends BaseEntity {

    private Long orderId;
    private String theme;
    private String customText;
    private String previewImage;
    private BigDecimal price;
}
