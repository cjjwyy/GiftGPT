package com.giftgpt.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {

    private String name;
    private BigDecimal price;
    private String category;
    private String platform;
    private String platformUrl;
    private String imageUrl;
    private String description;
    private Integer salesCount;
    private Double rating;
    private Integer status;
}
