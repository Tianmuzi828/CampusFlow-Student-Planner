package com.campusflow.app.service;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleTimeScaleTest {
    @Test
    void positionsCoursesUsingTenMinuteSlots() {
        assertEquals(7, ScheduleTimeScale.slotFor(LocalTime.of(8, 10)));
        assertEquals(5, ScheduleTimeScale.visibleSpan(
                LocalTime.of(8, 10), LocalTime.of(9, 0)
        ));
        assertEquals(90, ScheduleTimeScale.totalSlots());
    }

    @Test
    void recognizesCoursesInsideTheVisibleCalendar() {
        assertTrue(ScheduleTimeScale.isVisible(
                LocalTime.of(8, 0), LocalTime.of(9, 0)
        ));
    }

    @Test
    void supportsCustomCalendarRangeAndFifteenMinutePrecision() {
        LocalTime visibleStart = LocalTime.of(8, 0);
        LocalTime visibleEnd = LocalTime.of(20, 0);

        assertEquals(48, ScheduleTimeScale.totalSlots(
                visibleStart, visibleEnd, 15
        ));
        assertEquals(2, ScheduleTimeScale.slotFor(
                LocalTime.of(8, 30), visibleStart, visibleEnd, 15
        ));
        assertEquals(5, ScheduleTimeScale.visibleSpan(
                LocalTime.of(9, 0), LocalTime.of(10, 15),
                visibleStart, visibleEnd, 15
        ));
    }
}
