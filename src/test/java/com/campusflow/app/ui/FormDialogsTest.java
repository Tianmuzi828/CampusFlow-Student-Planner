package com.campusflow.app.ui;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormDialogsTest {
    @Test
    void createsOneFullDayOfFiveMinuteDeadlineChoices() {
        List<LocalTime> choices = FormDialogs.deadlineTimes();

        assertEquals(288, choices.size());
        assertEquals(LocalTime.MIDNIGHT, choices.getFirst());
        assertEquals(LocalTime.of(23, 55), choices.getLast());
        assertEquals(288, choices.stream().distinct().count());
    }

    @Test
    void adaptsDeadlineChoicesToTheSavedPrecision() {
        List<LocalTime> choices = FormDialogs.deadlineTimes(15);

        assertEquals(96, choices.size());
        assertEquals(LocalTime.MIDNIGHT, choices.getFirst());
        assertEquals(LocalTime.of(23, 45), choices.getLast());
    }
}
