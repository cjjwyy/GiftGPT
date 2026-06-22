package com.giftgpt.content.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.content.dto.StoryCreateRequest;
import com.giftgpt.content.entity.CalendarEvent;
import com.giftgpt.content.entity.Story;
import com.giftgpt.content.entity.StoryLike;
import com.giftgpt.content.entity.StoryReply;
import com.giftgpt.content.mapper.CalendarEventMapper;
import com.giftgpt.content.mapper.StoryLikeMapper;
import com.giftgpt.content.mapper.StoryMapper;
import com.giftgpt.content.mapper.StoryReplyMapper;
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

    public Page<Story> listStories(int page, int size) {
        Page<Story> p = new Page<>(page, size);
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
        Story story = new Story();
        story.setUserId(userId);
        story.setGiftRecordId(request.getGiftRecordId());
        story.setTitle(request.getTitle());
        story.setContent(request.getContent());
        story.setImages(request.getImages());
        story.setIsAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : 0);
        story.setLikes(0);
        story.setStatus(1);
        storyMapper.insert(story);
        return story;
    }

    public Story likeStory(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Story story = storyMapper.selectById(id);
        if (story == null) return null;

        Long count = storyLikeMapper.selectCount(
                new LambdaQueryWrapper<StoryLike>()
                        .eq(StoryLike::getStoryId, id)
                        .eq(StoryLike::getUserId, userId));
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "already liked");
        }

        StoryLike like = new StoryLike();
        like.setStoryId(id);
        like.setUserId(userId);
        storyLikeMapper.insert(like);

        int likeCount = storyLikeMapper.selectCount(
                new LambdaQueryWrapper<StoryLike>().eq(StoryLike::getStoryId, id)).intValue();
        story.setLikes(likeCount);
        storyMapper.updateById(story);
        return story;
    }

    public Story unlikeStory(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Story story = storyMapper.selectById(id);
        if (story == null) return null;

        storyLikeMapper.delete(
                new LambdaQueryWrapper<StoryLike>()
                        .eq(StoryLike::getStoryId, id)
                        .eq(StoryLike::getUserId, userId));

        int likeCount = storyLikeMapper.selectCount(
                new LambdaQueryWrapper<StoryLike>().eq(StoryLike::getStoryId, id)).intValue();
        story.setLikes(likeCount);
        storyMapper.updateById(story);
        return story;
    }

    public List<StoryReply> getReplies(Long storyId) {
        return storyReplyMapper.selectRepliesWithUser(storyId);
    }

    public StoryReply addReply(Long storyId, String content) {
        Long userId = StpUtil.getLoginIdAsLong();
        StoryReply reply = new StoryReply();
        reply.setStoryId(storyId);
        reply.setUserId(userId);
        reply.setContent(content);
        storyReplyMapper.insert(reply);
        reply.setNickname(storyReplyMapper.selectRepliesWithUser(storyId).stream()
                .filter(r -> r.getId().equals(reply.getId()))
                .map(StoryReply::getNickname)
                .findFirst()
                .orElse(null));
        return reply;
    }

    public Page<CalendarEvent> listCalendarEvents(int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<CalendarEvent> p = new Page<>(page, size);
        return calendarEventMapper.selectPage(p,
                new LambdaQueryWrapper<CalendarEvent>()
                        .eq(CalendarEvent::getUserId, userId)
                        .orderByAsc(CalendarEvent::getEventDate));
    }

    public CalendarEvent createCalendarEvent(CalendarEvent event) {
        Long userId = StpUtil.getLoginIdAsLong();
        event.setUserId(userId);
        calendarEventMapper.insert(event);
        return event;
    }
}
