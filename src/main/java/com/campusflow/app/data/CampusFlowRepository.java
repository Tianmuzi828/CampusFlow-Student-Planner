package com.campusflow.app.data;

import com.campusflow.app.i18n.AppLanguage;
import com.campusflow.app.model.Assignment;
import com.campusflow.app.model.AssignmentStatus;
import com.campusflow.app.model.AppSettings;
import com.campusflow.app.model.Course;
import com.campusflow.app.model.Priority;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class CampusFlowRepository {
    private final DatabaseManager databaseManager;

    public CampusFlowRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Course addCourse(Course course) {
        String sql = """
                INSERT INTO courses
                    (code, name, instructor, location, meeting_days, start_date,
                     start_time, end_time, color)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, course.getCode());
            statement.setString(2, course.getName());
            statement.setString(3, course.getInstructor());
            statement.setString(4, course.getLocation());
            statement.setString(5, course.getMeetingDays());
            statement.setString(6, course.getStartDate().toString());
            statement.setString(7, course.getStartTime().toString());
            statement.setString(8, course.getEndTime().toString());
            statement.setString(9, course.getColor());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Course(
                            keys.getInt(1), course.getCode(), course.getName(),
                            course.getInstructor(), course.getLocation(),
                            course.getMeetingDays(), course.getStartDate(),
                            course.getStartTime(), course.getEndTime(),
                            course.getColor()
                    );
                }
            }
            throw new SQLException("No generated ID was returned for the course.");
        } catch (SQLException exception) {
            throw new DataAccessException("Could not save the course.", exception);
        }
    }

    public void updateCourse(Course course) {
        String sql = """
                UPDATE courses
                SET code = ?, name = ?, instructor = ?, location = ?,
                    meeting_days = ?, start_date = ?, start_time = ?,
                    end_time = ?, color = ?
                WHERE id = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, course.getCode());
            statement.setString(2, course.getName());
            statement.setString(3, course.getInstructor());
            statement.setString(4, course.getLocation());
            statement.setString(5, course.getMeetingDays());
            statement.setString(6, course.getStartDate().toString());
            statement.setString(7, course.getStartTime().toString());
            statement.setString(8, course.getEndTime().toString());
            statement.setString(9, course.getColor());
            statement.setInt(10, course.getId());
            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new SQLException("The course no longer exists.");
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not update the course.", exception);
        }
    }

    public List<Course> findAllCourses() {
        String sql = "SELECT * FROM courses ORDER BY code, name";
        List<Course> courses = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                courses.add(readCourse(results));
            }
            return courses;
        } catch (SQLException exception) {
            throw new DataAccessException("Could not load courses.", exception);
        }
    }

    public Assignment addAssignment(Assignment assignment) {
        String sql = """
                INSERT INTO assignments
                    (course_id, title, description, due_date, due_time, priority, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, assignment.getCourseId());
            statement.setString(2, assignment.getTitle());
            statement.setString(3, assignment.getDescription());
            statement.setString(4, assignment.getDueDate().toString());
            statement.setString(5, assignment.getDueTime().toString());
            statement.setString(6, assignment.getPriority().name());
            statement.setString(7, assignment.getStatus().name());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Assignment(
                            keys.getInt(1), assignment.getCourseId(),
                            assignment.getTitle(), assignment.getDescription(),
                            assignment.getDueDate(), assignment.getDueTime(),
                            assignment.getPriority(),
                            assignment.getStatus()
                    );
                }
            }
            throw new SQLException("No generated ID was returned for the assignment.");
        } catch (SQLException exception) {
            throw new DataAccessException("Could not save the assignment.", exception);
        }
    }

    public List<Assignment> findAllAssignments() {
        String sql = "SELECT * FROM assignments ORDER BY due_date, due_time, id";
        List<Assignment> assignments = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                assignments.add(readAssignment(results));
            }
            return assignments;
        } catch (SQLException exception) {
            throw new DataAccessException("Could not load assignments.", exception);
        }
    }

    public void updateAssignmentStatus(int assignmentId, AssignmentStatus status) {
        String sql = "UPDATE assignments SET status = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, assignmentId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Could not update the assignment.", exception);
        }
    }

    public AppSettings findSettings() {
        String sql = "SELECT * FROM app_settings WHERE id = 1";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            if (results.next()) {
                return readSettings(results);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not load settings.", exception);
        }

        AppSettings defaults = AppSettings.defaults();
        saveSettings(defaults);
        return defaults;
    }

    public void saveSettings(AppSettings settings) {
        String sql = """
                INSERT INTO app_settings (
                    id, display_name, semester_name, semester_start, semester_end,
                    week_starts_on, use_24_hour_time, calendar_start_time,
                    calendar_end_time, time_interval_minutes, default_deadline_time,
                    default_priority, show_completed_assignments, theme, accent_color,
                    density, remind_one_day_before, remind_one_hour_before,
                    remind_overdue, language
                ) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    display_name = excluded.display_name,
                    semester_name = excluded.semester_name,
                    semester_start = excluded.semester_start,
                    semester_end = excluded.semester_end,
                    week_starts_on = excluded.week_starts_on,
                    use_24_hour_time = excluded.use_24_hour_time,
                    calendar_start_time = excluded.calendar_start_time,
                    calendar_end_time = excluded.calendar_end_time,
                    time_interval_minutes = excluded.time_interval_minutes,
                    default_deadline_time = excluded.default_deadline_time,
                    default_priority = excluded.default_priority,
                    show_completed_assignments = excluded.show_completed_assignments,
                    theme = excluded.theme,
                    accent_color = excluded.accent_color,
                    density = excluded.density,
                    remind_one_day_before = excluded.remind_one_day_before,
                    remind_one_hour_before = excluded.remind_one_hour_before,
                    remind_overdue = excluded.remind_overdue,
                    language = excluded.language
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, settings.getDisplayName());
            statement.setString(2, settings.getSemesterName());
            statement.setString(3, settings.getSemesterStart().toString());
            statement.setString(4, settings.getSemesterEnd().toString());
            statement.setString(5, settings.getWeekStartsOn().name());
            statement.setInt(6, settings.isUse24HourTime() ? 1 : 0);
            statement.setString(7, settings.getCalendarStartTime().toString());
            statement.setString(8, settings.getCalendarEndTime().toString());
            statement.setInt(9, settings.getTimeIntervalMinutes());
            statement.setString(10, settings.getDefaultDeadlineTime().toString());
            statement.setString(11, settings.getDefaultPriority().name());
            statement.setInt(12, settings.isShowCompletedAssignments() ? 1 : 0);
            statement.setString(13, settings.getTheme().name());
            statement.setString(14, settings.getAccentColor().name());
            statement.setString(15, settings.getDensity().name());
            statement.setInt(16, settings.isRemindOneDayBefore() ? 1 : 0);
            statement.setInt(17, settings.isRemindOneHourBefore() ? 1 : 0);
            statement.setInt(18, settings.isRemindOverdue() ? 1 : 0);
            statement.setString(19, settings.getLanguage().name());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Could not save settings.", exception);
        }
    }

    public void clearPlannerData() {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.executeUpdate("DELETE FROM assignments");
            statement.executeUpdate("DELETE FROM courses");
            connection.commit();
        } catch (SQLException exception) {
            throw new DataAccessException("Could not clear CampusFlow data.", exception);
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    private Course readCourse(ResultSet results) throws SQLException {
        return new Course(
                results.getInt("id"),
                results.getString("code"),
                results.getString("name"),
                results.getString("instructor"),
                results.getString("location"),
                results.getString("meeting_days"),
                LocalDate.parse(results.getString("start_date")),
                LocalTime.parse(results.getString("start_time")),
                LocalTime.parse(results.getString("end_time")),
                results.getString("color")
        );
    }

    private Assignment readAssignment(ResultSet results) throws SQLException {
        return new Assignment(
                results.getInt("id"),
                results.getInt("course_id"),
                results.getString("title"),
                results.getString("description"),
                LocalDate.parse(results.getString("due_date")),
                LocalTime.parse(results.getString("due_time")),
                Priority.valueOf(results.getString("priority")),
                AssignmentStatus.valueOf(results.getString("status"))
        );
    }

    private AppSettings readSettings(ResultSet results) throws SQLException {
        return new AppSettings(
                results.getString("display_name"),
                results.getString("semester_name"),
                LocalDate.parse(results.getString("semester_start")),
                LocalDate.parse(results.getString("semester_end")),
                DayOfWeek.valueOf(results.getString("week_starts_on")),
                results.getInt("use_24_hour_time") == 1,
                LocalTime.parse(results.getString("calendar_start_time")),
                LocalTime.parse(results.getString("calendar_end_time")),
                results.getInt("time_interval_minutes"),
                LocalTime.parse(results.getString("default_deadline_time")),
                Priority.valueOf(results.getString("default_priority")),
                results.getInt("show_completed_assignments") == 1,
                AppSettings.Theme.valueOf(results.getString("theme")),
                AppSettings.AccentColor.valueOf(results.getString("accent_color")),
                AppSettings.Density.valueOf(results.getString("density")),
                results.getInt("remind_one_day_before") == 1,
                results.getInt("remind_one_hour_before") == 1,
                results.getInt("remind_overdue") == 1,
                AppLanguage.fromStorage(results.getString("language"))
        );
    }
}
