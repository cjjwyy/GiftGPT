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
    private String requestKey;
    private String customizationsJson;
    private String priceDetailsJson;
    private Integer version;

    private Long orderId;
    private Long userId;
    private Long giftRecordId;
    private Long productId;
    private String theme;
    @com.baomidou.mybatisplus.annotation.TableField(updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.IGNORED)
    private String customText;
    private String previewImage;
    private BigDecimal price;
    private String productName;
    private BigDecimal productPrice;
    private String productImageUrl;
    @com.baomidou.mybatisplus.annotation.TableField(updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.IGNORED)
    private String ribbonText;
    private String ribbonColor;
    @com.baomidou.mybatisplus.annotation.TableField(updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.IGNORED)
    private String scent;
    private String photoUrl;
    private String wrappingStyle;
}
