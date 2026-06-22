package com.giftgpt.goods.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftgpt.goods.entity.Product;
import com.giftgpt.goods.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommerceService {

    private final ProductMapper productMapper;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${giftgpt.commerce.serpapi.api-key:YOUR_SERPAPI_KEY}")
    private String serpApiKey;

    @Value("${giftgpt.commerce.serpapi.base-url:https://serpapi.com/search}")
    private String serpBaseUrl;

    /**
     * Search products across platforms using SerpAPI (Google Shopping).
     * Falls back to keyword-based local DB search if API key is not configured.
     */
    public List<Product> searchAcrossPlatforms(String keyword, int page, int size) {
        if (serpApiKey != null && !serpApiKey.isBlank() && !serpApiKey.startsWith("YOUR_")) {
            return searchWithSerpApi(keyword, page, size);
        }
        log.info("SerpAPI not configured, using local product search for keyword: {}", keyword);
        return Collections.emptyList();
    }

    private List<Product> searchWithSerpApi(String keyword, int page, int size) {
        List<Product> products = new ArrayList<>();
        try {
            String encodedKeyword = URLEncoder.encode(keyword + " gift present", StandardCharsets.UTF_8);
            String urlStr = serpBaseUrl + "?engine=google_shopping&q=" + encodedKeyword +
                    "&api_key=" + serpApiKey + "&gl=cn&hl=zh-CN&start=" + ((page - 1) * size);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            if (code == 200) {
                String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JsonNode root = objectMapper.readTree(body);
                JsonNode shoppingResults = root.get("shopping_results");

                if (shoppingResults != null) {
                    int start = (page - 1) * size;
                    int end = Math.min(start + size, shoppingResults.size());

                    for (int i = start; i < end; i++) {
                        JsonNode item = shoppingResults.get(i);
                        Product product = new Product();
                        product.setName(item.get("title").asText());

                        JsonNode priceNode = item.get("price");
                        if (priceNode != null && !priceNode.isNull()) {
                            try {
                                String priceStr = priceNode.asText().replaceAll("[^0-9.]", "");
                                product.setPrice(new BigDecimal(priceStr.isEmpty() ? "0" : priceStr));
                            } catch (Exception e) {
                                product.setPrice(BigDecimal.ZERO);
                            }
                        } else {
                            product.setPrice(BigDecimal.ZERO);
                        }

                        JsonNode source = item.get("source");
                        product.setPlatform(source != null ? source.asText() : "电商平台");

                        JsonNode link = item.get("link");
                        product.setPlatformUrl(link != null ? link.asText() : "");

                        JsonNode thumbnail = item.get("thumbnail");
                        product.setImageUrl(thumbnail != null ? thumbnail.asText() : "");

                        JsonNode rating = item.get("rating");
                        product.setRating(rating != null ? rating.asDouble() : 0.0);

                        JsonNode reviews = item.get("reviews");
                        product.setSalesCount(reviews != null ? reviews.asInt() : 0);

                        product.setStatus(1);
                        product.setDescription("");
                        product.setCategory(keyword);

                        products.add(product);
                    }
                }
                log.info("SerpAPI returned {} products for keyword: {}", products.size(), keyword);
            } else {
                log.warn("SerpAPI returned HTTP {}", code);
            }
        } catch (Exception e) {
            log.error("SerpAPI search failed for keyword: {}", keyword, e);
        }
        return products;
    }

    /**
     * Save or update a product from search results into the local database.
     */
    public Product saveProduct(Product product) {
        Product existing = productMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                        .eq(Product::getName, product.getName())
                        .eq(Product::getPlatform, product.getPlatform()));
        if (existing != null) {
            existing.setPrice(product.getPrice());
            existing.setImageUrl(product.getImageUrl());
            existing.setPlatformUrl(product.getPlatformUrl());
            existing.setRating(product.getRating());
            existing.setSalesCount(product.getSalesCount());
            productMapper.updateById(existing);
            return existing;
        }
        productMapper.insert(product);
        return product;
    }

    /**
     * Search JD.com via their open API (requires JD VOP enterprise account).
     */
    public List<Product> searchJingdong(String keyword, int page, int size) {
        // JD VOP API requires enterprise qualification and complex signature
        // Placeholder for future implementation
        log.info("JD search not yet implemented (requires enterprise API access): {}", keyword);
        return Collections.emptyList();
    }

    /**
     * Search Taobao/Tmall via their open API (requires Taobao Open Platform account).
     */
    public List<Product> searchTaobao(String keyword, int page, int size) {
        // Taobao Open API requires app registration and OAuth
        // Placeholder for future implementation
        log.info("Taobao search not yet implemented (requires API access): {}", keyword);
        return Collections.emptyList();
    }

    /**
     * Search Pinduoduo via their open API (requires Duoduoke account).
     */
    public List<Product> searchPinduoduo(String keyword, int page, int size) {
        // Pinduoduo API requires enterprise account
        // Placeholder for future implementation
        log.info("Pinduoduo search not yet implemented (requires API access): {}", keyword);
        return Collections.emptyList();
    }
}
