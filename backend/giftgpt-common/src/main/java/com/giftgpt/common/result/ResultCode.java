package com.giftgpt.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // Business codes 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    PHONE_EXISTS(1003, "手机号已注册"),
    RECIPIENT_NOT_FOUND(1010, "收礼人不存在"),
    GIFT_RECORD_NOT_FOUND(1020, "送礼记录不存在"),
    ORDER_NOT_FOUND(1030, "订单不存在"),
    PRODUCT_NOT_FOUND(1040, "商品不存在"),

    // AI codes 2xxx
    AI_SERVICE_ERROR(2001, "AI服务异常"),
    RECOMMENDATION_FAILED(2002, "推荐生成失败"),
    PROFILE_ANALYSIS_FAILED(2003, "画像分析失败"),
    ;

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
