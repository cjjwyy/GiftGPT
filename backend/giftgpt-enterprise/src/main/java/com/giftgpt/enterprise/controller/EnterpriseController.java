package com.giftgpt.enterprise.controller;

import com.giftgpt.common.result.Result;
import com.giftgpt.enterprise.dto.BatchOrderRequest;
import com.giftgpt.enterprise.entity.Enterprise;
import com.giftgpt.enterprise.service.EnterpriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "B端企业服务", description = "企业注册、批量团购、员工关怀")
@RestController
@RequestMapping("/api/v1/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @Operation(summary = "企业注册")
    @PostMapping("/register")
    public Result<Enterprise> register(@RequestBody Enterprise enterprise) {
        return Result.ok(enterpriseService.register(enterprise));
    }

    @Operation(summary = "查询企业信息")
    @GetMapping("/{id}")
    public Result<Enterprise> getById(@PathVariable Long id) {
        return Result.ok(enterpriseService.getById(id));
    }

    @Operation(summary = "获取我的企业")
    @GetMapping("/my")
    public Result<Enterprise> getMyEnterprise() {
        return Result.ok(enterpriseService.getMyEnterprise());
    }

    @Operation(summary = "批量团购下单")
    @PostMapping("/orders/batch")
    public Result<Object> batchOrder(@Valid @RequestBody BatchOrderRequest request) {
        return Result.ok(enterpriseService.createBatchOrder(request));
    }
}
