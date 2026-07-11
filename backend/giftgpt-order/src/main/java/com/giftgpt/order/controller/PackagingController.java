package com.giftgpt.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.result.Result;
import com.giftgpt.order.dto.packaging.AiPackagingRequest;
import com.giftgpt.order.dto.packaging.AiPackagingResult;
import com.giftgpt.order.dto.packaging.PackagingTheme;
import com.giftgpt.order.dto.packaging.SavePackagingRequest;
import com.giftgpt.order.entity.Packaging;
import com.giftgpt.order.service.PackagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "包装服务", description = "包装方案、AI推荐、保存")
@RestController
@RequestMapping("/api/v1/packaging")
@RequiredArgsConstructor
public class PackagingController {

    private final PackagingService packagingService;

    @Operation(summary = "获取包装方案列表")
    @GetMapping("/themes")
    public Result<List<PackagingTheme>> themes() {
        return Result.ok(packagingService.getThemes());
    }

    @Operation(summary = "AI智能推荐包装")
    @PostMapping("/ai-recommend")
    public Result<AiPackagingResult> aiRecommend(@RequestBody AiPackagingRequest request) {
        return Result.ok(packagingService.aiRecommend(request));
    }

    @Operation(summary = "保存包装方案")
    @PostMapping("/save")
    public Result<Packaging> save(@RequestBody SavePackagingRequest request) {
        return Result.ok(packagingService.savePackaging(request));
    }

    @Operation(summary = "我的包装方案列表")
    @GetMapping("/list")
    public Result<Page<Packaging>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(packagingService.listPackaging(page, size));
    }
}
