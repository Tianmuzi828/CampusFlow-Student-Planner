package com.campusflow.app.data;

import com.campusflow.app.i18n.AppLanguage;
import com.campusflow.app.model.Assignment;
import com.campusflow.app.model.AssignmentStatus;
import com.campusflow.app.model.AppSettings;
import com.campusflow.app.model.Course;
import com.campusflow.app.model.Priority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampusFlowRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesReloadsAndCompletesAnAssignment() {
        Path databaseFile = temporaryDirectory.resolve("campusflow-test.db");
        DatabaseManager databaseManager = new DatabaseManager(databaseFile);
        databaseManager.initialize();
        CampusFlowRepository repository = new CampusFlowRepository(databaseManager);

        Course savedCourse = repository.addCourse(new Course(
                0, "CMPSC 221", "Introduction to Java", "Professor Smith",
                "IST 103", "MON,WED", LocalDate.of(2026, 7, 1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 15), "blue"
        ));
        Assignment savedAssignment = repository.addAssignment(new Assignment(
                0, savedCourse.getId(), "Project proposal", "First draft",
                LocalDate.of(2026, 8, 3), LocalTime.of(17, 30), Priority.HIGH,
                AssignmentStatus.NOT_STARTED
        ));

        List<Course> courses = repository.findAllCourses();
        List<Assignment> assignments = repository.findAllAssignments();

        assertTrue(databaseFile.toFile().exists());
        assertTrue(savedCourse.getId() > 0);
        assertEquals(1, courses.size());
        assertEquals("Introduction to Java", courses.getFirst().getName());
        assertEquals(LocalDate.of(2026, 7, 1), courses.getFirst().getStartDate());
        assertEquals(1, assignments.size());
        assertEquals("Project proposal", assignments.getFirst().getTitle());
        assertEquals(LocalTime.of(17, 30), assignments.getFirst().getDueTime());

        repository.updateCourse(new Course(
                savedCourse.getId(), "CMPSC 221", "Advanced Java",
                "Professor Jones", "IST 201", "TUE,THU",
                LocalDate.of(2026, 8, 10),
                LocalTime.of(8, 10), LocalTime.of(9, 20), "purple"
        ));
        Course updatedCourse = repository.findAllCourses().getFirst();
        assertEquals(savedCourse.getId(), updatedCourse.getId());
        assertEquals("Advanced Java", updatedCourse.getName());
        assertEquals("TUE,THU", updatedCourse.getMeetingDays());
        assertEquals(LocalDate.of(2026, 8, 10), updatedCourse.getStartDate());
        assertEquals(LocalTime.of(8, 10), updatedCourse.getStartTime());
        assertEquals("purple", updatedCourse.getColor());

        repository.updateAssignmentStatus(
                savedAssignment.getId(), AssignmentStatus.COMPLETED
        );
        assertEquals(
                AssignmentStatus.COMPLETED,
                repository.findAllAssignments().getFirst().getStatus()
        );

        AppSettings settings = new AppSettings(
                "Alex", "Fall 2026",
                LocalDate.of(2026, 8, 24), LocalDate.of(2026, 12, 18),
                DayOfWeek.SUNDAY, true,
                LocalTime.of(8, 0), LocalTime.of(21, 0), 15,
                LocalTime.of(23, 45), Priority.HIGH, true,
                AppSettings.Theme.DARK, AppSettings.AccentColor.GREEN,
                AppSettings.Density.COMPACT, true, true, true,
                AppLanguage.SIMPLIFIED_CHINESE
        );
        repository.saveSettings(settings);
        AppSettings reloadedSettings = repository.findSettings();
        assertEquals("Alex", reloadedSettings.getDisplayName());
        assertEquals(DayOfWeek.SUNDAY, reloadedSettings.getWeekStartsOn());
        assertEquals(15, reloadedSettings.getTimeIntervalMinutes());
        assertEquals(AppSettings.Theme.DARK, reloadedSettings.getTheme());
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, reloadedSettings.getLanguage());

        Path backupFile = temporaryDirectory.resolve("campusflow-backup.db");
        databaseManager.backupTo(backupFile);
        repository.clearPlannerData();
        assertTrue(repository.findAllCourses().isEmpty());
        databaseManager.restoreFrom(backupFile);
        assertEquals(1, repository.findAllCourses().size());
        assertEquals("Project proposal", repository.findAllAssignments().getFirst().getTitle());
    }

    @Test
    void migratesExistingAssignmentsWithoutLosingTheirData() throws Exception {
        Path databaseFile = temporaryDirectory.resolve("legacy-campusflow.db");
        DatabaseManager databaseManager = new DatabaseManager(databaseFile);

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE courses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        code TEXT NOT NULL,
                        name TEXT NOT NULL,
                        instructor TEXT NOT NULL DEFAULT '',
                        location TEXT NOT NULL DEFAULT '',
                        meeting_days TEXT NOT NULL,
                        start_time TEXT NOT NULL,
                        end_time TEXT NOT NULL,
                        color TEXT NOT NULL DEFAULT 'blue'
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE assignments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        course_id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        due_date TEXT NOT NULL,
                        priority TEXT NOT NULL,
                        status TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO courses
                        (code, name, meeting_days, start_time, end_time)
                    VALUES ('CMPSC 465', 'Data Structures', 'MON,WED', '08:00', '09:00')
                    """);
            statement.executeUpdate("""
                    INSERT INTO assignments
                        (course_id, title, due_date, priority, status)
                    VALUES (1, 'homework1', '2026-08-01', 'HIGH', 'NOT_STARTED')
                    """);
        }

        databaseManager.initialize();
        CampusFlowRepository repository = new CampusFlowRepository(databaseManager);
        Assignment migrated = repository.findAllAssignments().getFirst();
        Course migratedCourse = repository.findAllCourses().getFirst();

        assertEquals("homework1", migrated.getTitle());
        assertEquals(LocalDate.of(2026, 8, 1), migrated.getDueDate());
        assertEquals(LocalTime.of(23, 59), migrated.getDueTime());
        assertEquals(Priority.HIGH, migrated.getPriority());
        assertEquals(LocalDate.now().withDayOfMonth(1), migratedCourse.getStartDate());
    }
}
