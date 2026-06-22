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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final CommerceService commerceService;

    public Page<Product> search(ProductSearchRequest request, int page, int size) {
        String keyword = request.getKeyword();

        // Try external commerce API search first
        List<Product> externalProducts = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            externalProducts = commerceService.searchAcrossPlatforms(keyword, page, size);
            // Save external products to local DB for future queries
            for (Product p : externalProducts) {
                try { commerceService.saveProduct(p); } catch (Exception ignored) {}
            }
        }

        // Also search local DB
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Product::getName, keyword)
                    .or().like(Product::getDescription, keyword));
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            wrapper.eq(Product::getCategory, request.getCategory());
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

        Page<Product> p = new Page<>(page, size);
        Page<Product> dbResult = productMapper.selectPage(p, wrapper);

        // Merge: external results take priority, then local DB
        if (!externalProducts.isEmpty()) {
            List<Product> merged = new ArrayList<>(externalProducts);
            for (Product dbProduct : dbResult.getRecords()) {
                boolean exists = merged.stream().anyMatch(ep ->
                        ep.getName().equals(dbProduct.getName()) &&
                        ep.getPlatform().equals(dbProduct.getPlatform()));
                if (!exists) merged.add(dbProduct);
            }
            Page<Product> mergedPage = new Page<>(page, size);
            mergedPage.setRecords(merged);
            mergedPage.setTotal(merged.size());
            return mergedPage;
        }

        return dbResult;
    }

    public Product getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }
}
