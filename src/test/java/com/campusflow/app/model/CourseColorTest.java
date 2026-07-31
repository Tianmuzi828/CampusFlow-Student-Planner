package com.campusflow.app.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseColorTest {
    @Test
    void providesEightDistinctCourseColors() {
        assertEquals(8, CourseColor.values().length);
        assertEquals(8, Arrays.stream(CourseColor.values())
                .map(CourseColor::storageValue)
                .distinct()
                .count());
        assertEquals(8, Arrays.stream(CourseColor.values())
                .map(CourseColor::calendarStyleClass)
                .distinct()
                .count());
    }

    @Test
    void safelyFallsBackToBlueForUnknownStoredValues() {
        assertEquals(CourseColor.PINK, CourseColor.fromStorage("pink"));
        assertEquals(CourseColor.BLUE, CourseColor.fromStorage("unknown"));
    }
}
