package com.giftgpt.common.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_invocation_log")
public class AiInvocationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String scene;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Long latencyMs;
    private Integer success;
    private Integer fallback;
    private String errorType;
    private LocalDateTime createTime;
}
