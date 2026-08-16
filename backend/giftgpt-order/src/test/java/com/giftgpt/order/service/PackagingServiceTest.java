package com.giftgpt.order.service;

import com.giftgpt.common.ai.DeepseekClient;
import com.giftgpt.order.dto.packaging.AiPackagingRequest;
import com.giftgpt.order.dto.packaging.AiPackagingResult;
import com.giftgpt.order.mapper.PackagingMapper;
import com.giftgpt.user.mapper.GiftRecordMapper;
import com.giftgpt.user.mapper.RecipientMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PackagingServiceTest {

    @Test
    void aiRecommendShouldFallbackWhenDeepseekFails() throws IOException {
        DeepseekClient deepseekClient = mock(DeepseekClient.class);
        when(deepseekClient.isConfigured()).thenReturn(true);
        when(deepseekClient.chat(anyString(), anyString(), anyInt())).thenThrow(new IOException("timeout"));

        PackagingService service = new PackagingService(
                mock(PackagingMapper.class),
                mock(GiftRecordMapper.class),
                mock(RecipientMapper.class),
                deepseekClient);

        AiPackagingRequest request = new AiPackagingRequest();
        request.setProductName("测试礼物");

        AiPackagingResult result = service.aiRecommend(request);

        assertNotNull(result);
        assertEquals("classic", result.getPackagingType());
    }
}
