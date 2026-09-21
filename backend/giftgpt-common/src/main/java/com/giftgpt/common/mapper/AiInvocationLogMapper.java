package com.giftgpt.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.giftgpt.common.ai.AiInvocationLog;
import com.giftgpt.common.ai.AiInvocationMetrics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiInvocationLogMapper extends BaseMapper<AiInvocationLog> {

    @Select("SELECT COUNT(*) AS total_calls, "
            + "COALESCE(SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END), 0) AS successful_calls, "
            + "COALESCE(SUM(CASE WHEN fallback = 1 THEN 1 ELSE 0 END), 0) AS fallback_calls, "
            + "COALESCE(SUM(prompt_tokens), 0) AS prompt_tokens, "
            + "COALESCE(SUM(completion_tokens), 0) AS completion_tokens, "
            + "COALESCE(AVG(latency_ms), 0) AS average_latency_ms FROM ai_invocation_log")
    AiInvocationMetrics aggregate();
}
