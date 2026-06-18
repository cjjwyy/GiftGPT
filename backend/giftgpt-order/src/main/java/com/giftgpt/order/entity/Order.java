package com.giftgpt.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.giftgpt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`order`")
public class Order extends BaseEntity {

    private Long giftRecordId;
    private String orderNo;
    private BigDecimal totalAmount;
    private String status;
    private String logisticsNo;
    private String logisticsCompany;
}
