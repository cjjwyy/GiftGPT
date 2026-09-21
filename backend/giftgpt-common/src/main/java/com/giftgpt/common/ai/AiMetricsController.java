package com.giftgpt.common.ai;

import com.giftgpt.common.mapper.AiInvocationLogMapper;
import com.giftgpt.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI可靠性", description = "AI调用质量与降级统计")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiMetricsController {

    private final AiInvocationLogMapper invocationLogMapper;

    @Operation(summary = "AI调用汇总")
    @GetMapping("/metrics")
    public Result<AiInvocationMetrics> metrics() {
        AiInvocationMetrics metrics = invocationLogMapper.aggregate();
        return Result.ok(metrics == null ? new AiInvocationMetrics() : metrics);
    }
}
