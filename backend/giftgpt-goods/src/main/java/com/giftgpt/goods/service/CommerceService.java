package com.giftgpt.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.goods.entity.Product;
import com.giftgpt.goods.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Commerce facade that dispatches to Pinduoduo API service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommerceService {

    private final ProductMapper productMapper;
    private final PddService pddService;

    /**
     * Search Pinduoduo for products matching the keyword.
     */
    public List<Product> searchAcrossPlatforms(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();

        List<Product> apiResults = pddService.searchGoods(keyword, page, size);
        List<Product> all = new ArrayList<>();
        for (Product p : apiResults) {
            try { all.add(saveProduct(p)); } catch (Exception e) { log.warn("saveProduct failed: {} / {}", p.getName(), p.getPlatform(), e); }
        }
        return all;
    }

    /**
     * Search a specific platform only (only 拼多多 supported).
     */
    public List<Product> searchByPlatform(String keyword, String platform, int page, int size) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();
        List<Product> result = pddService.searchGoods(keyword, page, size);
        for (Product p : result) {
            try { saveProduct(p); } catch (Exception e) { log.warn("saveProduct failed: {} / {}", p.getName(), p.getPlatform(), e); }
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
}
