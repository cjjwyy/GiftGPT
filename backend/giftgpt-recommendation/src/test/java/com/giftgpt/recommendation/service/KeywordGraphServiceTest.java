package com.giftgpt.recommendation.service;

import com.giftgpt.user.entity.Recipient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class KeywordGraphServiceTest {
    private KeywordGraphService graph() {
        KeywordGraphService graph = new KeywordGraphService();
        ReflectionTestUtils.setField(graph, "filePath", "classpath:kg_keywords.json");
        graph.init();
        return graph;
    }

    @Test
    void canonicalAliasesAndThreeDimensionEvidence() {
        KeywordGraphService graph = graph();
        assertTrue(graph.isLoaded());
        assertEquals("朋友", graph.canonical("relation", "闺蜜"));
        assertEquals("美妆护肤", graph.canonical("tag", "爱美"));
        Recipient r = new Recipient();
        r.setRelation("朋友");
        List<KeywordGraphService.KeywordHit> hits = graph.pickKeywords(r, List.of("文艺"), "birthday");
        KeywordGraphService.KeywordHit hit = graph.match("精选金属书签礼盒", hits);
        assertNotNull(hit);
        assertTrue(hit.explanation().contains("文艺"));
        assertTrue(hit.explanation().contains("朋友"));
        assertTrue(hit.explanation().contains("生日"));
        assertTrue(hit.evidenceChain().contains("独立原文支持"));
        assertNull(graph.match("没有任何图谱关联的特殊物品", hits));
    }

    @Test
    void aliasesDoNotInflateScoresAndUnknownOccasionsAreNotOther() {
        KeywordGraphService graph = graph();
        Recipient r = new Recipient();
        List<KeywordGraphService.KeywordHit> a = graph.pickKeywords(r, List.of("科技"), "unrecognized");
        List<KeywordGraphService.KeywordHit> b = graph.pickKeywords(r, List.of("科技", "极客"), "unrecognized");
        assertFalse(a.isEmpty());
        assertEquals(a.size(), b.size());
        for (int i=0; i<a.size(); i++) {
            assertEquals(a.get(i).getWeight(), b.get(i).getWeight());
            assertFalse(a.get(i).explanation().contains("场景"));
        }
    }
}
