package com.giftgpt.auth.config;

import cn.dev33.satoken.exception.NotLoginException;
import com.giftgpt.common.result.Result;
import com.giftgpt.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SaTokenExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        log.warn("Not logged in: {}", e.getMessage());
        return Result.fail(ResultCode.UNAUTHORIZED.getCode(), "未登录或登录已过期，请先登录");
    }
}
