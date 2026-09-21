package com.giftgpt.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.goods.entity.Product;
import com.giftgpt.goods.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 商品平台门面：聚合所有 ProductSearchProvider，并统一负责缓存、相关性过滤和本地落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommerceService {

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final long EMPTY_CACHE_TTL_MS = 60 * 1000L;
    private static final int MAX_CACHE_ENTRIES = 500;

    private final ProductMapper productMapper;
    private final List<ProductSearchProvider> providers;
    private final Map<String, CachedEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Object> keyLocks = new ConcurrentHashMap<>();

    private static class CachedEntry {
        private final List<Product> products;
        private final long expireAt;

        private CachedEntry(List<Product> products, long ttlMs) {
            this.products = Collections.unmodifiableList(new ArrayList<>(products));
            this.expireAt = System.currentTimeMillis() + ttlMs;
        }
    }

    public List<Product> searchAcrossPlatforms(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();
        String normalized = normalizeKeyword(keyword);
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        return searchCached("all#" + normalized + "#" + safePage + "#" + safeSize,
                normalized, safePage, safeSize, null);
    }

    public List<Product> searchByPlatform(String keyword, String platform, int page, int size) {
        if (keyword == null || keyword.isBlank() || platform == null || platform.isBlank()) {
            return Collections.emptyList();
        }
        ProductSearchProvider provider = providers.stream()
                .filter(candidate -> platformMatches(candidate.platformName(), platform))
                .findFirst()
                .orElse(null);
        if (provider == null) {
            log.info("Unsupported product platform requested: {}", platform);
            return Collections.emptyList();
        }
        String normalized = normalizeKeyword(keyword);
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        String key = provider.platformName() + "#" + normalized + "#" + safePage + "#" + safeSize;
        return searchCached(key, normalized, safePage, safeSize, provider);
    }

    private List<Product> searchCached(String key, String keyword, int page, int size,
                                       ProductSearchProvider selectedProvider) {
        CachedEntry hit = validEntry(key);
        if (hit != null) return new ArrayList<>(hit.products);

        Object lock = keyLocks.computeIfAbsent(key, ignored -> new Object());
        try {
            synchronized (lock) {
                hit = validEntry(key);
                if (hit != null) return new ArrayList<>(hit.products);
                List<Product> result = doSearchAndUpsert(keyword, page, size, selectedProvider);
                pruneCache();
                cache.put(key, new CachedEntry(result,
                        result.isEmpty() ? EMPTY_CACHE_TTL_MS : CACHE_TTL_MS));
                return new ArrayList<>(result);
            }
        } finally {
            keyLocks.remove(key, lock);
        }
    }

    private List<Product> doSearchAndUpsert(String keyword, int page, int size,
                                            ProductSearchProvider selectedProvider) {
        List<Product> apiResults = new ArrayList<>();
        for (ProductSearchProvider provider : providers) {
            if (selectedProvider != null && provider != selectedProvider) continue;
            if (!provider.isConfigured()) continue;
            try {
                List<Product> found = provider.search(keyword, page, size);
                if (found != null) apiResults.addAll(found);
            } catch (Exception e) {
                log.warn("{} search failed for '{}': {}", provider.platformName(), keyword, e.getMessage());
            }
        }

        List<Product> relevant = new ArrayList<>();
        for (Product product : apiResults) {
            try {
                Product saved = saveProduct(product);
                if (isRelevant(saved, keyword)) relevant.add(saved);
            } catch (Exception e) {
                log.warn("saveProduct failed: {} / {}", product == null ? "" : product.getName(),
                        product == null ? "" : product.getPlatform(), e);
            }
        }
        return relevant;
    }

    /** Upsert 平台商品。平台和名称组成业务唯一键。 */
    public synchronized Product saveProduct(Product product) {
        if (product == null || product.getName() == null || product.getName().isBlank()
                || product.getPrice() == null || product.getPrice().signum() <= 0) {
            throw new IllegalArgumentException("Invalid product from provider");
        }
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
            existing.setStatus(product.getStatus() == null ? 1 : product.getStatus());
            if (product.getDescription() != null && !product.getDescription().isBlank()) {
                existing.setDescription(product.getDescription());
            }
            if (product.getCategory() != null && !product.getCategory().isBlank()) {
                existing.setCategory(product.getCategory());
            }
            productMapper.updateById(existing);
            return existing;
        }
        if (product.getStatus() == null) product.setStatus(1);
        productMapper.insert(product);
        return product;
    }

    public List<Product> searchLocal(String keyword, int limit) {
        Page<Product> page = new Page<>(1, Math.max(1, Math.min(limit, 100)));
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Product::getName, keyword.trim());
        }
        wrapper.orderByDesc(Product::getSalesCount);
        return productMapper.selectPage(page, wrapper).getRecords();
    }

    private CachedEntry validEntry(String key) {
        CachedEntry entry = cache.get(key);
        if (entry == null) return null;
        if (entry.expireAt <= System.currentTimeMillis()) {
            cache.remove(key, entry);
            return null;
        }
        return entry;
    }

    private boolean isRelevant(Product product, String keyword) {
        if (product == null || product.getName() == null) return false;
        if (isGenericGiftKeyword(keyword)) return true;
        String name = product.getName().toLowerCase(Locale.ROOT);
        String description = product.getDescription() == null
                ? "" : product.getDescription().toLowerCase(Locale.ROOT);
        for (String token : keyword.toLowerCase(Locale.ROOT).split("[\\s,，、/]+")) {
            if (token.length() >= 2 && (name.contains(token) || description.contains(token))) {
                return true;
            }
        }
        return false;
    }

    private boolean isGenericGiftKeyword(String keyword) {
        return "礼物".equals(keyword) || "礼品".equals(keyword) || "送礼".equals(keyword);
    }

    private boolean platformMatches(String providerName, String requested) {
        if (providerName.equalsIgnoreCase(requested)) return true;
        return "拼多多".equals(providerName)
                && ("pdd".equalsIgnoreCase(requested) || "pinduoduo".equalsIgnoreCase(requested));
    }

    private String normalizeKeyword(String keyword) {
        return keyword.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private int safePage(int page) {
        return Math.max(1, page);
    }

    private int safeSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private void pruneCache() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> entry.getValue().expireAt <= now);
        if (cache.size() < MAX_CACHE_ENTRIES) return;
        cache.entrySet().stream()
                .min(Comparator.comparingLong(entry -> entry.getValue().expireAt))
                .ifPresent(entry -> cache.remove(entry.getKey(), entry.getValue()));
    }
}
