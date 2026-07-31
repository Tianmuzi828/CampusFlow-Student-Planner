package com.campusflow.app.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Assignment {
    private final int id;
    private final int courseId;
    private final String title;
    private final String description;
    private final LocalDate dueDate;
    private final LocalTime dueTime;
    private final Priority priority;
    private final AssignmentStatus status;

    public Assignment(int id, int courseId, String title, String description,
                      LocalDate dueDate, LocalTime dueTime, Priority priority,
                      AssignmentStatus status) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.priority = priority;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public Priority getPriority() {
        return priority;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public boolean isCompleted() {
        return status == AssignmentStatus.COMPLETED;
    }
}
