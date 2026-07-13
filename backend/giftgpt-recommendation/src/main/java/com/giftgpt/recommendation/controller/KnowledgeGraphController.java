package com.giftgpt.recommendation.controller;

import com.giftgpt.common.result.Result;
import com.giftgpt.recommendation.service.KnowledgeGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "知识图谱管理", description = "重建/同步知识图谱，预留后续 taxonomy 变动入口")
@RestController
@RequestMapping("/api/v1/kg")
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    @Value("${giftgpt.kg.enabled:false}")
    private boolean kgEnabled;

    @Value("${giftgpt.kg.taxonomy-file:classpath:kg_taxonomy.json}")
    private String defaultTaxonomyFile;

    @Operation(summary = "查看 KG 状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> s = new HashMap<>();
        s.put("enabled", kgEnabled);
        s.put("connected", knowledgeGraphService.isEnabled());
        s.put("taxonomyFile", defaultTaxonomyFile);
        return Result.ok(s);
    }

    @Operation(summary = "重建整个知识图谱（重新加载 taxonomy JSON + H2 数据）")
    @PostMapping("/rebuild")
    public Result<Map<String, Object>> rebuild(@RequestBody(required = false) Map<String, String> body) {
        String filePath = null;
        if (body != null) {
            filePath = body.get("taxonomyFilePath");
        }
        Map<String, Object> result;
        if (filePath != null && !filePath.isBlank()) {
            log.info("KG rebuild from user-provided file: {}", filePath);
            result = knowledgeGraphService.rebuildFromFilePath(filePath);
        } else {
            log.info("KG rebuild from default taxonomy file");
            result = knowledgeGraphService.rebuildGraph();
        }
        return Result.ok(result);
    }

    @Operation(summary = "仅同步商品到 KG（增量）")
    @PostMapping("/sync-products")
    public Result<String> syncProducts() {
        knowledgeGraphService.resyncProducts();
        return Result.ok("商品同步完成");
    }
}