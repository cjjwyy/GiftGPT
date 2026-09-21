package com.giftgpt.server;

import com.giftgpt.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("status", "ok");
        status.put("service", "giftgpt-server");
        return Result.ok(status);
    }
}
