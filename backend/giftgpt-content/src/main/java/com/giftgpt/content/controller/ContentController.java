package com.giftgpt.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.result.Result;
import com.giftgpt.content.dto.StoryCreateRequest;
import com.giftgpt.content.entity.CalendarEvent;
import com.giftgpt.content.entity.Story;
import com.giftgpt.content.entity.StoryReply;
import com.giftgpt.content.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "内容社区", description = "礼物故事、日历提醒")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @Operation(summary = "社区故事列表")
    @GetMapping("/stories")
    public Result<Page<Story>> listStories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(contentService.listStories(page, size));
    }

    @Operation(summary = "发布故事")
    @PostMapping("/stories")
    public Result<Story> createStory(@Valid @RequestBody StoryCreateRequest request) {
        return Result.ok(contentService.createStory(request));
    }

    @Operation(summary = "点赞故事")
    @PostMapping("/stories/{id}/like")
    public Result<Story> likeStory(@PathVariable Long id) {
        return Result.ok(contentService.likeStory(id));
    }

    @Operation(summary = "取消点赞")
    @PostMapping("/stories/{id}/unlike")
    public Result<Story> unlikeStory(@PathVariable Long id) {
        return Result.ok(contentService.unlikeStory(id));
    }

    @Operation(summary = "获取故事回复")
    @GetMapping("/stories/{storyId}/replies")
    public Result<List<StoryReply>> getReplies(@PathVariable Long storyId) {
        return Result.ok(contentService.getReplies(storyId));
    }

    @Operation(summary = "回复故事")
    @PostMapping("/stories/{storyId}/replies")
    public Result<StoryReply> addReply(@PathVariable Long storyId, @RequestBody Map<String, String> body) {
        return Result.ok(contentService.addReply(storyId, body.get("content")));
    }

    @Operation(summary = "日历提醒列表")
    @GetMapping("/calendar")
    public Result<Page<CalendarEvent>> calendarEvents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(contentService.listCalendarEvents(page, size));
    }

    @Operation(summary = "创建日历提醒")
    @PostMapping("/calendar")
    public Result<CalendarEvent> createCalendarEvent(@RequestBody CalendarEvent event) {
        return Result.ok(contentService.createCalendarEvent(event));
    }
}
