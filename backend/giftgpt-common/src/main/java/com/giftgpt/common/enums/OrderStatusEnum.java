package com.giftgpt.common.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    PENDING("pending", "待支付"),
    PAID("paid", "已支付"),
    SHIPPED("shipped", "已发货"),
    DELIVERED("delivered", "已送达"),
    CANCELLED("cancelled", "已取消"),
    ;

    private final String code;
    private final String desc;

    OrderStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
