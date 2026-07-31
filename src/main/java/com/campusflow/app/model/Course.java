package com.campusflow.app.model;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;

public class Course {
    private final int id;
    private final String code;
    private final String name;
    private final String instructor;
    private final String location;
    private final String meetingDays;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String color;

    public Course(int id, String code, String name, String instructor,
                  String location, String meetingDays, LocalTime startTime,
                  LocalTime endTime, String color) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.instructor = instructor;
        this.location = location;
        this.meetingDays = meetingDays;
        this.startTime = startTime;
        this.endTime = endTime;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getInstructor() {
        return instructor;
    }

    public String getLocation() {
        return location;
    }

    public String getMeetingDays() {
        return meetingDays;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getColor() {
        return color;
    }

    public boolean meetsOn(DayOfWeek day) {
        String abbreviation = day.name().substring(0, 3);
        return Arrays.stream(meetingDays.split(","))
                .map(String::trim)
                .anyMatch(abbreviation::equals);
    }

    public long getDurationMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }

    @Override
    public String toString() {
        return code + " · " + name;
    }
}
