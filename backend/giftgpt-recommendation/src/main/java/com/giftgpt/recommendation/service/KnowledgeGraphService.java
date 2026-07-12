package com.giftgpt.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftgpt.goods.entity.Product;
import com.giftgpt.goods.mapper.ProductMapper;
import com.giftgpt.recommendation.dto.RecommendItem;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.entity.RecipientTag;
import com.giftgpt.user.mapper.RecipientMapper;
import com.giftgpt.user.mapper.RecipientTagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private final RecipientMapper recipientMapper;
    private final RecipientTagMapper recipientTagMapper;
    private final ProductMapper productMapper;

    @Value("${giftgpt.kg.enabled:false}")
    private boolean enabled;

    @Value("${giftgpt.kg.uri:bolt://localhost:7687}")
    private String uri;

    @Value("${giftgpt.kg.user:neo4j}")
    private String user;

    @Value("${giftgpt.kg.password:giftgpt123}")
    private String password;

    @Value("${giftgpt.kg.taxonomy-file:classpath:kg_taxonomy.json}")
    private String taxonomyFile;

    private Driver driver;
    private JsonNode taxonomy;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, String> optNameToCategoryId = new HashMap<>();
    private final Map<String, String> categoryIdToName = new HashMap<>();
    private final Map<String, String> occasionCodeToName = new HashMap<>();

    public boolean isEnabled() {
        return enabled && driver != null;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (!enabled) {
            log.info("KG disabled (giftgpt.kg.enabled=false), skipping Neo4j init");
            return;
        }
        try {
            loadTaxonomy();
            driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
            driver.verifyConnectivity();
            log.info("Neo4j connected: {}", uri);
            buildSchema();
            syncDataAndBuildGraph();
            log.info("Knowledge graph built successfully");
        } catch (Exception e) {
            log.warn("KG init failed, KG recommendations will be skipped: {}", e.getMessage());
            driver = null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (driver != null) {
            driver.close();
            log.info("Neo4j driver closed");
        }
    }

    private void loadTaxonomy() throws Exception {
        InputStream is;
        if (taxonomyFile.startsWith("classpath:")) {
            is = new ClassPathResource(taxonomyFile.substring("classpath:".length())).getInputStream();
        } else {
            is = new java.io.FileInputStream(taxonomyFile);
        }
        taxonomy = objectMapper.readTree(is);
        is.close();

        for (JsonNode cat : taxonomy.path("standard_categories")) {
            categoryIdToName.put(cat.get("id").asText(), cat.get("name").asText());
        }
        for (Map.Entry<String, JsonNode> entry : fieldsToList(taxonomy.path("pdd_opt_to_category"))) {
            String categoryId = entry.getKey();
            for (JsonNode optName : entry.getValue()) {
                optNameToCategoryId.put(optName.asText(), categoryId);
            }
        }
        for (Map.Entry<String, JsonNode> entry : fieldsToList(taxonomy.path("occasion_codes"))) {
            occasionCodeToName.put(entry.getKey(), entry.getValue().asText());
        }
        log.info("Taxonomy loaded: {} categories, {} opt_name mappings, {} occasions",
                categoryIdToName.size(), optNameToCategoryId.size(), occasionCodeToName.size());
    }

    private void buildSchema() {
        try (Session session = driver.session()) {
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (r:Recipient) REQUIRE r.id IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (t:Tag) REQUIRE t.name IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (c:Category) REQUIRE c.id IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (p:Product) REQUIRE p.id IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (o:Occasion) REQUIRE o.code IS UNIQUE");
        }
        log.info("Neo4j schema constraints created");
    }

    private void syncDataAndBuildGraph() {
        createCategoryAndOccasionNodes();
        buildTagCategoryRelations();
        buildCategoryOccasionRelations();
        syncRecipients();
        syncProducts();
    }

    private void createCategoryAndOccasionNodes() {
        try (Session session = driver.session()) {
            for (JsonNode cat : taxonomy.path("standard_categories")) {
                session.run("MERGE (c:Category {id: $id}) SET c.name = $name",
                        Map.of("id", cat.get("id").asText(), "name", cat.get("name").asText()));
            }
            for (Map.Entry<String, String> entry : occasionCodeToName.entrySet()) {
                session.run("MERGE (o:Occasion {code: $code}) SET o.name = $name",
                        Map.of("code", entry.getKey(), "name", entry.getValue()));
            }
        }
        log.info("Created {} category nodes, {} occasion nodes",
                categoryIdToName.size(), occasionCodeToName.size());
    }

    private void buildTagCategoryRelations() {
        try (Session session = driver.session()) {
            for (Map.Entry<String, JsonNode> entry : fieldsToList(taxonomy.path("tag_to_categories"))) {
                String tagName = entry.getKey();
                for (JsonNode catId : entry.getValue()) {
                    session.run(
                        "MERGE (t:Tag {name: $tagName}) " +
                        "MERGE (c:Category {id: $catId}) " +
                        "MERGE (t)-[:PREFERS_CATEGORY]->(c)",
                        Map.of("tagName", tagName, "catId", catId.asText()));
                }
            }
        }
        log.info("Built Tag -> Category relationships");
    }

    private void buildCategoryOccasionRelations() {
        try (Session session = driver.session()) {
            for (Map.Entry<String, JsonNode> entry : fieldsToList(taxonomy.path("category_to_occasions"))) {
                String catId = entry.getKey();
                for (JsonNode occCode : entry.getValue()) {
                    session.run(
                        "MERGE (c:Category {id: $catId}) " +
                        "MERGE (o:Occasion {code: $occCode}) " +
                        "MERGE (c)-[:FIT_OCCASION]->(o)",
                        Map.of("catId", catId, "occCode", occCode.asText()));
                }
            }
        }
        log.info("Built Category -> Occasion relationships");
    }

    private void syncRecipients() {
        List<Recipient> recipients = recipientMapper.selectList(null);
        try (Session session = driver.session()) {
            for (Recipient r : recipients) {
                session.run(
                    "MERGE (r:Recipient {id: $id}) " +
                    "SET r.name = $name, r.mbti = $mbti, r.gender = $gender, r.relation = $relation",
                    Map.of("id", r.getId(), "name", nullSafe(r.getName()),
                           "mbti", nullSafe(r.getMbti()), "gender", r.getGender(),
                           "relation", nullSafe(r.getRelation())));

                List<RecipientTag> tags = recipientTagMapper.selectList(
                        new LambdaQueryWrapper<RecipientTag>().eq(RecipientTag::getRecipientId, r.getId()));
                for (RecipientTag tag : tags) {
                    session.run(
                        "MERGE (r:Recipient {id: $rid}) " +
                        "MERGE (t:Tag {name: $tagName}) " +
                        "MERGE (r)-[:HAS_TAG]->(t)",
                        Map.of("rid", r.getId(), "tagName", tag.getTagName()));
                }
            }
        }
        log.info("Synced {} recipients with tags", recipients.size());
    }

    private void syncProducts() {
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1));
        int linked = 0;
        try (Session session = driver.session()) {
            for (Product p : products) {
                String categoryId = resolveCategory(p.getCategory(), p.getName());
                if (categoryId == null) continue;

                session.run(
                    "MERGE (p:Product {id: $id}) " +
                    "SET p.name = $name, p.price = $price, p.platform = $platform, " +
                    "p.imageUrl = $imageUrl, p.salesCount = $salesCount " +
                    "WITH p " +
                    "MERGE (c:Category {id: $catId}) " +
                    "MERGE (p)-[:BELONGS_TO]->(c)",
                    Map.of("id", p.getId(),
                           "name", nullSafe(p.getName()),
                           "price", p.getPrice() != null ? p.getPrice().doubleValue() : 0.0,
                           "platform", nullSafe(p.getPlatform()),
                           "imageUrl", nullSafe(p.getImageUrl()),
                           "salesCount", p.getSalesCount() != null ? p.getSalesCount() : 0,
                           "catId", categoryId));
                linked++;
            }
        }
        log.info("Synced {} products ({} linked to categories)", products.size(), linked);
    }

    private String resolveCategory(String optName, String productName) {
        if (optName != null) {
            String catId = optNameToCategoryId.get(optName);
            if (catId != null) return catId;
            for (Map.Entry<String, String> entry : optNameToCategoryId.entrySet()) {
                if (optName.contains(entry.getKey())) return entry.getValue();
            }
        }
        if (productName != null) {
            String lower = productName.toLowerCase();
            for (Map.Entry<String, String> entry : optNameToCategoryId.entrySet()) {
                if (entry.getKey().length() >= 2 && lower.contains(entry.getKey().toLowerCase())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public List<RecommendItem> queryRecommendations(Long recipientId, String occasion, BigDecimal budgetMax) {
        if (!isEnabled()) return Collections.emptyList();

        double budgetMaxVal = budgetMax != null ? budgetMax.doubleValue() : 10000.0;
        double budgetMinVal = budgetMaxVal * 0.6;

        String cypher =
            "MATCH (r:Recipient {id: $recipientId})-[:HAS_TAG]->(t:Tag)-[:PREFERS_CATEGORY]->(c:Category) " +
            "MATCH (p:Product)-[:BELONGS_TO]->(c) " +
            "MATCH (c)-[:FIT_OCCASION]->(o:Occasion {code: $occasion}) " +
            "WHERE p.price >= $budgetMin AND p.price <= $budgetMax " +
            "WITH p, c, collect(DISTINCT t.name) AS matchedTags, count(DISTINCT t) AS tagScore " +
            "ORDER BY tagScore DESC, p.salesCount DESC " +
            "LIMIT 8 " +
            "RETURN p.id AS productId, p.name AS productName, p.price AS price, " +
            "p.imageUrl AS imageUrl, p.platform AS platform, " +
            "p.salesCount AS salesCount, " +
            "matchedTags, tagScore, " +
            "c.name AS categoryName";

        List<RecommendItem> items = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of(
                    "recipientId", recipientId,
                    "occasion", occasion,
                    "budgetMin", budgetMinVal,
                    "budgetMax", budgetMaxVal));

            while (result.hasNext()) {
                Record record = result.next();
                RecommendItem item = new RecommendItem();
                item.setProductId(record.get("productId").asLong());
                item.setProductName(record.get("productName").asString());
                item.setPrice(BigDecimal.valueOf(record.get("price").asDouble()));
                item.setImageUrl(record.get("imageUrl").isNull() ? "" : record.get("imageUrl").asString());
                item.setPlatform(record.get("platform").isNull() ? "\u62fc\u591a\u591a" : record.get("platform").asString());
                item.setPlatformUrl("");
                item.setScore(0.85 + Math.min(record.get("tagScore").asInt() * 0.05, 0.15));

                List<String> matchedTags = new ArrayList<>();
                record.get("matchedTags").asList().forEach(v -> matchedTags.add((String) v));
                item.setMatchTags(matchedTags);

                String tagNameStr = String.join("\u3001", matchedTags);
                String categoryName = record.get("categoryName").asString();
                String occasionName = occasionCodeToName.getOrDefault(occasion, occasion);
                String productName = record.get("productName").asString();
                item.setReasoningChain(String.format(
                    "(%s)\u2192\u504f\u597d\u2192(%s)\u2192\u5305\u542b\u2192(%s)\u2192\u9002\u5408\u2192(%s)",
                    tagNameStr, categoryName, productName, occasionName));
                item.setReason(String.format(
                    "\u57fa\u4e8e%s\u5174\u8da3\u504f\u597d\uff0c\u5339\u914d%s\u54c1\u7c7b",
                    tagNameStr, categoryName));

                items.add(item);
            }
        } catch (Exception e) {
            log.warn("KG query failed: {}", e.getMessage());
        }
        log.info("KG query: recipientId={}, occasion={}, budget={} -> {} items",
                recipientId, occasion, budgetMax, items.size());
        return items;
    }

    public void resyncProducts() {
        if (!isEnabled()) return;
        try {
            syncProducts();
        } catch (Exception e) {
            log.warn("KG product re-sync failed: {}", e.getMessage());
        }
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }

    private static List<Map.Entry<String, JsonNode>> fieldsToList(JsonNode node) {
        List<Map.Entry<String, JsonNode>> result = new ArrayList<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(result::add);
        }
        return result;
    }
}