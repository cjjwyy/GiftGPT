package com.giftgpt.content.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.content.entity.CalendarEvent;
import com.giftgpt.content.entity.Notification;
import com.giftgpt.content.mapper.CalendarEventMapper;
import com.giftgpt.content.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final CalendarEventMapper calendarEventMapper;
    private final NotificationMapper notificationMapper;

    @Scheduled(cron = "${giftgpt.notification.scan-cron:0 0 8 * * ?}", zone = "Asia/Shanghai")
    public void scanAndNotify() {
        int created = scan(null);
        if (created > 0) {
            log.info("calendar_reminder_scan created={}", created);
        }
    }

    public int scanForCurrentUser() {
        return scan(StpUtil.getLoginIdAsLong());
    }

    public Page<Notification> list(int page, int size, boolean unreadOnly) {
        Long userId = StpUtil.getLoginIdAsLong();
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return notificationMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(unreadOnly, Notification::getIsRead, 0)
                        .orderByDesc(Notification::getCreateTime));
    }

    public long unreadCount() {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, StpUtil.getLoginIdAsLong())
                .eq(Notification::getIsRead, 0));
    }

    public void markRead(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!userId.equals(notification.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
    }

    public void markAllRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }

    private int scan(Long userId) {
        List<CalendarEvent> events = calendarEventMapper.selectList(
                new LambdaQueryWrapper<CalendarEvent>()
                        .eq(userId != null, CalendarEvent::getUserId, userId));
        LocalDate today = CalendarDates.today();
        int created = 0;
        for (CalendarEvent event : events) {
            if (event.getEventDate() == null || event.getUserId() == null) {
                continue;
            }
            LocalDate occurrence = CalendarDates.next(event, today);
            if (occurrence == null || occurrence.isBefore(today)) {
                continue;
            }
            int remindDays = event.getRemindBeforeDays() == null
                    ? 3 : Math.max(0, Math.min(event.getRemindBeforeDays(), 365));
            if (occurrence.minusDays(remindDays).isAfter(today)) {
                continue;
            }
            Notification notification = new Notification();
            notification.setUserId(event.getUserId());
            notification.setCalendarEventId(event.getId());
            notification.setOccurrenceDate(occurrence);
            notification.setTitle("礼物提醒：" + event.getTitle());
            long days = ChronoUnit.DAYS.between(today, occurrence);
            notification.setContent(days == 0
                    ? "今天就是这个重要日子，别忘了准备心意。"
                    : "距离这个重要日子还有" + days + "天，可以开始准备礼物了。");
            notification.setIsRead(0);
            try {
                notificationMapper.insert(notification);
                created++;
            } catch (DataIntegrityViolationException ignored) {
                // This occurrence has already produced a reminder.
            }
        }
        return created;
    }

}
