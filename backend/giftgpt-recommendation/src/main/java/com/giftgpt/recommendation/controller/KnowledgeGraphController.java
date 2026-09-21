package com.giftgpt.recommendation.controller;

import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.Result;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.recommendation.service.KnowledgeGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "知识图谱管理", description = "重建/同步知识图谱")
@RestController
@RequestMapping("/api/v1/kg")
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    @Value("${giftgpt.kg.enabled:false}")
    private boolean kgEnabled;

    @Value("${giftgpt.kg.admin-token:}")
    private String adminToken;

    @Operation(summary = "查看 KG 状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", kgEnabled);
        status.put("connected", knowledgeGraphService.isEnabled());
        status.put("taxonomyLoaded", knowledgeGraphService.isTaxonomyLoaded());
        return Result.ok(status);
    }

    @Operation(summary = "按服务端配置的 taxonomy 重建知识图谱")
    @PostMapping("/rebuild")
    public Result<Map<String, Object>> rebuild(
            @RequestHeader(value = "X-KG-Admin-Token", required = false) String providedToken) {
        checkAdmin(providedToken);
        log.info("KG rebuild from configured taxonomy resource");
        return Result.ok(knowledgeGraphService.rebuildGraph());
    }

    @Operation(summary = "仅同步商品到 KG（增量）")
    @PostMapping("/sync-products")
    public Result<String> syncProducts(
            @RequestHeader(value = "X-KG-Admin-Token", required = false) String providedToken) {
        checkAdmin(providedToken);
        knowledgeGraphService.resyncProducts();
        return Result.ok("商品同步完成");
    }

    private void checkAdmin(String providedToken) {
        if (adminToken == null || adminToken.isBlank() || providedToken == null
                || !MessageDigest.isEqual(adminToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "KG 管理接口未启用或管理令牌无效");
        }
    }
}
