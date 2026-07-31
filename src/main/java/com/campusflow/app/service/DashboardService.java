package com.campusflow.app.service;

import com.campusflow.app.model.Assignment;
import com.campusflow.app.model.Course;
import com.campusflow.app.model.Priority;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class DashboardService {
    public DashboardSummary summarize(List<Course> courses,
                                      List<Assignment> assignments,
                                      LocalDate today) {
        List<Course> todayCourses = courses.stream()
                .filter(course -> course.meetsOn(today.getDayOfWeek()))
                .sorted(Comparator.comparing(Course::getStartTime))
                .toList();

        LocalDate weekEnd = today.plusDays(7);
        List<Assignment> activeAssignments = assignments.stream()
                .filter(assignment -> !assignment.isCompleted())
                .toList();

        int dueThisWeek = (int) activeAssignments.stream()
                .filter(assignment -> !assignment.getDueDate().isBefore(today))
                .filter(assignment -> !assignment.getDueDate().isAfter(weekEnd))
                .count();

        int highPriorityDue = (int) activeAssignments.stream()
                .filter(assignment -> assignment.getPriority() == Priority.HIGH)
                .filter(assignment -> !assignment.getDueDate().isBefore(today))
                .filter(assignment -> !assignment.getDueDate().isAfter(weekEnd))
                .count();

        int overdue = (int) activeAssignments.stream()
                .filter(assignment -> assignment.getDueDate().isBefore(today))
                .count();

        int completed = (int) assignments.stream()
                .filter(Assignment::isCompleted)
                .count();
        int completionPercentage = assignments.isEmpty()
                ? 0
                : (int) Math.round(completed * 100.0 / assignments.size());

        List<Assignment> upcoming = activeAssignments.stream()
                .sorted(Comparator.comparing(Assignment::getDueDate)
                        .thenComparing(Assignment::getDueTime)
                        .thenComparing(Assignment::getId))
                .limit(6)
                .toList();

        return new DashboardSummary(
                todayCourses, upcoming, dueThisWeek, highPriorityDue,
                overdue, completionPercentage
        );
    }
}
