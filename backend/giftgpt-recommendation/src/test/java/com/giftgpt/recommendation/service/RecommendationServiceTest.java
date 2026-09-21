package com.giftgpt.recommendation.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.StpUtil;
import com.giftgpt.common.ai.DeepseekClient;
import com.giftgpt.goods.entity.Product;
import com.giftgpt.goods.mapper.ProductMapper;
import com.giftgpt.goods.service.CommerceService;
import com.giftgpt.recommendation.dto.AiGift;
import com.giftgpt.recommendation.dto.AiGiftsResponse;
import com.giftgpt.recommendation.dto.MatchRequest;
import com.giftgpt.recommendation.dto.RecommendItem;
import com.giftgpt.recommendation.dto.RecommendRequest;
import com.giftgpt.recommendation.dto.RecommendResponse;
import com.giftgpt.recommendation.mapper.RecommendationHistoryMapper;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.entity.RecipientTag;
import com.giftgpt.user.mapper.RecipientMapper;
import com.giftgpt.user.mapper.RecipientTagMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 推荐链路核心单测（纯 Mockito，不启动 Spring 上下文）。
 * 覆盖：LLM 失败降级、LLM 结果解析、KG 通道合并去重。
 *
 * 登录态通过给 Sa-Token 配置 ThreadLocal 存储上下文实现：
 * StpUtil.login() 依赖 SaManager.getSaTokenContext() 提供 request/storage，
 * 这里用 Map 实现 SaStorage + Mockito mock 的 SaRequest/SaResponse 建立最小可用上下文，
 * 从而让 StpUtil.getLoginIdAsLong() 在无 Web 容器环境下返回真实登录态。
 */
class RecommendationServiceTest {

