package com.campusflow.app;

import com.campusflow.app.data.CampusFlowRepository;
import com.campusflow.app.data.DataAccessException;
import com.campusflow.app.data.DatabaseManager;
import com.campusflow.app.i18n.I18n;
import com.campusflow.app.model.Assignment;
import com.campusflow.app.model.AssignmentStatus;
import com.campusflow.app.model.AppSettings;
import com.campusflow.app.model.Course;
import com.campusflow.app.model.CourseColor;
import com.campusflow.app.service.DashboardService;
import com.campusflow.app.service.DashboardSummary;
import com.campusflow.app.ui.FormDialogs;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainController {
    @FXML private BorderPane rootPane;
    @FXML private ScrollPane overviewView;
    @FXML private Button overviewNavButton;
    @FXML private Button scheduleNavButton;
    @FXML private Button assignmentsNavButton;
    @FXML private Button coursesNavButton;
    @FXML private Button settingsNavButton;
    @FXML private Label profileAvatarLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileSemesterLabel;
    @FXML private Label currentDateLabel;
    @FXML private Label greetingLabel;
    @FXML private Label todayClassesValueLabel;
    @FXML private Label todayClassesNoteLabel;
    @FXML private Label dueThisWeekValueLabel;
    @FXML private Label dueThisWeekNoteLabel;
    @FXML private Label overdueValueLabel;
    @FXML private Label overdueNoteLabel;
    @FXML private Label completionValueLabel;
    @FXML private ProgressBar completionProgressBar;
    @FXML private ProgressBar weeklyFocusProgressBar;
    @FXML private Label weeklyFocusProgressLabel;
    @FXML private Label todayScheduleSubtitleLabel;
    @FXML private Label upcomingAssignmentsSubtitleLabel;
    @FXML private VBox todayScheduleContainer;
    @FXML private VBox upcomingAssignmentsContainer;

    private final DashboardService dashboardService = new DashboardService();
    private CampusFlowRepository repository;
    private Node scheduleView;
    private ScheduleController scheduleController;
    private Node coursesView;
    private CoursesController coursesController;
    private Node assignmentsView;
    private AssignmentsController assignmentsController;
    private Node settingsView;
    private SettingsController settingsController;
    private AppSettings appSettings = AppSettings.defaults();

    @FXML
    private void onShowOverview() {
        rootPane.setCenter(overviewView);
        setActiveNavigation(overviewNavButton);
        refreshDashboard();
    }

    @FXML
    private void onShowSchedule() {
        if (repository == null) {
            return;
        }
        try {
            prepareScheduleView();
            scheduleController.refreshSchedule();
            rootPane.setCenter(scheduleView);
            setActiveNavigation(scheduleNavButton);
        } catch (IOException exception) {
            showAlert(
                    Alert.AlertType.ERROR,
                    I18n.text("schedule.error.open"),
                    exception.getMessage()
            );
        }
    }

    @FXML
    private void onShowCourses() {
        if (repository == null) {
            return;
        }
        try {
            prepareCoursesView();
            coursesController.refreshCourses();
            rootPane.setCenter(coursesView);
            setActiveNavigation(coursesNavButton);
        } catch (IOException exception) {
            showAlert(
                    Alert.AlertType.ERROR,
                    I18n.text("courses.error.open"),
                    exception.getMessage()
            );
        }
    }

    @FXML
    private void onShowAssignments() {
        if (repository == null) {
            return;
        }
        try {
            prepareAssignmentsView();
            assignmentsController.refreshAssignments();
            rootPane.setCenter(assignmentsView);
            setActiveNavigation(assignmentsNavButton);
        } catch (IOException exception) {
            showAlert(
                    Alert.AlertType.ERROR,
                    I18n.text("assignments.error.open"),
                    exception.getMessage()
            );
        }
    }

    @FXML
    private void onShowSettings() {
        if (repository == null) {
            return;
        }
        try {
            prepareSettingsView();
            settingsController.loadSettings();
            rootPane.setCenter(settingsView);
            setActiveNavigation(settingsNavButton);
        } catch (IOException exception) {
            showAlert(
                    Alert.AlertType.ERROR,
                    I18n.text("settings.error.open"),
                    exception.getMessage()
            );
        }
    }

    @FXML
    private void initialize() {
        currentDateLabel.setText(I18n.date(
                LocalDate.now(), "date.current.pattern"
        ));
        greetingLabel.setText(createGreeting());

        try {
            DatabaseManager databaseManager = new DatabaseManager();
            databaseManager.initialize();
            repository = new CampusFlowRepository(databaseManager);
            appSettings = repository.findSettings();
            applySettings(appSettings);
            refreshDashboard();
            prepareScheduleView();
            prepareCoursesView();
            prepareAssignmentsView();
            prepareSettingsView();
            Platform.runLater(this::showStartupReminders);
        } catch (DataAccessException exception) {
            showUnavailableState();
            Platform.runLater(() -> showAlert(
                    Alert.AlertType.ERROR,
                    I18n.text("alert.database.header"),
                    exception.getMessage()
            ));
        } catch (IOException exception) {
            Platform.runLater(() -> showAlert(
                    Alert.AlertType.ERROR,
                    I18n.text("alert.page.header"),
                    exception.getMessage()
            ));
        }
    }

    private void prepareScheduleView() throws IOException {
        if (scheduleView != null) {
            return;
        }
        FXMLLoader loader = new FXMLLoader(
                CampusFlowApplication.class.getResource("schedule-view.fxml"),
                I18n.bundle()
        );
        scheduleView = loader.load();
        scheduleController = loader.getController();
        scheduleController.setRepository(repository);
    }

    private void prepareCoursesView() throws IOException {
        if (coursesView != null) {
            return;
        }
        FXMLLoader loader = new FXMLLoader(
                CampusFlowApplication.class.getResource("courses-view.fxml"),
                I18n.bundle()
        );
        coursesView = loader.load();
        coursesController = loader.getController();
        coursesController.setRepository(repository);
    }

    private void prepareAssignmentsView() throws IOException {
        if (assignmentsView != null) {
            return;
        }
        FXMLLoader loader = new FXMLLoader(
                CampusFlowApplication.class.getResource("assignments-view.fxml"),
                I18n.bundle()
        );
        assignmentsView = loader.load();
        assignmentsController = loader.getController();
        assignmentsController.setRepository(repository);
    }

    private void prepareSettingsView() throws IOException {
        if (settingsView != null) {
            return;
        }
        FXMLLoader loader = new FXMLLoader(
                CampusFlowApplication.class.getResource("settings-view.fxml"),
                I18n.bundle()
        );
        settingsView = loader.load();
        settingsController = loader.getController();
        settingsController.configure(
                repository, this::onSettingsSaved, this::refreshAllViews
        );
    }

    @FXML
    private void onAddCourse() {
        if (repository == null) {
            return;
        }
        FormDialogs.showCourseDialog(getWindow(), appSettings).ifPresent(course -> {
            try {
                repository.addCourse(course);
                refreshDashboard();
                refreshCourseViews();
            } catch (DataAccessException exception) {
                showDataError(exception);
            }
        });
    }

    @FXML
    private void onQuickAddAssignment() {
        if (repository == null) {
            return;
        }
        try {
            List<Course> courses = repository.findAllCourses();
            if (courses.isEmpty()) {
                showAlert(
                        Alert.AlertType.INFORMATION,
                        I18n.text("alert.addCourseFirst.header"),
                        I18n.text("alert.addCourseFirst.content")
                );
                return;
            }

            FormDialogs.showAssignmentDialog(getWindow(), courses, appSettings)
                    .ifPresent(assignment -> {
                        try {
                            repository.addAssignment(assignment);
                            refreshDashboard();
                            refreshAssignmentView();
                        } catch (DataAccessException exception) {
                            showDataError(exception);
                        }
                    });
        } catch (DataAccessException exception) {
            showDataError(exception);
        }
    }

    private void refreshDashboard() {
        try {
            List<Course> courses = repository.findAllCourses();
            List<Assignment> assignments = repository.findAllAssignments();
            LocalDate today = LocalDate.now();
            DashboardSummary summary = dashboardService.summarize(
                    courses, assignments, today
            );

            Map<Integer, Course> coursesById = new HashMap<>();
            for (Course course : courses) {
                coursesById.put(course.getId(), course);
            }

            updateStatistics(summary);
            renderTodaySchedule(summary.getTodayCourses(), today);
            renderUpcomingAssignments(
                    summary.getUpcomingAssignments(), coursesById, today
            );
        } catch (DataAccessException exception) {
            showDataError(exception);
        }
    }

    private void updateStatistics(DashboardSummary summary) {
        int todayCount = summary.getTodayCourses().size();
        todayClassesValueLabel.setText(Integer.toString(todayCount));
        if (todayCount == 0) {
            todayClassesNoteLabel.setText(I18n.text("overview.stat.noClasses"));
        } else {
            LocalTime nextTime = summary.getTodayCourses().getFirst().getStartTime();
            todayClassesNoteLabel.setText(I18n.text(
                    "overview.stat.firstAt", appSettings.formatTime(nextTime)
            ));
        }

        dueThisWeekValueLabel.setText(
                Integer.toString(summary.getDueThisWeekCount())
        );
        int highPriority = summary.getHighPriorityDueCount();
        dueThisWeekNoteLabel.setText(highPriority == 0
                ? I18n.text("overview.stat.noHighPriority")
                : I18n.text("overview.stat.highPriority", highPriority));

        overdueValueLabel.setText(Integer.toString(summary.getOverdueCount()));
        overdueNoteLabel.setText(summary.getOverdueCount() == 0
                ? I18n.text("overview.stat.caughtUp")
                : I18n.text("overview.stat.needsAttention"));

        int completion = summary.getCompletionPercentage();
        completionValueLabel.setText(completion + "%");
        completionProgressBar.setProgress(completion / 100.0);
        weeklyFocusProgressBar.setProgress(completion / 100.0);
        weeklyFocusProgressLabel.setText(I18n.text(
                "progress.percentage", completion
        ));
    }

    private void renderTodaySchedule(List<Course> courses, LocalDate today) {
        todayScheduleContainer.getChildren().clear();
        String weekday = today.getDayOfWeek().getDisplayName(
                TextStyle.FULL, I18n.locale()
        );
        todayScheduleSubtitleLabel.setText(
                I18n.text(courses.size() == 1
                                ? "overview.classes.one" : "overview.classes.many",
                        weekday, courses.size())
        );

        if (courses.isEmpty()) {
            todayScheduleContainer.getChildren().add(createEmptyState(
                    I18n.text("overview.empty.schedule.title"),
                    I18n.text("overview.empty.schedule.copy"),
                    I18n.text("common.addCourse"),
                    this::onAddCourse
            ));
            return;
        }

        for (Course course : courses) {
            todayScheduleContainer.getChildren().add(createCourseRow(course));
        }
    }

    private HBox createCourseRow(Course course) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("schedule-row");

        VBox timeBox = new VBox(2);
        timeBox.setAlignment(Pos.CENTER);
        timeBox.setPrefWidth(66);
        String formattedTime = appSettings.formatTime(course.getStartTime());
        String[] timeParts = formattedTime.split(" ", 2);
        Label time = styledLabel(timeParts[0], "time-primary");
        Label period = styledLabel(
                timeParts.length == 2 ? timeParts[1] : "", "time-secondary"
        );
        period.setManaged(timeParts.length == 2);
        period.setVisible(timeParts.length == 2);
        timeBox.getChildren().addAll(time, period);

        Region accent = new Region();
        accent.setPrefSize(4, 52);
        accent.setMinWidth(4);
        accent.getStyleClass().add(courseAccentClass(course.getColor()));

        VBox details = new VBox(3);
        HBox.setHgrow(details, Priority.ALWAYS);
        Label name = styledLabel(course.getName(), "course-name");
        Label meta = styledLabel(courseMeta(course), "course-meta");
        details.getChildren().addAll(name, meta);

        Label duration = styledLabel(
                I18n.text("overview.duration", course.getDurationMinutes()),
                "duration-pill"
        );
        row.getChildren().addAll(timeBox, accent, details, duration);
        return row;
    }

    private void renderUpcomingAssignments(List<Assignment> assignments,
                                           Map<Integer, Course> coursesById,
                                           LocalDate today) {
        upcomingAssignmentsContainer.getChildren().clear();
        upcomingAssignmentsSubtitleLabel.setText(
                assignments.isEmpty()
                        ? I18n.text("overview.nothingWaiting")
                        : I18n.text("overview.assignmentOrder")
        );

        if (assignments.isEmpty()) {
            upcomingAssignmentsContainer.getChildren().add(createEmptyState(
                    I18n.text("overview.empty.assignments.title"),
                    I18n.text("overview.empty.assignments.copy"),
                    I18n.text("common.addAssignment"),
                    this::onQuickAddAssignment
            ));
            return;
        }

        for (int index = 0; index < assignments.size(); index++) {
            Assignment assignment = assignments.get(index);
            Course course = coursesById.get(assignment.getCourseId());
            upcomingAssignmentsContainer.getChildren().add(
                    createAssignmentRow(
                            assignment, course, today,
                            index == assignments.size() - 1
                    )
            );
        }
    }

    private HBox createAssignmentRow(Assignment assignment, Course course,
                                     LocalDate today, boolean lastRow) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(lastRow ? "task-row-last" : "task-row");

        CheckBox completedCheckBox = new CheckBox();
        completedCheckBox.getStyleClass().add("task-check-box");
        completedCheckBox.setAccessibleText(
                I18n.text("overview.markComplete", assignment.getTitle())
        );
        completedCheckBox.setOnAction(event -> {
            if (completedCheckBox.isSelected()) {
                try {
                    repository.updateAssignmentStatus(
                            assignment.getId(), AssignmentStatus.COMPLETED
                    );
                    refreshDashboard();
                    refreshAssignmentView();
                } catch (DataAccessException exception) {
                    completedCheckBox.setSelected(false);
                    showDataError(exception);
                }
            }
        });

        Region courseAccent = new Region();
        courseAccent.getStyleClass().add("assignment-course-accent");
        courseAccent.getStyleClass().add(course == null
                ? "course-accent-blue" : courseAccentClass(course.getColor()));

        VBox details = new VBox(5);
        HBox.setHgrow(details, Priority.ALWAYS);
        Label title = styledLabel(assignment.getTitle(), "task-title");
        String courseLabel = course == null
                ? I18n.text("overview.courseFallback")
                : course.getCode() + " · " + course.getName();
        Label meta = styledLabel(
                courseLabel + " · " + dueText(assignment, today),
                "task-meta"
        );
        meta.setWrapText(true);
        if (assignment.getDueDate().isBefore(today)) {
            meta.getStyleClass().add("task-meta-overdue");
        }
        details.getChildren().addAll(title, meta);

        Label priority = styledLabel(
                assignment.getPriority().getDisplayName()
                        .toUpperCase(I18n.locale()),
                assignment.getPriority().getCssClass()
        );
        priority.setMinWidth(Region.USE_PREF_SIZE);
        priority.setMaxWidth(Region.USE_PREF_SIZE);
        row.getChildren().addAll(
                completedCheckBox, courseAccent, details, priority
        );
        return row;
    }

    private VBox createEmptyState(String title, String message,
                                  String buttonText, Runnable action) {
        VBox emptyState = new VBox(7);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.getStyleClass().add("empty-state");
        Label titleLabel = styledLabel(title, "empty-state-title");
        Label messageLabel = styledLabel(message, "empty-state-copy");
        messageLabel.setWrapText(true);
        Button actionButton = new Button(buttonText);
        actionButton.getStyleClass().add("empty-state-button");
        actionButton.setOnAction(event -> action.run());
        emptyState.getChildren().addAll(titleLabel, messageLabel, actionButton);
        return emptyState;
    }

    private Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private String courseMeta(Course course) {
        StringBuilder meta = new StringBuilder(course.getCode());
        if (!course.getLocation().isBlank()) {
            meta.append(" · ").append(course.getLocation());
        }
        if (!course.getInstructor().isBlank()) {
            meta.append(" · ").append(course.getInstructor());
        }
        return meta.toString();
    }

    private String courseAccentClass(String color) {
        return CourseColor.fromStorage(color).accentStyleClass();
    }

    private String dueText(Assignment assignment, LocalDate today) {
        LocalDate dueDate = assignment.getDueDate();
        String deadline = appSettings.formatTime(assignment.getDueTime());
        long days = ChronoUnit.DAYS.between(today, dueDate);
        if (days < 0) {
            long overdueDays = Math.abs(days);
            return I18n.text(overdueDays == 1
                            ? "overview.due.overdue.one"
                            : "overview.due.overdue.many",
                    overdueDays, deadline);
        }
        if (days == 0) {
            return I18n.text("overview.due.today", deadline);
        }
        if (days == 1) {
            return I18n.text("overview.due.tomorrow", deadline);
        }
        if (days <= 7) {
            return I18n.text(
                    "overview.due.weekday",
                    dueDate.getDayOfWeek().getDisplayName(
                            TextStyle.FULL, I18n.locale()
                    ),
                    deadline
            );
        }
        return I18n.text(
                "overview.due.date",
                I18n.date(dueDate, "date.short.pattern"),
                deadline
        );
    }

    private String createGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) {
            return I18n.text("overview.greeting.morning");
        }
        if (hour < 18) {
            return I18n.text("overview.greeting.afternoon");
        }
        return I18n.text("overview.greeting.evening");
    }

    private Window getWindow() {
        return currentDateLabel.getScene() == null
                ? null : currentDateLabel.getScene().getWindow();
    }

    private void setActiveNavigation(Button activeButton) {
        overviewNavButton.getStyleClass().remove("nav-button-active");
        scheduleNavButton.getStyleClass().remove("nav-button-active");
        assignmentsNavButton.getStyleClass().remove("nav-button-active");
        coursesNavButton.getStyleClass().remove("nav-button-active");
        settingsNavButton.getStyleClass().remove("nav-button-active");
        if (!activeButton.getStyleClass().contains("nav-button-active")) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    private void refreshCourseViews() {
        if (scheduleController != null) {
            scheduleController.refreshSchedule();
        }
        if (coursesController != null) {
            coursesController.refreshCourses();
        }
    }

    private void refreshAssignmentView() {
        if (assignmentsController != null) {
            assignmentsController.refreshAssignments();
        }
    }

    private void refreshAllViews() {
        refreshDashboard();
        refreshCourseViews();
        refreshAssignmentView();
    }

    private void onSettingsSaved(AppSettings settings) {
        boolean languageChanged = appSettings.getLanguage() != settings.getLanguage();
        appSettings = settings;
        if (languageChanged) {
            I18n.setLanguage(settings.getLanguage());
            Platform.runLater(CampusFlowApplication::reloadMainView);
            return;
        }
        applySettings(settings);
        refreshAllViews();
    }

    private void applySettings(AppSettings settings) {
        profileNameLabel.setText(settings.getDisplayName());
        profileSemesterLabel.setText(settings.getSemesterName());
        String name = settings.getDisplayName().trim();
        profileAvatarLabel.setText(name.isEmpty()
                ? "S" : name.substring(0, 1).toUpperCase(Locale.ENGLISH));

        rootPane.getStyleClass().removeAll(
                "theme-light", "theme-dark",
                "accent-indigo", "accent-purple", "accent-green", "accent-orange",
                "density-comfortable", "density-compact"
        );
        boolean systemDark = System.getProperty(
                "apple.awt.application.appearance", ""
        ).toLowerCase(Locale.ENGLISH).contains("dark");
        boolean useDark = settings.getTheme() == AppSettings.Theme.DARK
                || (settings.getTheme() == AppSettings.Theme.SYSTEM && systemDark);
        rootPane.getStyleClass().add(useDark ? "theme-dark" : "theme-light");
        rootPane.getStyleClass().add(
                "accent-" + settings.getAccentColor().name().toLowerCase(Locale.ENGLISH)
        );
        rootPane.getStyleClass().add(
                "density-" + settings.getDensity().name().toLowerCase(Locale.ENGLISH)
        );
    }

    private void showStartupReminders() {
        if (!appSettings.isRemindOneDayBefore()
                && !appSettings.isRemindOneHourBefore()
                && !appSettings.isRemindOverdue()) {
            return;
        }
        try {
            Map<Integer, Course> courses = new HashMap<>();
            for (Course course : repository.findAllCourses()) {
                courses.put(course.getId(), course);
            }
            LocalDateTime now = LocalDateTime.now();
            List<String> reminders = repository.findAllAssignments().stream()
                    .filter(assignment -> !assignment.isCompleted())
                    .filter(assignment -> shouldRemind(assignment, now))
                    .limit(8)
                    .map(assignment -> {
                        Course course = courses.get(assignment.getCourseId());
                        String courseName = course == null
                                ? I18n.text("common.course") : course.getCode();
                        return "• " + assignment.getTitle() + " — " + courseName
                                + " — " + assignment.getDueDate() + " "
                                + appSettings.formatTime(assignment.getDueTime());
                    })
                    .toList();
            if (!reminders.isEmpty()) {
                showAlert(
                        Alert.AlertType.INFORMATION,
                        reminders.size() == 1
                                ? I18n.text("alert.assignmentAttention.one")
                                : I18n.text(
                                "alert.assignmentAttention.many", reminders.size()
                        ),
                        String.join("\n", reminders)
                );
            }
        } catch (DataAccessException exception) {
            // Reminders should never prevent CampusFlow from opening.
        }
    }

    private boolean shouldRemind(Assignment assignment, LocalDateTime now) {
        LocalDateTime deadline = LocalDateTime.of(
                assignment.getDueDate(), assignment.getDueTime()
        );
        long minutes = ChronoUnit.MINUTES.between(now, deadline);
        if (minutes < 0) {
            return appSettings.isRemindOverdue();
        }
        if (minutes <= 60 && appSettings.isRemindOneHourBefore()) {
            return true;
        }
        return minutes <= 24 * 60 && appSettings.isRemindOneDayBefore();
    }

    private void showUnavailableState() {
        todayScheduleContainer.getChildren().setAll(styledLabel(
                I18n.text("overview.unavailable.schedule"), "empty-state-copy"
        ));
        upcomingAssignmentsContainer.getChildren().setAll(styledLabel(
                I18n.text("overview.unavailable.assignments"), "empty-state-copy"
        ));
    }

    private void showDataError(DataAccessException exception) {
        showAlert(Alert.AlertType.ERROR, I18n.text("alert.save.header"),
                exception.getMessage());
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("CampusFlow");
        alert.setHeaderText(header);
        alert.setContentText(message);
        Window owner = getWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }
}
