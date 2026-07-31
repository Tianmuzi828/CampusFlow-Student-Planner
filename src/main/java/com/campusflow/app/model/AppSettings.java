package com.campusflow.app.model;

import com.campusflow.app.i18n.AppLanguage;
import com.campusflow.app.i18n.I18n;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AppSettings {
    public enum Theme {
        LIGHT("settings.theme.light"),
        DARK("settings.theme.dark"),
        SYSTEM("settings.theme.system");

        private final String textKey;

        Theme(String textKey) {
            this.textKey = textKey;
        }

        @Override
        public String toString() {
            return I18n.text(textKey);
        }
    }

    public enum AccentColor {
        INDIGO("settings.accent.indigo"),
        PURPLE("settings.accent.purple"),
        GREEN("settings.accent.green"),
        ORANGE("settings.accent.orange");

        private final String textKey;

        AccentColor(String textKey) {
            this.textKey = textKey;
        }

        @Override
        public String toString() {
            return I18n.text(textKey);
        }
    }

    public enum Density {
        COMFORTABLE("settings.density.comfortable"),
        COMPACT("settings.density.compact");

        private final String textKey;

        Density(String textKey) {
            this.textKey = textKey;
        }

        @Override
        public String toString() {
            return I18n.text(textKey);
        }
    }

    private final String displayName;
    private final String semesterName;
    private final LocalDate semesterStart;
    private final LocalDate semesterEnd;
    private final DayOfWeek weekStartsOn;
    private final boolean use24HourTime;
    private final LocalTime calendarStartTime;
    private final LocalTime calendarEndTime;
    private final int timeIntervalMinutes;
    private final LocalTime defaultDeadlineTime;
    private final Priority defaultPriority;
    private final boolean showCompletedAssignments;
    private final Theme theme;
    private final AccentColor accentColor;
    private final Density density;
    private final boolean remindOneDayBefore;
    private final boolean remindOneHourBefore;
    private final boolean remindOverdue;
    private final AppLanguage language;

    public AppSettings(
            String displayName,
            String semesterName,
            LocalDate semesterStart,
            LocalDate semesterEnd,
            DayOfWeek weekStartsOn,
            boolean use24HourTime,
            LocalTime calendarStartTime,
            LocalTime calendarEndTime,
            int timeIntervalMinutes,
            LocalTime defaultDeadlineTime,
            Priority defaultPriority,
            boolean showCompletedAssignments,
            Theme theme,
            AccentColor accentColor,
            Density density,
            boolean remindOneDayBefore,
            boolean remindOneHourBefore,
            boolean remindOverdue,
            AppLanguage language) {
        this.displayName = displayName;
        this.semesterName = semesterName;
        this.semesterStart = semesterStart;
        this.semesterEnd = semesterEnd;
        this.weekStartsOn = weekStartsOn;
        this.use24HourTime = use24HourTime;
        this.calendarStartTime = calendarStartTime;
        this.calendarEndTime = calendarEndTime;
        this.timeIntervalMinutes = timeIntervalMinutes;
        this.defaultDeadlineTime = defaultDeadlineTime;
        this.defaultPriority = defaultPriority;
        this.showCompletedAssignments = showCompletedAssignments;
        this.theme = theme;
        this.accentColor = accentColor;
        this.density = density;
        this.remindOneDayBefore = remindOneDayBefore;
        this.remindOneHourBefore = remindOneHourBefore;
        this.remindOverdue = remindOverdue;
        this.language = language == null ? AppLanguage.ENGLISH : language;
    }

    public static AppSettings defaults() {
        LocalDate today = LocalDate.now();
        LocalDate semesterStart = today.withDayOfMonth(1);
        return new AppSettings(
                "Student",
                "My semester",
                semesterStart,
                semesterStart.plusMonths(4).minusDays(1),
                DayOfWeek.MONDAY,
                false,
                LocalTime.of(7, 0),
                LocalTime.of(22, 0),
                5,
                LocalTime.of(23, 50),
                Priority.MEDIUM,
                false,
                Theme.LIGHT,
                AccentColor.INDIGO,
                Density.COMFORTABLE,
                false,
                false,
                false,
                AppLanguage.ENGLISH
        );
    }

    public String formatTime(LocalTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                use24HourTime
                        ? "HH:mm"
                        : (language == AppLanguage.SIMPLIFIED_CHINESE
                        ? "a h:mm" : "h:mm a"),
                language.locale()
        );
        return time.format(formatter);
    }

    public String getDisplayName() { return displayName; }
    public String getSemesterName() { return semesterName; }
    public LocalDate getSemesterStart() { return semesterStart; }
    public LocalDate getSemesterEnd() { return semesterEnd; }
    public DayOfWeek getWeekStartsOn() { return weekStartsOn; }
    public boolean isUse24HourTime() { return use24HourTime; }
    public LocalTime getCalendarStartTime() { return calendarStartTime; }
    public LocalTime getCalendarEndTime() { return calendarEndTime; }
    public int getTimeIntervalMinutes() { return timeIntervalMinutes; }
    public LocalTime getDefaultDeadlineTime() { return defaultDeadlineTime; }
    public Priority getDefaultPriority() { return defaultPriority; }
    public boolean isShowCompletedAssignments() { return showCompletedAssignments; }
    public Theme getTheme() { return theme; }
    public AccentColor getAccentColor() { return accentColor; }
    public Density getDensity() { return density; }
    public boolean isRemindOneDayBefore() { return remindOneDayBefore; }
    public boolean isRemindOneHourBefore() { return remindOneHourBefore; }
    public boolean isRemindOverdue() { return remindOverdue; }
    public AppLanguage getLanguage() { return language; }
}
