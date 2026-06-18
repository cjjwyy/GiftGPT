package com.giftgpt.content.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.content.dto.StoryCreateRequest;
import com.giftgpt.content.entity.CalendarEvent;
import com.giftgpt.content.entity.Story;
import com.giftgpt.content.mapper.CalendarEventMapper;
import com.giftgpt.content.mapper.StoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final StoryMapper storyMapper;
    private final CalendarEventMapper calendarEventMapper;

    public Page<Story> listStories(int page, int size) {
        Page<Story> p = new Page<>(page, size);
        return storyMapper.selectPage(p,
                new LambdaQueryWrapper<Story>()
                        .eq(Story::getStatus, 1)
                        .orderByDesc(Story::getCreateTime));
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
        Story story = storyMapper.selectById(id);
        if (story != null) {
            story.setLikes(story.getLikes() != null ? story.getLikes() + 1 : 1);
            storyMapper.updateById(story);
        }
        return story;
    }

    public Page<CalendarEvent> listCalendarEvents(int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<CalendarEvent> p = new Page<>(page, size);
        return calendarEventMapper.selectPage(p,
                new LambdaQueryWrapper<CalendarEvent>()
                        .eq(CalendarEvent::getUserId, userId)
                        .ge(CalendarEvent::getEventDate, LocalDate.now())
                        .orderByAsc(CalendarEvent::getEventDate));
    }

    public CalendarEvent createCalendarEvent(CalendarEvent event) {
        Long userId = StpUtil.getLoginIdAsLong();
        event.setUserId(userId);
        calendarEventMapper.insert(event);
        return event;
    }
}
