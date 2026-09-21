package com.giftgpt.order.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.*;
import cn.dev33.satoken.context.model.*;
import cn.dev33.satoken.stp.StpUtil;
import com.giftgpt.order.mapper.*;
import com.giftgpt.order.entity.Packaging;
import com.giftgpt.order.dto.packaging.SavePackagingRequest;
import com.giftgpt.goods.mapper.ProductMapper;
import com.giftgpt.user.mapper.*;
import com.giftgpt.common.ai.DeepseekClient;
import com.giftgpt.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import java.util.*;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class PackagingSaveTest {
    PackagingMapper mapper;
    GiftRecordMapper gifts;
    PackagingService service;
    @BeforeEach void setup() {
        Map<String,Object> values = new HashMap<>();
        SaStorage storage = new SaStorage() {
            public Object getSource() { return values; }
            public Object get(String key) { return values.get(key); }
            public SaStorage set(String key,Object value) { values.put(key,value); return this; }
            public SaStorage delete(String key) { values.remove(key); return this; }
        };
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(mock(SaRequest.class),mock(SaResponse.class),storage);
        StpUtil.login(1L);
        mapper=mock(PackagingMapper.class); gifts=mock(GiftRecordMapper.class);
        service=new PackagingService(mapper,gifts,mock(RecipientMapper.class),mock(ProductMapper.class),
                mock(GreetingCardMapper.class),mock(DeepseekClient.class));
    }
    @AfterEach void cleanup() { StpUtil.logout(); SaTokenContextForThreadLocalStorage.clearBox(); }
    SavePackagingRequest request() {
        SavePackagingRequest req=new SavePackagingRequest();
        req.setProductName("测试礼物");req.setProductPrice(new BigDecimal("100"));
        req.setPackagingType("classic");req.setRequestKey("test-key");
        req.setCustomizations(List.of("dried_flower","band_wrap","dried_flower"));return req;
    }
    @Test void savesAllOptionsAndChargesEachOnlyOnce() {
        Packaging p=service.savePackaging(request());
        assertEquals(new BigDecimal("48.90"),p.getPrice());
        assertEquals(2,cn.hutool.json.JSONUtil.parseArray(p.getCustomizationsJson()).size());
        assertTrue(p.getPriceDetailsJson().contains("band_wrap"));
        verify(mapper).lockOwner(1L); verify(mapper).insert(p);
    }
    @Test void retryReturnsExistingPlanWithoutNewGiftOrPlan() {
        Packaging existing=new Packaging();existing.setId(7L);
        when(mapper.selectOne(any())).thenReturn(existing);
        assertSame(existing,service.savePackaging(request()));
        verify(mapper,never()).insert(any());verifyNoInteractions(gifts);
    }
    @Test void updateChecksOwnershipAndVersionAndDoesNotInsert() {
        Packaging p=new Packaging();p.setId(7L);p.setUserId(2L);p.setVersion(1);
        when(mapper.selectById(7L)).thenReturn(p);
        SavePackagingRequest req=request();req.setPlanId(7L);req.setVersion(1);
        assertThrows(BusinessException.class,()->service.savePackaging(req));
        p.setUserId(1L);req.setVersion(0);
        assertThrows(BusinessException.class,()->service.savePackaging(req));
        req.setVersion(1);
        Packaging updated=service.savePackaging(req);
        assertEquals(2,updated.getVersion());verify(mapper).updateById(updated);
        verify(mapper,never()).insert(any());
    }
}
