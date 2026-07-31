package com.campusflow.app.service;

import com.campusflow.app.model.Assignment;
import com.campusflow.app.model.AssignmentStatus;
import com.campusflow.app.model.Course;
import com.campusflow.app.model.Priority;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardServiceTest {
    private final DashboardService service = new DashboardService();

    @Test
    void summarizesCoursesDeadlinesAndCompletion() {
        LocalDate monday = LocalDate.of(2026, 7, 27);
        Course java = course(1, "MON,WED", LocalTime.of(10, 0));
        Course tuesdayCourse = course(2, "TUE,THU", LocalTime.of(9, 0));

        List<Assignment> assignments = List.of(
                assignment(1, monday, Priority.HIGH, AssignmentStatus.NOT_STARTED),
                assignment(2, monday.minusDays(2), Priority.LOW, AssignmentStatus.IN_PROGRESS),
                assignment(3, monday.plusDays(1), Priority.MEDIUM, AssignmentStatus.COMPLETED)
        );

        DashboardSummary summary = service.summarize(
                List.of(java, tuesdayCourse), assignments, monday
        );

        assertEquals(1, summary.getTodayCourses().size());
        assertEquals("CMPSC 221", summary.getTodayCourses().getFirst().getCode());
        assertEquals(1, summary.getDueThisWeekCount());
        assertEquals(1, summary.getHighPriorityDueCount());
        assertEquals(1, summary.getOverdueCount());
        assertEquals(33, summary.getCompletionPercentage());
        assertEquals(2, summary.getUpcomingAssignments().size());
        assertEquals(2, summary.getUpcomingAssignments().getFirst().getId());
    }

    @Test
    void returnsZeroCompletionWhenThereAreNoAssignments() {
        DashboardSummary summary = service.summarize(
                List.of(), List.of(), LocalDate.of(2026, 7, 27)
        );

        assertEquals(0, summary.getCompletionPercentage());
        assertEquals(0, summary.getUpcomingAssignments().size());
    }

    private Course course(int id, String meetingDays, LocalTime startTime) {
        return new Course(
                id, "CMPSC 221", "Introduction to Java", "Professor",
                "IST 103", meetingDays, startTime, startTime.plusMinutes(75),
                "blue"
        );
    }

    private Assignment assignment(int id, LocalDate dueDate, Priority priority,
                                  AssignmentStatus status) {
        return new Assignment(
                id, 1, "Assignment " + id, "", dueDate,
                LocalTime.of(23, 50), priority, status
        );
    }
}
