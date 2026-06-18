package com.giftgpt.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.result.Result;
import com.giftgpt.user.entity.GiftRecord;
import com.giftgpt.user.service.GiftMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "礼物记忆库", description = "送礼历史记录查询")
@RestController
@RequestMapping("/api/v1/gifts")
@RequiredArgsConstructor
public class GiftMemoryController {

    private final GiftMemoryService giftMemoryService;

    @Operation(summary = "送礼记录列表")
    @GetMapping
    public Result<Page<GiftRecord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(giftMemoryService.listHistory(page, size));
    }

    @Operation(summary = "送礼记录详情")
    @GetMapping("/{id}")
    public Result<GiftRecord> detail(@PathVariable Long id) {
        return Result.ok(giftMemoryService.getById(id));
    }
}
