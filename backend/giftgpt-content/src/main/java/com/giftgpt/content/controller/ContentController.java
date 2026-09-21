package com.giftgpt.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.result.Result;
import com.giftgpt.content.dto.CalendarEventRequest;
import com.giftgpt.content.dto.ReplyRequest;
import com.giftgpt.content.dto.StoryCreateRequest;
import com.giftgpt.content.entity.CalendarEvent;
import com.giftgpt.content.entity.Story;
import com.giftgpt.content.dto.StoryView;
import cn.dev33.satoken.stp.StpUtil;
import com.giftgpt.content.entity.StoryReply;
import com.giftgpt.content.service.ContentService;
import com.giftgpt.content.entity.Notification;
import com.giftgpt.content.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "内容社区", description = "礼物故事、日历提醒")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final NotificationService notificationService;

    @Operation(summary = "社区故事列表")
    @GetMapping("/stories")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<StoryView>> listStories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long viewer = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        return Result.ok(contentService.listStories(page, size).convert(s -> StoryView.from(s, viewer)));
    }

    @Operation(summary = "发布故事")
    @PostMapping("/stories")
    public Result<StoryView> createStory(@Valid @RequestBody StoryCreateRequest request) {
        return Result.ok(StoryView.from(contentService.createStory(request), StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "点赞故事")
    @PostMapping("/stories/{id}/like")
    public Result<StoryView> likeStory(@PathVariable Long id) {
        return Result.ok(StoryView.from(contentService.likeStory(id), StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "取消点赞")
    @PostMapping("/stories/{id}/unlike")
    public Result<StoryView> unlikeStory(@PathVariable Long id) {
        return Result.ok(StoryView.from(contentService.unlikeStory(id), StpUtil.getLoginIdAsLong()));
    }

    @DeleteMapping("/stories/{id}")
    public Result<Void> deleteStory(@PathVariable Long id) {
        contentService.deleteStory(id);
        return Result.ok();
    }

    @Operation(summary = "获取故事回复")
    @GetMapping("/stories/{storyId}/replies")
    public Result<Page<StoryReply>> getReplies(@PathVariable Long storyId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(contentService.getReplies(storyId, page, size));
    }

    @Operation(summary = "回复故事")
    @PostMapping("/stories/{storyId}/replies")
    public Result<StoryReply> addReply(
            @PathVariable Long storyId, @Valid @RequestBody ReplyRequest request) {
        return Result.ok(contentService.addReply(storyId, request.getContent()));
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
    public Result<CalendarEvent> createCalendarEvent(@Valid @RequestBody CalendarEventRequest request) {
        return Result.ok(contentService.createCalendarEvent(request));
    }

    @Operation(summary = "编辑日历提醒")
    @PutMapping("/calendar/{id}")
    public Result<CalendarEvent> updateCalendarEvent(
            @PathVariable Long id, @Valid @RequestBody CalendarEventRequest request) {
        return Result.ok(contentService.updateCalendarEvent(id, request));
    }

    @Operation(summary = "删除日历提醒")
    @DeleteMapping("/calendar/{id}")
    public Result<Void> deleteCalendarEvent(@PathVariable Long id) {
        contentService.deleteCalendarEvent(id);
        return Result.ok();
    }

    @Operation(summary = "提醒通知列表")
    @GetMapping("/notifications")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Notification>> notifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return Result.ok(notificationService.list(page, size, unreadOnly));
    }

    @Operation(summary = "未读提醒数")
    @GetMapping("/notifications/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount());
    }

    @Operation(summary = "标记提醒已读")
    @PostMapping("/notifications/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.ok();
    }

    @Operation(summary = "全部提醒已读")
    @PostMapping("/notifications/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.ok();
    }

    @Operation(summary = "立即检查当前用户提醒")
    @PostMapping("/notifications/check-now")
    public Result<Integer> checkNotificationsNow() {
        return Result.ok(notificationService.scanForCurrentUser());
    }
}
