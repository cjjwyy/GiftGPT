package com.giftgpt.content.service;
import com.giftgpt.content.entity.CalendarEvent;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class CalendarDatesTest {
    @Test void repeatsAcrossYearsAndHandlesLeapDay() {
        CalendarEvent event = new CalendarEvent(); event.setIsRepeat(1);
        event.setEventDate(LocalDate.of(2020,2,29));
        assertEquals(LocalDate.of(2026,2,28), CalendarDates.next(event, LocalDate.of(2026,1,1)));
        assertEquals(LocalDate.of(2027,2,28), CalendarDates.next(event, LocalDate.of(2026,3,1)));
        assertEquals(LocalDate.of(2028,2,29), CalendarDates.next(event, LocalDate.of(2028,2,1)));
        event.setEventDate(LocalDate.of(2030,1,1));
        assertEquals(LocalDate.of(2030,1,1), CalendarDates.next(event, LocalDate.of(2026,1,1)));
    }
    @Test void oneTimeEventsKeepTheirOriginalDate() {
        CalendarEvent event = new CalendarEvent(); event.setIsRepeat(0);
        event.setEventDate(LocalDate.of(2020,1,1));
        assertEquals(event.getEventDate(), CalendarDates.next(event, LocalDate.of(2026,1,1)));
    }
}
