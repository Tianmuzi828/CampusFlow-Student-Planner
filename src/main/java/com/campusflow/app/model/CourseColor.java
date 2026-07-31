package com.campusflow.app.model;

import com.campusflow.app.i18n.I18n;

import java.util.Locale;

/**
 * Supported course colors and the CSS classes used throughout the planner.
 */
public enum CourseColor {
    BLUE("courseColor.blue"),
    PURPLE("courseColor.purple"),
    GREEN("courseColor.green"),
    ORANGE("courseColor.orange"),
    RED("courseColor.red"),
    TEAL("courseColor.teal"),
    PINK("courseColor.pink"),
    GOLD("courseColor.gold");

    private final String textKey;

    CourseColor(String textKey) {
        this.textKey = textKey;
    }

    public String storageValue() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    public String accentStyleClass() {
        return "course-accent-" + storageValue();
    }

    public String calendarStyleClass() {
        return "calendar-course-" + storageValue();
    }

    public static CourseColor fromStorage(String value) {
        if (value == null || value.isBlank()) {
            return BLUE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException exception) {
            return BLUE;
        }
    }

    @Override
    public String toString() {
        return I18n.text(textKey);
    }
}
