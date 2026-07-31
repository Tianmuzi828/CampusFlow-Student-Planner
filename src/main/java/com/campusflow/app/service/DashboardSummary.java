package com.campusflow.app.service;

import com.campusflow.app.model.Assignment;
import com.campusflow.app.model.Course;

import java.util.List;

public class DashboardSummary {
    private final List<Course> todayCourses;
    private final List<Assignment> upcomingAssignments;
    private final int dueThisWeekCount;
    private final int highPriorityDueCount;
    private final int overdueCount;
    private final int completionPercentage;

    public DashboardSummary(List<Course> todayCourses,
                            List<Assignment> upcomingAssignments,
                            int dueThisWeekCount, int highPriorityDueCount,
                            int overdueCount, int completionPercentage) {
        this.todayCourses = todayCourses;
        this.upcomingAssignments = upcomingAssignments;
        this.dueThisWeekCount = dueThisWeekCount;
        this.highPriorityDueCount = highPriorityDueCount;
        this.overdueCount = overdueCount;
        this.completionPercentage = completionPercentage;
    }

    public List<Course> getTodayCourses() {
        return todayCourses;
    }

    public List<Assignment> getUpcomingAssignments() {
        return upcomingAssignments;
    }

    public int getDueThisWeekCount() {
        return dueThisWeekCount;
    }

    public int getHighPriorityDueCount() {
        return highPriorityDueCount;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public int getCompletionPercentage() {
        return completionPercentage;
    }
}
