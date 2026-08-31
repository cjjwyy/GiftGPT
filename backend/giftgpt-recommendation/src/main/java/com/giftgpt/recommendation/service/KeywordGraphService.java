package com.giftgpt.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftgpt.user.entity.Recipient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关键词权重图谱：读取 kg_keywords.json（由标注工具/迁移脚本聚合生成）。
 *
 * 结构:
 *   {
 *     "gender":   { "男": {"男士香水": 2, ...}, ... },
 *     "relation": { "恋人": {"送女朋友礼物": 3, ...}, ... },
 *     "tag":      { "音乐": {"吉他 礼物": 2, ...}, ... }
 *   }
 *
 * 推荐时按收礼人性别/关系/标签取关键词，按权重降序排列，直接用于商品搜索。
 */
@Slf4j
@Service
public class KeywordGraphService {

    @Value("${giftgpt.keyword-graph.file:classpath:kg_keywords.json}")
    private String filePath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // nodeType -> nodeName -> keyword -> weight
    private Map<String, Map<String, Map<String, Integer>>> graph = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        try {
            InputStream is;
            if (filePath.startsWith("classpath:")) {
                is = new ClassPathResource(filePath.substring("classpath:".length())).getInputStream();
            } else {
                is = new java.io.FileInputStream(filePath);
            }
            JsonNode root = objectMapper.readTree(is);
            is.close();
            for (String nodeType : new String[]{"gender", "relation", "tag"}) {
                JsonNode sub = root.path(nodeType);
                Map<String, Map<String, Integer>> nodeMap = new LinkedHashMap<>();
                if (sub.isObject()) {
                    sub.fields().forEachRemaining(entry -> {
                        Map<String, Integer> kwMap = new LinkedHashMap<>();
                        if (entry.getValue().isObject()) {
                            entry.getValue().fields().forEachRemaining(kw -> {
                                int w = kw.getValue().asInt(1);
                                if (w > 0) kwMap.put(kw.getKey(), w);
                            });
                        }
                        if (!kwMap.isEmpty()) nodeMap.put(entry.getKey(), kwMap);
                    });
                }
                graph.put(nodeType, nodeMap);
            }
            log.info("KeywordGraph loaded: {} genders, {} relations, {} tags",
                    graph.get("gender").size(), graph.get("relation").size(), graph.get("tag").size());
        } catch (Exception e) {
            log.warn("Failed to load keyword graph from {}: {}", filePath, e.getMessage());
        }
    }

    public boolean isLoaded() {
        return !graph.isEmpty();
    }

    /** 按权重降序取关键词（同一关键词从多个节点命中时权重累加）。 */
    public List<KeywordHit> pickKeywords(Recipient recipient, List<String> tags) {
        Map<String, Integer> merged = new LinkedHashMap<>();
        if (recipient != null) {
            addNode(merged, "gender", mapGender(recipient.getGender()));
            addNode(merged, "relation", recipient.getRelation());
        }
        if (tags != null) {
            for (String tag : tags) {
                addNode(merged, "tag", tag);
            }
        }

        List<KeywordHit> hits = new ArrayList<>();
        for (Map.Entry<String, Integer> e : merged.entrySet()) {
            hits.add(new KeywordHit(e.getKey(), e.getValue()));
        }
        hits.sort((a, b) -> Integer.compare(b.getWeight(), a.getWeight()));
        return hits;
    }

    private void addNode(Map<String, Integer> merged, String nodeType, String nodeName) {
        if (nodeName == null || nodeName.isBlank()) return;
        Map<String, Map<String, Integer>> nodeMap = graph.get(nodeType);
        if (nodeMap == null) return;
        Map<String, Integer> kws = nodeMap.get(nodeName);
        if (kws == null) return;
        for (Map.Entry<String, Integer> e : kws.entrySet()) {
            merged.merge(e.getKey(), e.getValue(), Integer::sum);
        }
    }

    private String mapGender(Integer gender) {
        if (gender == null || gender == 0) return "";
        return gender == 1 ? "男" : "女";
    }

    public static class KeywordHit {
        private final String keyword;
        private final int weight;

        public KeywordHit(String keyword, int weight) {
            this.keyword = keyword;
            this.weight = weight;
        }

        public String getKeyword() { return keyword; }
        public int getWeight() { return weight; }
    }
}
