package com.giftgpt.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.goods.dto.ProductSearchRequest;
import com.giftgpt.goods.entity.Product;
import com.giftgpt.goods.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final CommerceService commerceService;

    public Page<Product> search(ProductSearchRequest request, int page, int size) {
        String keyword = request.getKeyword();

        // Fetch fresh results from platform APIs when keyword is provided
        if (keyword != null && !keyword.isBlank()) {
            if (request.getPlatform() != null && !request.getPlatform().isBlank()) {
                commerceService.searchByPlatform(keyword, request.getPlatform(), page, size);
            } else {
                commerceService.searchAcrossPlatforms(keyword, page, size);
            }
        }

        // Also query local DB for cached products
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Product::getName, keyword)
                    .or().like(Product::getDescription, keyword));
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            wrapper.eq(Product::getCategory, request.getCategory());
        }
        if (request.getPlatform() != null && !request.getPlatform().isBlank()) {
            wrapper.eq(Product::getPlatform, request.getPlatform());
        }
        if (request.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, request.getMinPrice());
        }
        if (request.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, request.getMaxPrice());
        }

        if ("price_asc".equals(request.getSort())) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(request.getSort())) {
            wrapper.orderByDesc(Product::getPrice);
        } else {
            wrapper.orderByDesc(Product::getSalesCount);
        }

        // 实时结果已先落入本地库，统一由数据库分页，避免“外部本页数 + DB 总数”的重复统计。
        Page<Product> p = new Page<>(Math.max(1, page), Math.max(1, Math.min(size, 100)));
        return productMapper.selectPage(p, wrapper);
    }

    public Product getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }
}
