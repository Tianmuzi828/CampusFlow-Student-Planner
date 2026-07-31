package com.campusflow.app.service;

import java.time.Duration;
import java.time.LocalTime;

public final class ScheduleTimeScale {
    public static final LocalTime START_TIME = LocalTime.of(7, 0);
    public static final LocalTime END_TIME = LocalTime.of(22, 0);
    public static final int SLOT_MINUTES = 10;
    public static final double SLOT_HEIGHT = 12.0;

    private ScheduleTimeScale() {
    }

    public static int totalSlots() {
        return totalSlots(START_TIME, END_TIME, SLOT_MINUTES);
    }

    public static int totalSlots(LocalTime visibleStart, LocalTime visibleEnd,
                                 int slotMinutes) {
        return (int) (Duration.between(visibleStart, visibleEnd).toMinutes()
                / slotMinutes);
    }

    public static boolean isVisible(LocalTime startTime, LocalTime endTime) {
        return isVisible(startTime, endTime, START_TIME, END_TIME);
    }

    public static boolean isVisible(LocalTime startTime, LocalTime endTime,
                                    LocalTime visibleStart, LocalTime visibleEnd) {
        return endTime.isAfter(visibleStart) && startTime.isBefore(visibleEnd);
    }

    public static int slotFor(LocalTime time) {
        return slotFor(time, START_TIME, END_TIME, SLOT_MINUTES);
    }

    public static int slotFor(LocalTime time, LocalTime visibleStart,
                              LocalTime visibleEnd, int slotMinutes) {
        long minutes = Duration.between(visibleStart, time).toMinutes();
        int slot = (int) Math.floorDiv(minutes, slotMinutes);
        return clamp(slot, 0, totalSlots(visibleStart, visibleEnd, slotMinutes));
    }

    public static int endSlotFor(LocalTime time) {
        return endSlotFor(time, START_TIME, END_TIME, SLOT_MINUTES);
    }

    public static int endSlotFor(LocalTime time, LocalTime visibleStart,
                                 LocalTime visibleEnd, int slotMinutes) {
        long minutes = Duration.between(visibleStart, time).toMinutes();
        int slot = (int) Math.ceil(minutes / (double) slotMinutes);
        return clamp(slot, 0, totalSlots(visibleStart, visibleEnd, slotMinutes));
    }

    public static int visibleSpan(LocalTime startTime, LocalTime endTime) {
        return visibleSpan(
                startTime, endTime, START_TIME, END_TIME, SLOT_MINUTES
        );
    }

    public static int visibleSpan(LocalTime startTime, LocalTime endTime,
                                  LocalTime visibleStart, LocalTime visibleEnd,
                                  int slotMinutes) {
        return Math.max(1,
                endSlotFor(endTime, visibleStart, visibleEnd, slotMinutes)
                        - slotFor(startTime, visibleStart, visibleEnd, slotMinutes));
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
