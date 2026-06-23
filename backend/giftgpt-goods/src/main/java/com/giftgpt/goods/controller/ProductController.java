package com.giftgpt.goods.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.result.Result;
import com.giftgpt.goods.dto.ProductSearchRequest;
import com.giftgpt.goods.entity.Product;
import com.giftgpt.goods.service.PddService;
import com.giftgpt.goods.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "商品服务", description = "商品搜索、详情")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final PddService pddService;

    @Operation(summary = "商品搜索")
    @GetMapping("/search")
    public Result<Page<Product>> search(
            ProductSearchRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return Result.ok(productService.search(request, page, size));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    @Operation(summary = "拼多多推广位授权状态")
    @GetMapping("/platforms/pinduoduo/authority")
    public Result<Map<String, Object>> pddAuthority() {
        return Result.ok(pddService.getAuthorityStatus());
    }
}
