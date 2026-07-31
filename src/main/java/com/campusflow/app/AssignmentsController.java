package com.campusflow.app;

import com.campusflow.app.data.CampusFlowRepository;
import com.campusflow.app.data.DataAccessException;
import com.campusflow.app.i18n.I18n;
import com.campusflow.app.model.Assignment;
import com.campusflow.app.model.AssignmentStatus;
import com.campusflow.app.model.AppSettings;
import com.campusflow.app.model.Course;
import com.campusflow.app.model.CourseColor;
import com.campusflow.app.model.Priority;
import com.campusflow.app.ui.FormDialogs;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignmentsController {
    @FXML private Label unfinishedCountLabel;
    @FXML private Label monthLabel;
    @FXML private GridPane monthGrid;
    @FXML private Label selectedDateLabel;
    @FXML private Label selectedDateCountLabel;
    @FXML private VBox selectedAssignmentsContainer;

    private CampusFlowRepository repository;
    private YearMonth displayedMonth;
    private LocalDate selectedDate;
    private List<Assignment> visibleAssignments = List.of();
    private Map<Integer, Course> coursesById = Map.of();
    private AppSettings settings = AppSettings.defaults();

    @FXML
    private void initialize() {
        displayedMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        configureCalendarGrid();
    }

    public void setRepository(CampusFlowRepository repository) {
        this.repository = repository;
        refreshAssignments();
    }

    public void refreshAssignments() {
        if (repository == null) {
            return;
        }
        try {
            List<Course> courses = repository.findAllCourses();
            Map<Integer, Course> courseMap = new HashMap<>();
            for (Course course : courses) {
                courseMap.put(course.getId(), course);
            }
            coursesById = courseMap;
            settings = repository.findSettings();
            List<Assignment> allAssignments = repository.findAllAssignments();
            visibleAssignments = allAssignments.stream()
                    .filter(assignment -> settings.isShowCompletedAssignments()
                            || !assignment.isCompleted())
                    .sorted(Comparator.comparing(Assignment::getDueDate)
                            .thenComparing(Assignment::getDueTime)
                            .thenComparing(Assignment::getId))
                    .toList();

            int count = (int) allAssignments.stream()
                    .filter(assignment -> !assignment.isCompleted()).count();
            unfinishedCountLabel.setText(I18n.text(count == 1
                    ? "assignments.count.one" : "assignments.count.many", count));
            renderMonth();
            renderSelectedDate();
        } catch (DataAccessException exception) {
            showError(I18n.text("assignments.error.load"), exception.getMessage());
        }
    }

    @FXML
    private void onPreviousMonth() {
        displayedMonth = displayedMonth.minusMonths(1);
        selectedDate = displayedMonth.atDay(1);
        renderMonth();
        renderSelectedDate();
    }

    @FXML
    private void onThisMonth() {
        displayedMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        renderMonth();
        renderSelectedDate();
    }

    @FXML
    private void onNextMonth() {
        displayedMonth = displayedMonth.plusMonths(1);
        selectedDate = displayedMonth.atDay(1);
        renderMonth();
        renderSelectedDate();
    }

    @FXML
    private void onAddAssignment() {
        try {
            List<Course> courses = repository.findAllCourses();
            if (courses.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("CampusFlow");
                alert.setHeaderText(I18n.text("alert.addCourseFirst.header"));
                alert.setContentText(I18n.text("alert.addCourseFirst.content"));
                if (getWindow() != null) {
                    alert.initOwner(getWindow());
                }
                alert.showAndWait();
                return;
            }

            FormDialogs.showAssignmentDialog(
                    getWindow(), courses, repository.findSettings()
            )
                    .ifPresent(assignment -> {
                        try {
                            repository.addAssignment(assignment);
                            selectedDate = assignment.getDueDate();
                            displayedMonth = YearMonth.from(selectedDate);
                            refreshAssignments();
                        } catch (DataAccessException exception) {
                            showError(I18n.text("alert.save.header"), exception.getMessage());
                        }
                    });
        } catch (DataAccessException exception) {
            showError(I18n.text("assignments.error.load"), exception.getMessage());
        }
    }

    private void configureCalendarGrid() {
        monthGrid.getColumnConstraints().clear();
        for (int column = 0; column < 7; column++) {
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(100.0 / 7.0);
            constraint.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            monthGrid.getColumnConstraints().add(constraint);
        }

        monthGrid.getRowConstraints().clear();
        monthGrid.getRowConstraints().add(new RowConstraints(34, 34, 34));
        for (int row = 0; row < 6; row++) {
            RowConstraints constraint = new RowConstraints(70, 82, Double.MAX_VALUE);
            constraint.setVgrow(javafx.scene.layout.Priority.ALWAYS);
            monthGrid.getRowConstraints().add(constraint);
        }
    }

    private void renderMonth() {
        monthGrid.getChildren().clear();
        monthLabel.setText(I18n.date(displayedMonth.atDay(1), "date.month.pattern"));

        DayOfWeek[] weekdays = new DayOfWeek[7];
        for (int index = 0; index < weekdays.length; index++) {
            weekdays[index] = settings.getWeekStartsOn().plus(index);
        }
        for (int column = 0; column < weekdays.length; column++) {
            Label weekday = new Label(weekdays[column].getDisplayName(
                    TextStyle.SHORT, I18n.locale()).toUpperCase(I18n.locale()));
            weekday.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            weekday.setAlignment(Pos.CENTER);
            weekday.getStyleClass().add("assignment-weekday-header");
            monthGrid.add(weekday, column, 0);
        }

        LocalDate firstVisibleDate = displayedMonth.atDay(1)
                .with(TemporalAdjusters.previousOrSame(settings.getWeekStartsOn()));
        for (int index = 0; index < 42; index++) {
            LocalDate date = firstVisibleDate.plusDays(index);
            VBox dayCell = createDayCell(date);
            monthGrid.add(dayCell, index % 7, index / 7 + 1);
            GridPane.setHgrow(dayCell, javafx.scene.layout.Priority.ALWAYS);
            GridPane.setVgrow(dayCell, javafx.scene.layout.Priority.ALWAYS);
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(7);
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cell.getStyleClass().add("assignment-calendar-day");
        if (!YearMonth.from(date).equals(displayedMonth)) {
            cell.getStyleClass().add("assignment-calendar-day-outside");
        }
        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("assignment-calendar-day-today");
        }
        if (date.equals(selectedDate)) {
            cell.getStyleClass().add("assignment-calendar-day-selected");
        }

        Label dayNumber = new Label(Integer.toString(date.getDayOfMonth()));
        dayNumber.getStyleClass().add("assignment-day-number");
        cell.getChildren().add(dayNumber);

        List<Assignment> dueAssignments = assignmentsDueOn(date);
        if (!dueAssignments.isEmpty()) {
            Label dueCount = new Label(I18n.text(
                    "assignments.calendar.dueCount", dueAssignments.size()));
            dueCount.getStyleClass().add("assignment-date-count");
            if (dueAssignments.stream()
                    .anyMatch(item -> !item.isCompleted()
                            && item.getPriority() == Priority.HIGH)) {
                dueCount.getStyleClass().add("assignment-date-count-high");
            }
            cell.getChildren().add(dueCount);

            HBox courseDots = new HBox(4);
            dueAssignments.stream()
                    .map(item -> coursesById.get(item.getCourseId()))
                    .filter(course -> course != null)
                    .distinct()
                    .forEach(course -> {
                        Region dot = new Region();
                        dot.getStyleClass().addAll(
                                "assignment-course-dot",
                                courseAccentClass(course.getColor())
                        );
                        Tooltip.install(dot, new Tooltip(
                                course.getCode() + " · " + course.getName()
                        ));
                        courseDots.getChildren().add(dot);
                    });
            cell.getChildren().add(courseDots);
        }

        cell.setOnMouseClicked(event -> {
            selectedDate = date;
            renderMonth();
            renderSelectedDate();
        });
        return cell;
    }

    private void renderSelectedDate() {
        selectedDateLabel.setText(I18n.date(selectedDate, "date.selected.pattern"));
        List<Assignment> dueAssignments = assignmentsDueOn(selectedDate);
        long unfinished = dueAssignments.stream()
                .filter(assignment -> !assignment.isCompleted()).count();
        selectedDateCountLabel.setText(I18n.text(
                unfinished == 0 ? "assignments.selected.count.none"
                        : unfinished == 1 ? "assignments.selected.count.one"
                        : "assignments.selected.count.many",
                unfinished));
        selectedAssignmentsContainer.getChildren().clear();

        if (dueAssignments.isEmpty()) {
            VBox empty = new VBox(7);
            empty.setAlignment(Pos.CENTER);
            empty.getStyleClass().add("assignment-date-empty");
            Label title = new Label(I18n.text("assignments.empty.title"));
            title.getStyleClass().add("empty-state-title");
            Label copy = new Label(I18n.text("assignments.empty.copy"));
            copy.setWrapText(true);
            copy.getStyleClass().add("empty-state-copy");
            empty.getChildren().addAll(title, copy);
            selectedAssignmentsContainer.getChildren().add(empty);
            return;
        }

        for (Assignment assignment : dueAssignments) {
            selectedAssignmentsContainer.getChildren().add(
                    createAssignmentRow(assignment)
            );
        }
    }

    private HBox createAssignmentRow(Assignment assignment) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("assignment-date-task");
        if (assignment.isCompleted()) {
            row.getStyleClass().add("assignment-date-task-completed");
        }

        CheckBox complete = new CheckBox();
        complete.getStyleClass().add("task-check-box");
        complete.setSelected(assignment.isCompleted());
        complete.setAccessibleText(I18n.text(
                "assignments.markComplete", assignment.getTitle()));
        complete.setOnAction(event -> {
            try {
                repository.updateAssignmentStatus(
                        assignment.getId(), complete.isSelected()
                                ? AssignmentStatus.COMPLETED
                                : AssignmentStatus.NOT_STARTED
                );
                refreshAssignments();
            } catch (DataAccessException exception) {
                complete.setSelected(!complete.isSelected());
                showError(I18n.text("assignments.error.update"), exception.getMessage());
            }
        });

        Course course = coursesById.get(assignment.getCourseId());
        Region courseAccent = new Region();
        courseAccent.getStyleClass().add("assignment-course-accent");
        courseAccent.getStyleClass().add(course == null
                ? "course-accent-blue" : courseAccentClass(course.getColor()));

        VBox details = new VBox(4);
        HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);
        Label title = new Label(assignment.getTitle());
        title.setWrapText(true);
        title.getStyleClass().add("assignment-date-task-title");
        String courseLabel = course == null
                ? I18n.text("common.course")
                : course.getCode() + " · " + course.getName();
        Label courseName = new Label(courseLabel);
        courseName.setWrapText(true);
        courseName.setMaxWidth(Double.MAX_VALUE);
        courseName.getStyleClass().add("assignment-date-task-course");
        Label deadline = new Label(I18n.text(
                "assignments.deadline", settings.formatTime(assignment.getDueTime())));
        deadline.getStyleClass().add("assignment-date-task-meta");
        if (selectedDate.isBefore(LocalDate.now())) {
            deadline.getStyleClass().add("task-meta-overdue");
        }
        details.getChildren().addAll(title, courseName, deadline);

        Label priority = new Label(
                assignment.getPriority().getDisplayName().toUpperCase(I18n.locale())
        );
        priority.getStyleClass().add(assignment.getPriority().getCssClass());
        priority.setMinWidth(Region.USE_PREF_SIZE);
        priority.setMaxWidth(Region.USE_PREF_SIZE);
        row.getChildren().addAll(complete, courseAccent, details, priority);

        if (!assignment.getDescription().isBlank()) {
            Tooltip.install(row, new Tooltip(assignment.getDescription()));
        }
        return row;
    }

    private String courseAccentClass(String color) {
        return CourseColor.fromStorage(color).accentStyleClass();
    }

    private List<Assignment> assignmentsDueOn(LocalDate date) {
        return visibleAssignments.stream()
                .filter(assignment -> assignment.getDueDate().equals(date))
                .sorted(Comparator.comparing(Assignment::getDueTime)
                        .thenComparing(Assignment::getId))
                .toList();
    }

    private Window getWindow() {
        return monthGrid.getScene() == null
                ? null : monthGrid.getScene().getWindow();
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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
