package com.giftgpt.content.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.content.dto.CalendarEventRequest;
import com.giftgpt.content.dto.StoryCreateRequest;
import com.giftgpt.content.entity.CalendarEvent;
import com.giftgpt.content.entity.Story;
import com.giftgpt.content.entity.StoryLike;
import com.giftgpt.content.entity.StoryReply;
import com.giftgpt.content.mapper.CalendarEventMapper;
import com.giftgpt.content.mapper.StoryLikeMapper;
import com.giftgpt.content.mapper.StoryMapper;
import com.giftgpt.content.mapper.StoryReplyMapper;
import com.giftgpt.user.entity.GiftRecord;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.mapper.GiftRecordMapper;
import com.giftgpt.user.mapper.RecipientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final StoryMapper storyMapper;
    private final CalendarEventMapper calendarEventMapper;
    private final StoryLikeMapper storyLikeMapper;
    private final StoryReplyMapper storyReplyMapper;
    private final GiftRecordMapper giftRecordMapper;
    private final RecipientMapper recipientMapper;

    public Page<Story> listStories(int page, int size) {
        Page<Story> p = new Page<>(Math.max(1, page), Math.max(1, Math.min(size, 100)));
        Long currentUserId = null;
        try {
            if (StpUtil.isLogin()) {
                currentUserId = StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignored) {
        }
        return storyMapper.selectPageWithUser(p, currentUserId);
    }

    public Story createStory(StoryCreateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (request.getGiftRecordId() != null) {
            GiftRecord giftRecord = giftRecordMapper.selectById(request.getGiftRecordId());
            if (giftRecord == null) {
                throw new BusinessException(ResultCode.GIFT_RECORD_NOT_FOUND);
            }
            if (!userId.equals(giftRecord.getUserId())) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
        }
        Story story = new Story();
        story.setUserId(userId);
        story.setGiftRecordId(request.getGiftRecordId());
        story.setTitle(request.getTitle().trim());
        story.setContent(request.getContent().trim());
        story.setImages(request.getImages());
        story.setIsAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : 0);
        story.setLikes(0);
        story.setStatus(1);
        storyMapper.insert(story);
        return story;
    }

    @org.springframework.transaction.annotation.Transactional
    public Story likeStory(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Story story = storyMapper.lockById(id);
        if (story == null || !Integer.valueOf(1).equals(story.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Long count = storyLikeMapper.selectCount(
                new LambdaQueryWrapper<StoryLike>()
                        .eq(StoryLike::getStoryId, id)
                        .eq(StoryLike::getUserId, userId));
        if (count > 0) {
            story.setLiked(1);
            return story;
        }

        StoryLike like = new StoryLike();
        like.setStoryId(id);
        like.setUserId(userId);
        storyLikeMapper.insert(like);

        int likeCount = storyLikeMapper.selectCount(
                new LambdaQueryWrapper<StoryLike>().eq(StoryLike::getStoryId, id)).intValue();
        story.setLikes(likeCount);
        storyMapper.updateById(story);
        story.setLiked(1);
        return story;
    }

    @org.springframework.transaction.annotation.Transactional
    public Story unlikeStory(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Story story = storyMapper.lockById(id);
        if (story == null || !Integer.valueOf(1).equals(story.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        storyLikeMapper.delete(
                new LambdaQueryWrapper<StoryLike>()
                        .eq(StoryLike::getStoryId, id)
                        .eq(StoryLike::getUserId, userId));

        int likeCount = storyLikeMapper.selectCount(
                new LambdaQueryWrapper<StoryLike>().eq(StoryLike::getStoryId, id)).intValue();
        story.setLikes(likeCount);
        storyMapper.updateById(story);
        story.setLiked(0);
        return story;
    }

    public Page<StoryReply> getReplies(Long storyId, int page, int size) {
        requireVisibleStory(storyId);
        return storyReplyMapper.selectReplyPage(new Page<>(Math.max(1, page), Math.max(1, Math.min(size, 100))), storyId);
    }

    public StoryReply addReply(Long storyId, String content) {
        Long userId = StpUtil.getLoginIdAsLong();
        requireVisibleStory(storyId);
        StoryReply reply = new StoryReply();
        reply.setStoryId(storyId);
        reply.setUserId(userId);
        reply.setContent(content.trim());
        storyReplyMapper.insert(reply);
        return reply;
    }

    public Page<CalendarEvent> listCalendarEvents(int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<CalendarEvent> p = new Page<>(Math.max(1, page), Math.max(1, Math.min(size, 100)));
        Page<CalendarEvent> result = calendarEventMapper.selectPage(p,
                new LambdaQueryWrapper<CalendarEvent>()
                        .eq(CalendarEvent::getUserId, userId)
                        .orderByAsc(CalendarEvent::getEventDate));
        LocalDate today = CalendarDates.today();
        result.getRecords().forEach(event -> {
            event.setNextOccurrence(CalendarDates.next(event, today));
            if (event.getNextOccurrence() != null) event.setDaysUntil(java.time.temporal.ChronoUnit.DAYS.between(today, event.getNextOccurrence()));
        });
        return result;
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteStory(Long id) {
        Story story = storyMapper.lockById(id);
        if (story == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!Long.valueOf(StpUtil.getLoginIdAsLong()).equals(story.getUserId())) throw new BusinessException(ResultCode.FORBIDDEN);
        story.setStatus(0);
        storyMapper.updateById(story);
    }

    public CalendarEvent createCalendarEvent(CalendarEventRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        validateRecipientOwnership(request.getRecipientId(), userId);
        CalendarEvent event = new CalendarEvent();
        event.setUserId(userId);
        applyCalendarRequest(event, request);
        calendarEventMapper.insert(event);
        return event;
    }

    public CalendarEvent updateCalendarEvent(Long id, CalendarEventRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        CalendarEvent exist = calendarEventMapper.selectById(id);
        if (exist == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!exist.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        validateRecipientOwnership(request.getRecipientId(), userId);
        applyCalendarRequest(exist, request);
        calendarEventMapper.updateById(exist);
        return exist;
    }

    public void deleteCalendarEvent(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        CalendarEvent exist = calendarEventMapper.selectById(id);
        if (exist == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!exist.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        calendarEventMapper.deleteById(id);
    }

    private void requireVisibleStory(Long storyId) {
        Story story = storyMapper.selectById(storyId);
        if (story == null || !Integer.valueOf(1).equals(story.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    private void validateRecipientOwnership(Long recipientId, Long userId) {
        if (recipientId == null) {
            return;
        }
        Recipient recipient = recipientMapper.selectById(recipientId);
        if (recipient == null) {
            throw new BusinessException(ResultCode.RECIPIENT_NOT_FOUND);
        }
        if (!userId.equals(recipient.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private void applyCalendarRequest(CalendarEvent event, CalendarEventRequest request) {
        int repeat = request.getIsRepeat() == null ? 1 : request.getIsRepeat();
        if (request.getEventDate().isBefore(CalendarDates.today()) && repeat != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非重复事件日期不能早于今天");
        }
        event.setRecipientId(request.getRecipientId());
        event.setTitle(request.getTitle().trim());
        event.setOccasion(request.getOccasion() == null ? null : request.getOccasion().trim());
        event.setEventDate(request.getEventDate());
        event.setRemindBeforeDays(request.getRemindBeforeDays() == null ? 3 : request.getRemindBeforeDays());
        event.setIsRepeat(repeat);
    }
}
