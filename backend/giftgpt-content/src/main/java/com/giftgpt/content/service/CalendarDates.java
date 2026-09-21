package com.giftgpt.content.service;

import com.giftgpt.content.entity.CalendarEvent;
import java.time.*;

/** Calendar dates use China local dates; Feb 29 repeats on Feb 28 in common years. */
public final class CalendarDates {
    private CalendarDates() {}
    public static LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
    public static LocalDate next(CalendarEvent event, LocalDate today) {
        if (event.getEventDate() == null) return null;
        if (!Integer.valueOf(1).equals(event.getIsRepeat())) return event.getEventDate();
        LocalDate base = event.getEventDate();
        int year = Math.max(today.getYear(), base.getYear());
        LocalDate next = MonthDay.from(base).atYear(year);
        return next.isBefore(today) ? MonthDay.from(base).atYear(year + 1) : next;
    }
}
