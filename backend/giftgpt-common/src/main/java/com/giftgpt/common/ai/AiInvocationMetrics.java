package com.giftgpt.common.ai;

import lombok.Data;

@Data
public class AiInvocationMetrics {
    private Long totalCalls;
    private Long successfulCalls;
    private Long fallbackCalls;
    private Long promptTokens;
    private Long completionTokens;
    private Double averageLatencyMs;
}
