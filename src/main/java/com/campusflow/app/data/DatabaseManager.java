package com.campusflow.app.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final Path databaseFile;

    public DatabaseManager() {
        this(Path.of(System.getProperty("user.dir"), "data", "campusflow.db"));
    }

    public DatabaseManager(Path databaseFile) {
        this.databaseFile = databaseFile;
    }

    public void initialize() {
        try {
            Path parent = databaseFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS courses (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            code TEXT NOT NULL,
                            name TEXT NOT NULL,
                            instructor TEXT NOT NULL DEFAULT '',
                            location TEXT NOT NULL DEFAULT '',
                            meeting_days TEXT NOT NULL,
                            start_date TEXT NOT NULL DEFAULT '1970-01-01',
                            start_time TEXT NOT NULL,
                            end_time TEXT NOT NULL,
                            color TEXT NOT NULL DEFAULT 'blue'
                        )
                        """);
                boolean migratedCourseStartDate =
                        ensureCourseStartDateColumn(connection);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS assignments (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            course_id INTEGER NOT NULL,
                            title TEXT NOT NULL,
                            description TEXT NOT NULL DEFAULT '',
                            due_date TEXT NOT NULL,
                            due_time TEXT NOT NULL DEFAULT '23:59',
                            priority TEXT NOT NULL,
                            status TEXT NOT NULL,
                            FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
                        )
                        """);
                ensureAssignmentDeadlineColumn(connection);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS app_settings (
                            id INTEGER PRIMARY KEY CHECK (id = 1),
                            display_name TEXT NOT NULL,
                            semester_name TEXT NOT NULL,
                            semester_start TEXT NOT NULL,
                            semester_end TEXT NOT NULL,
                            week_starts_on TEXT NOT NULL,
                            use_24_hour_time INTEGER NOT NULL,
                            calendar_start_time TEXT NOT NULL,
                            calendar_end_time TEXT NOT NULL,
                            time_interval_minutes INTEGER NOT NULL,
                            default_deadline_time TEXT NOT NULL,
                            default_priority TEXT NOT NULL,
                            show_completed_assignments INTEGER NOT NULL,
                            theme TEXT NOT NULL,
                            accent_color TEXT NOT NULL,
                            density TEXT NOT NULL,
                            remind_one_day_before INTEGER NOT NULL,
                            remind_one_hour_before INTEGER NOT NULL,
                            remind_overdue INTEGER NOT NULL,
                            language TEXT NOT NULL DEFAULT 'ENGLISH'
                        )
                        """);
                ensureSettingsLanguageColumn(connection);
                if (migratedCourseStartDate) {
                    initializeLegacyCourseStartDates(connection);
                }
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_assignments_due_date
                        ON assignments(due_date)
                        """);
            }
        } catch (IOException | SQLException exception) {
            throw new DataAccessException("Could not initialize the CampusFlow database.", exception);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + databaseFile.toAbsolutePath()
        );
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public Path getDatabaseFile() {
        return databaseFile;
    }

    public void backupTo(Path destination) {
        try {
            Files.copy(
                    databaseFile.toAbsolutePath(),
                    destination.toAbsolutePath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException exception) {
            throw new DataAccessException("Could not create the database backup.", exception);
        }
    }

    public void restoreFrom(Path source) {
        Path absoluteSource = source.toAbsolutePath();
        Path absoluteDatabase = databaseFile.toAbsolutePath();
        validateBackup(absoluteSource);

        try {
            if (Files.isSameFile(absoluteSource, absoluteDatabase)) {
                throw new DataAccessException(
                        "The selected backup is already the active CampusFlow database."
                );
            }
        } catch (IOException exception) {
            throw new DataAccessException("Could not inspect the selected backup.", exception);
        }

        Path rollbackFile = null;
        try {
            Path parent = absoluteDatabase.getParent();
            rollbackFile = Files.createTempFile(parent, "campusflow-before-restore-", ".db");
            Files.copy(absoluteDatabase, rollbackFile, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(absoluteSource, absoluteDatabase, StandardCopyOption.REPLACE_EXISTING);
            initialize();
        } catch (Exception exception) {
            if (rollbackFile != null) {
                try {
                    Files.copy(
                            rollbackFile, absoluteDatabase,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (IOException ignored) {
                    // The original exception remains the most useful error to report.
                }
            }
            throw new DataAccessException("Could not restore the database backup.", exception);
        } finally {
            if (rollbackFile != null) {
                try {
                    Files.deleteIfExists(rollbackFile);
                } catch (IOException ignored) {
                    // A temporary rollback file does not affect the restored database.
                }
            }
        }
    }

    private void validateBackup(Path source) {
        if (!Files.isRegularFile(source)) {
            throw new DataAccessException("Please choose a valid CampusFlow database file.");
        }

        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + source);
             Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("PRAGMA integrity_check")) {
                if (!result.next() || !"ok".equalsIgnoreCase(result.getString(1))) {
                    throw new SQLException("SQLite integrity check failed.");
                }
            }

            boolean hasCourses = false;
            boolean hasAssignments = false;
            try (ResultSet tables = statement.executeQuery("""
                    SELECT name FROM sqlite_master
                    WHERE type = 'table' AND name IN ('courses', 'assignments')
                    """)) {
                while (tables.next()) {
                    hasCourses |= "courses".equals(tables.getString("name"));
                    hasAssignments |= "assignments".equals(tables.getString("name"));
                }
            }
            if (!hasCourses || !hasAssignments) {
                throw new SQLException("Required CampusFlow tables are missing.");
            }
        } catch (SQLException exception) {
            throw new DataAccessException(
                    "The selected file is not a valid CampusFlow backup.", exception
            );
        }
    }

    private void ensureAssignmentDeadlineColumn(Connection connection)
            throws SQLException {
        boolean columnExists = false;
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery(
                     "PRAGMA table_info(assignments)")) {
            while (columns.next()) {
                if ("due_time".equalsIgnoreCase(columns.getString("name"))) {
                    columnExists = true;
                    break;
                }
            }
        }

        if (!columnExists) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        ALTER TABLE assignments
                        ADD COLUMN due_time TEXT NOT NULL DEFAULT '23:59'
                        """);
            }
        }
    }

    private boolean ensureCourseStartDateColumn(Connection connection)
            throws SQLException {
        boolean columnExists = false;
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery(
                     "PRAGMA table_info(courses)")) {
            while (columns.next()) {
                if ("start_date".equalsIgnoreCase(columns.getString("name"))) {
                    columnExists = true;
                    break;
                }
            }
        }

        if (columnExists) {
            return false;
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    ALTER TABLE courses
                    ADD COLUMN start_date TEXT NOT NULL DEFAULT '1970-01-01'
                    """);
        }
        return true;
    }

    private void initializeLegacyCourseStartDates(Connection connection)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE courses
                    SET start_date = COALESCE(
                        (SELECT semester_start FROM app_settings WHERE id = 1),
                        strftime('%Y-%m-01', 'now')
                    )
                    WHERE start_date = '1970-01-01'
                    """);
        }
    }

    private void ensureSettingsLanguageColumn(Connection connection)
            throws SQLException {
        boolean columnExists = false;
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery(
                     "PRAGMA table_info(app_settings)")) {
            while (columns.next()) {
                if ("language".equalsIgnoreCase(columns.getString("name"))) {
                    columnExists = true;
                    break;
                }
            }
        }

        if (!columnExists) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        ALTER TABLE app_settings
                        ADD COLUMN language TEXT NOT NULL DEFAULT 'ENGLISH'
                        """);
            }
        }
    }
}
