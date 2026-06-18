package com.giftgpt.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.result.Result;
import com.giftgpt.user.dto.RecipientCreateRequest;
import com.giftgpt.user.dto.RecipientDetailResponse;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.service.RecipientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "收礼人画像", description = "收礼人画像 CRUD、标签管理")
@RestController
@RequestMapping("/api/v1/recipients")
@RequiredArgsConstructor
public class RecipientController {

    private final RecipientService recipientService;

    @Operation(summary = "创建收礼人画像")
    @PostMapping
    public Result<Recipient> create(@RequestBody RecipientCreateRequest request) {
        return Result.ok(recipientService.create(request));
    }

    @Operation(summary = "获取收礼人列表")
    @GetMapping
    public Result<Page<Recipient>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(recipientService.list(page, size));
    }

    @Operation(summary = "获取收礼人详情及画像")
    @GetMapping("/{id}")
    public Result<RecipientDetailResponse> detail(@PathVariable Long id) {
        return Result.ok(recipientService.getDetail(id));
    }

    @Operation(summary = "更新收礼人画像")
    @PutMapping("/{id}")
    public Result<Recipient> update(@PathVariable Long id, @RequestBody RecipientCreateRequest request) {
        return Result.ok(recipientService.update(id, request));
    }

    @Operation(summary = "删除收礼人")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recipientService.delete(id);
        return Result.ok();
    }
}
