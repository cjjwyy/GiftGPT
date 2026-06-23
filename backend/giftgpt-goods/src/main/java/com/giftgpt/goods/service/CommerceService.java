package com.giftgpt.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.goods.entity.Product;
import com.giftgpt.goods.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Unified commerce facade that dispatches to platform-specific API services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommerceService {

    private final ProductMapper productMapper;
    private final JdUnionService jdUnionService;
    private final TbkService tbkService;
    private final PddService pddService;

    /**
     * Search all available platforms in parallel.
     */
    public List<Product> searchAcrossPlatforms(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();

        CompletableFuture<List<Product>> jd = CompletableFuture.supplyAsync(
                () -> jdUnionService.searchGoods(keyword, page, size));
        CompletableFuture<List<Product>> tb = CompletableFuture.supplyAsync(
                () -> tbkService.searchGoods(keyword, page, size));
        CompletableFuture<List<Product>> pdd = CompletableFuture.supplyAsync(
                () -> pddService.searchGoods(keyword, page, size));

        List<Product> all = new ArrayList<>();
        all.addAll(safeGet(jd, 15));
        all.addAll(safeGet(tb, 15));
        all.addAll(safeGet(pdd, 15));

        // Cache results in local DB
        for (Product p : all) {
            try { saveProduct(p); } catch (Exception ignored) {}
        }

        return all;
    }

    /**
     * Search a specific platform only.
     */
    public List<Product> searchByPlatform(String keyword, String platform, int page, int size) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();
        List<Product> result;
        switch (platform) {
            case "京东": result = jdUnionService.searchGoods(keyword, page, size); break;
            case "淘宝": result = tbkService.searchGoods(keyword, page, size); break;
            case "拼多多": result = pddService.searchGoods(keyword, page, size); break;
            default: result = searchAcrossPlatforms(keyword, page, size);
        }
        for (Product p : result) {
            try { saveProduct(p); } catch (Exception ignored) {}
        }
        return result;
    }

    /**
     * Upsert a product into the local database.
     */
    public Product saveProduct(Product product) {
        Product existing = productMapper.selectOne(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getName, product.getName())
                        .eq(Product::getPlatform, product.getPlatform()));
        if (existing != null) {
            existing.setPrice(product.getPrice());
            existing.setImageUrl(product.getImageUrl());
            existing.setPlatformUrl(product.getPlatformUrl());
            existing.setRating(product.getRating());
            existing.setSalesCount(product.getSalesCount());
            if (product.getDescription() != null && !product.getDescription().isBlank()) {
                existing.setDescription(product.getDescription());
            }
            if (product.getCategory() != null && !product.getCategory().isBlank()) {
                existing.setCategory(product.getCategory());
            }
            productMapper.updateById(existing);
            return existing;
        }
        productMapper.insert(product);
        return product;
    }

    /**
     * Search local DB for products matching a keyword (used for recommendation matching).
     */
    public List<Product> searchLocal(String keyword, int limit) {
        Page<Product> p = new Page<>(1, limit);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Product::getName, keyword);
        }
        wrapper.orderByDesc(Product::getSalesCount);
        return productMapper.selectPage(p, wrapper).getRecords();
    }

    private static List<Product> safeGet(CompletableFuture<List<Product>> future, int timeoutSeconds) {
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Platform API call failed or timed out: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
