package com.giftgpt.content.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.*;
import cn.dev33.satoken.context.model.*;
import cn.dev33.satoken.stp.StpUtil;
import com.giftgpt.content.mapper.*;
import com.giftgpt.content.entity.*;
import com.giftgpt.user.mapper.*;
import com.giftgpt.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ContentServiceTest {
    StoryMapper stories; StoryLikeMapper likes; ContentService service; Story story;
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
        stories=mock(StoryMapper.class);likes=mock(StoryLikeMapper.class);
        service=new ContentService(stories,mock(CalendarEventMapper.class),likes,mock(StoryReplyMapper.class),
                mock(GiftRecordMapper.class),mock(RecipientMapper.class));
        story=new Story();story.setId(7L);story.setUserId(2L);story.setStatus(1);story.setLikes(3);
        when(stories.lockById(7L)).thenReturn(story);
    }
    @AfterEach void cleanup() { StpUtil.logout(); SaTokenContextForThreadLocalStorage.clearBox(); }
    @Test void repeatedLikesAreIdempotent() {
        when(likes.selectCount(any())).thenReturn(1L);
        assertEquals(1,service.likeStory(7L).getLiked());
        verify(likes,never()).insert(any()); verify(stories).lockById(7L);
    }
    @Test void firstLikeRecountsAfterWriteAndUnlikeCanRepeat() {
        when(likes.selectCount(any())).thenReturn(0L,4L,3L,3L);
        assertEquals(4,service.likeStory(7L).getLikes());
        assertEquals(3,service.unlikeStory(7L).getLikes());
        assertEquals(3,service.unlikeStory(7L).getLikes());
        verify(likes,times(1)).insert(any());
    }
    @Test void deletionRequiresOwnershipAndHidesStory() {
        assertThrows(BusinessException.class,()->service.deleteStory(7L));
        verify(stories,never()).updateById(any());
        story.setUserId(1L);service.deleteStory(7L);
        assertEquals(0,story.getStatus()); verify(stories).updateById(story);
        assertThrows(BusinessException.class,()->service.likeStory(7L));
    }
}