    private RecipientMapper recipientMapper;
    private RecipientTagMapper recipientTagMapper;
    private RecommendationHistoryMapper historyMapper;
    private ProductMapper productMapper;
    private CommerceService commerceService;
    private KnowledgeGraphService knowledgeGraphService;
    private KeywordGraphService keywordGraphService;
    private DeepseekClient deepseekClient;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        recipientMapper = mock(RecipientMapper.class);
        recipientTagMapper = mock(RecipientTagMapper.class);
        historyMapper = mock(RecommendationHistoryMapper.class);
        productMapper = mock(ProductMapper.class);
        commerceService = mock(CommerceService.class);
        knowledgeGraphService = mock(KnowledgeGraphService.class);
        keywordGraphService = mock(KeywordGraphService.class);
        deepseekClient = mock(DeepseekClient.class);
        service = new RecommendationService(recipientMapper, recipientTagMapper, historyMapper,
                productMapper, commerceService, knowledgeGraphService, keywordGraphService, deepseekClient);
        loginAs(1L);
    }

    @AfterEach
    void tearDown() {
        service.shutdownSearchExecutor();
        try {
            StpUtil.logout();
        } catch (Exception ignored) {
        }
        SaTokenContextForThreadLocalStorage.clearBox();
    }

    /** 建立最小可用的 Sa-Token ThreadLocal 上下文并登录指定用户。 */
    private void loginAs(Long userId) {
        Map<String, Object> store = new HashMap<>();
        SaStorage storage = new SaStorage() {
            @Override public Object getSource() { return store; }
            @Override public Object get(String key) { return store.get(key); }
            @Override public SaStorage set(String key, Object value) { store.put(key, value); return this; }
            @Override public SaStorage delete(String key) { store.remove(key); return this; }
        };
        SaRequest request = mock(SaRequest.class);
        SaResponse response = mock(SaResponse.class);
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(request, response, storage);
        StpUtil.login(userId);
    }

    private Recipient ownRecipient() {
        Recipient r = new Recipient();
        r.setId(1L);
        r.setUserId(1L);
        r.setName("小美");
        r.setRelation("恋人");
        r.setGender(2);
        r.setAgeRange("18-25");
        r.setMbti("ENFP");
        r.setPersonality("开朗外向");
        return r;
    }

    private RecipientTag tag(String name) {
        RecipientTag t = new RecipientTag();
        t.setRecipientId(1L);
        t.setTagCode(name);
        t.setTagName(name);
        return t;
    }

    @Test
    void realGraphDrivesAiPromptAndFinalReasonsWithoutTrustingClientWeight() throws IOException {
        KeywordGraphService graph = new KeywordGraphService();
        org.springframework.test.util.ReflectionTestUtils.setField(graph, "filePath", "classpath:kg_keywords.json");
        graph.init();
        service.shutdownSearchExecutor();
        service = new RecommendationService(recipientMapper, recipientTagMapper, historyMapper,
                productMapper, commerceService, knowledgeGraphService, graph, deepseekClient);
        Recipient recipient = ownRecipient();
        recipient.setRelation("朋友");
        when(recipientMapper.selectById(1L)).thenReturn(recipient);
        when(recipientTagMapper.selectList(any())).thenReturn(List.of(tag("文艺")));
        when(deepseekClient.chat(anyString(), anyString(), anyInt())).thenReturn(
                "{\"gifts\":[{\"name\":\"金属书签\",\"price\":99,\"reason\":\"测试\"}],\"summary\":\"测试\"}");
        RecommendRequest request = new RecommendRequest();
        request.setRecipientId(1L); request.setOccasion("birthday"); request.setBudget(new BigDecimal("300"));
        AiGiftsResponse ai = service.generateAiGifts(request);
        assertTrue(ai.getAiGenerated());
        assertTrue(ai.getGifts().get(0).getReason().contains("文艺"));
        org.mockito.ArgumentCaptor<String> prompt = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(deepseekClient).chat(anyString(), prompt.capture(), anyInt());
        assertTrue(prompt.getValue().contains("加权关键词图谱证据"));
        Product product = new Product();
        product.setId(99L); product.setName("金属书签礼盒"); product.setPrice(new BigDecimal("99"));
        when(commerceService.searchAcrossPlatforms(anyString(), anyInt(), anyInt())).thenReturn(List.of(product));
        AiGift gift = ai.getGifts().get(0); gift.setWeight(999999);
        MatchRequest match = new MatchRequest();
        match.setRecipientId(1L); match.setOccasion("birthday"); match.setBudget(request.getBudget()); match.setGifts(List.of(gift));
        RecommendResponse result = service.matchAndSearch(match);
        RecommendItem item = result.getItems().get(0);
        assertEquals("金属书签礼盒", item.getProductName());
        assertTrue(result.getKgEnhanced());
        assertTrue(item.getReason().contains("文艺"));
        assertTrue(item.getReason().contains("朋友"));
        assertTrue(item.getReason().contains("生日"));
        assertTrue(item.getKeywordWeight() <= 300);
        assertTrue(item.getReasoningChain().contains("独立原文支持"));
    }

    @Test
    void generateAiGiftsShouldFallbackToKeywordGraphWhenDeepseekFails() throws IOException {
        when(recipientMapper.selectById(1L)).thenReturn(ownRecipient());
        when(recipientTagMapper.selectList(any())).thenReturn(List.of(tag("文艺")));
        when(knowledgeGraphService.getTagCategoryNames(anyList())).thenReturn(Collections.emptyMap());
        when(deepseekClient.chat(anyString(), anyString(), anyInt())).thenThrow(new IOException("timeout"));
        when(keywordGraphService.pickKeywords(any(), anyList(), anyString()))
                .thenReturn(List.of(new KeywordGraphService.KeywordHit("吉他 礼物", 3)));

        RecommendRequest req = new RecommendRequest();
        req.setRecipientId(1L);
        req.setOccasion("birthday");
        req.setBudget(new BigDecimal("300"));

        AiGiftsResponse resp = service.generateAiGifts(req);
        assertNotNull(resp);
        assertFalse(resp.getGifts().isEmpty());
        assertTrue(resp.getSummary().contains("降级"));
    }

    @Test
    void fallbackCandidatesShouldFitMatchRequestLimitAndKeepRanking() throws IOException {
        when(recipientMapper.selectById(1L)).thenReturn(ownRecipient());
        when(recipientTagMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(knowledgeGraphService.getTagCategoryNames(anyList())).thenReturn(Collections.emptyMap());
        when(deepseekClient.chat(anyString(), anyString(), anyInt())).thenThrow(new IOException("timeout"));
        when(keywordGraphService.pickKeywords(any(), anyList(), anyString())).thenReturn(
                java.util.stream.IntStream.range(0, 12)
                        .mapToObj(i -> new KeywordGraphService.KeywordHit("礼物" + i, 12 - i))
                        .collect(java.util.stream.Collectors.toList()));
        RecommendRequest req = new RecommendRequest();
        req.setRecipientId(1L);
        req.setOccasion("birthday");
        req.setBudget(new BigDecimal("300"));
        AiGiftsResponse resp = service.generateAiGifts(req);
        assertEquals(8, resp.getGifts().size());
        assertEquals("礼物0", resp.getGifts().get(0).getName());
        assertEquals("礼物7", resp.getGifts().get(7).getName());
        MatchRequest match = new MatchRequest();
        match.setRecipientId(1L);
        match.setOccasion("birthday");
        match.setBudget(req.getBudget());
        match.setGifts(resp.getGifts());
        try (javax.validation.ValidatorFactory factory = javax.validation.Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(match).isEmpty());
        }
    }

    @Test
    void generateAiGiftsShouldParseMarkdownWrappedLlmJson() throws IOException {
        when(recipientMapper.selectById(1L)).thenReturn(ownRecipient());
        when(recipientTagMapper.selectList(any())).thenReturn(List.of(tag("文艺")));
        when(knowledgeGraphService.getTagCategoryNames(anyList()))
                .thenReturn(Map.of("文艺", List.of("书籍/文具", "文创/艺术")));
        when(deepseekClient.chat(anyString(), anyString(), anyInt())).thenReturn(
                "```json\n" +
                "{\"gifts\":[{\"name\":\"手账本套装\",\"price\":129,\"reason\":\"送TA记录美好\",\"tags\":[\"文艺\"],\"platform\":\"拼多多\"}],"
                + "\"summary\":\"文艺风首选\"}\n```");

        RecommendRequest req = new RecommendRequest();
        req.setRecipientId(1L);
        req.setOccasion("birthday");
        req.setBudget(new BigDecimal("300"));

        AiGiftsResponse resp = service.generateAiGifts(req);
        assertNotNull(resp);
        assertEquals(1, resp.getGifts().size());
        assertEquals("手账本套装", resp.getGifts().get(0).getName());
        assertEquals("文艺风首选", resp.getSummary());
    }

    @Test
    void matchAndSearchShouldMergeKgResultsWithDedup() {
        when(recipientMapper.selectById(1L)).thenReturn(ownRecipient());
        when(recipientTagMapper.selectList(any())).thenReturn(Collections.emptyList());

        Product product = new Product();
        product.setId(10L);
        product.setName("蓝牙耳机");
        product.setPrice(new BigDecimal("199.00"));
        product.setPlatform("拼多多");
        product.setPlatformUrl("http://example.com/goods?id=10");
        when(commerceService.searchAcrossPlatforms(anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(product));

        RecommendItem kgSame = new RecommendItem();
        kgSame.setProductId(10L);
        kgSame.setProductName("蓝牙耳机");
        kgSame.setPrice(new BigDecimal("199.00"));
        kgSame.setScore(0.9);
        RecommendItem kgExtra = new RecommendItem();
        kgExtra.setProductId(20L);
        kgExtra.setProductName("文创礼盒");
        kgExtra.setPrice(new BigDecimal("150.00"));
        kgExtra.setScore(0.85);
        when(knowledgeGraphService.queryRecommendations(eq(1L), anyString(), any()))
                .thenReturn(List.of(kgSame, kgExtra));

        AiGift gift = new AiGift();
        gift.setName("蓝牙耳机");
        gift.setPrice(199);
        gift.setPlatform("拼多多");
        MatchRequest req = new MatchRequest();
        req.setRecipientId(1L);
        req.setOccasion("birthday");
        req.setBudget(new BigDecimal("300"));
        req.setGifts(List.of(gift));

        RecommendResponse resp = service.matchAndSearch(req);
        assertNotNull(resp);
        List<RecommendItem> items = resp.getItems();
        long distinctIds = items.stream()
                .map(RecommendItem::getProductId)
                .filter(id -> id != null && id > 0)
                .distinct().count();
        assertEquals(items.size(), distinctIds, "推荐结果不应包含重复 productId");
        assertTrue(items.stream().anyMatch(i -> Long.valueOf(20L).equals(i.getProductId())),
                "KG 通道结果应被合并进推荐列表");
    }
}
