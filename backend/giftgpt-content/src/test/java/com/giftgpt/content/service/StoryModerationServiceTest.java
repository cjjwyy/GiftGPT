package com.giftgpt.content.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.*;
import cn.dev33.satoken.context.model.*;
import cn.dev33.satoken.stp.StpUtil;
import com.giftgpt.content.mapper.*;
import com.giftgpt.content.entity.*;
import com.giftgpt.content.dto.*;
import com.giftgpt.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class StoryModerationServiceTest {
    StoryMapper stories; StoryReportMapper reports; StoryModerationService service;
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
        stories=mock(StoryMapper.class);reports=mock(StoryReportMapper.class);
        service=new StoryModerationService(stories,reports);
    }
    @AfterEach void cleanup() { StpUtil.logout(); SaTokenContextForThreadLocalStorage.clearBox(); }
    StoryReviewRequest reviewRequest() {
        StoryReviewRequest r=new StoryReviewRequest();r.setAction("hide");r.setNote("Verified privacy issue");return r;
    }
    @Test void ordinaryUsersCannotReadOrChangeModerationQueue() {
        assertFalse(service.canModerate());
        assertThrows(BusinessException.class,()->service.reports(1,20,"pending"));
        assertThrows(BusinessException.class,()->service.review(4L,reviewRequest()));
        assertThrows(BusinessException.class,()->service.reportStory(4L));
        verifyNoInteractions(reports,stories);
    }
    @Test void reportsAreIdempotentAndDoNotHidePostsAutomatically() {
        Story story=new Story();story.setId(2L);story.setStatus(1);
        when(stories.lockById(2L)).thenReturn(story);
        StoryReportRequest r=new StoryReportRequest();r.setReason("spam");r.setDetail("ad");
        doAnswer(i->{((StoryReport)i.getArgument(0)).setId(8L);return 1;}).when(reports).insert(any());
        assertEquals("pending",service.report(2L,r).get("status"));
        StoryReport existing=new StoryReport();existing.setId(8L);existing.setStatus("pending");
        when(reports.selectOne(any())).thenReturn(existing);
        assertEquals(8L,service.report(2L,r).get("id"));
        verify(reports,times(1)).insert(any());verify(stories,never()).updateById(any());
    }
    @Test void authorizedReviewRecordsDecisionAndRejectsSecondReview() {
        ReflectionTestUtils.setField(service,"moderatorIds","1,2");
        assertTrue(service.canModerate());
        StoryReport report=new StoryReport();report.setId(4L);report.setStoryId(2L);report.setStatus("pending");
        when(reports.lockById(4L)).thenReturn(report);
        Story story=new Story();story.setId(2L);story.setStatus(1);
        when(stories.lockById(2L)).thenReturn(story);
        service.review(4L,reviewRequest());
        assertEquals(0,story.getStatus());assertEquals("hide",report.getStatus());
        assertEquals(1L,report.getModeratorId());assertEquals("Verified privacy issue",report.getDecisionNote());
        verify(reports).updateById(report);
        assertThrows(BusinessException.class,()->service.review(4L,reviewRequest()));
    }
    @Test void dismissalKeepsStoryAndAnonymousContentStaysAnonymous() {
        ReflectionTestUtils.setField(service,"moderatorIds","1");
        StoryReport report=new StoryReport();report.setId(4L);report.setStoryId(2L);report.setStatus("pending");
        when(reports.lockById(4L)).thenReturn(report);when(reports.selectById(4L)).thenReturn(report);
        Story story=new Story();story.setId(2L);story.setStatus(1);story.setIsAnonymous(1);story.setUserId(9L);story.setNickname("private");
        when(stories.selectById(2L)).thenReturn(story);
        assertNull(service.reportStory(4L).getUserId());assertNull(service.reportStory(4L).getNickname());
        StoryReviewRequest r=reviewRequest();r.setAction("dismiss");service.review(4L,r);
        verify(stories,never()).updateById(any());assertEquals("dismiss",report.getStatus());
    }
}
