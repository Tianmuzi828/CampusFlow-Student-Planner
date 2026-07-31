package com.campusflow.app;

import com.campusflow.app.data.CampusFlowRepository;
import com.campusflow.app.data.DataAccessException;
import com.campusflow.app.i18n.I18n;
import com.campusflow.app.model.AppSettings;
import com.campusflow.app.model.Course;
import com.campusflow.app.model.CourseColor;
import com.campusflow.app.ui.FormDialogs;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CoursesController {
    @FXML private Label courseCountLabel;
    @FXML private VBox courseListContainer;

    private CampusFlowRepository repository;
    private AppSettings settings = AppSettings.defaults();

    public void setRepository(CampusFlowRepository repository) {
        this.repository = repository;
        refreshCourses();
    }

    public void refreshCourses() {
        if (repository == null) {
            return;
        }
        try {
            settings = repository.findSettings();
            List<Course> courses = repository.findAllCourses();
            courseCountLabel.setText(I18n.text(courses.size() == 1
                    ? "courses.count.one" : "courses.count.many", courses.size()));
            courseListContainer.getChildren().clear();

            if (courses.isEmpty()) {
                courseListContainer.getChildren().add(createEmptyState());
                return;
            }
            for (Course course : courses) {
                courseListContainer.getChildren().add(createCourseCard(course));
            }
        } catch (DataAccessException exception) {
            showError(exception);
        }
    }

    @FXML
    private void onAddCourse() {
        FormDialogs.showCourseDialog(getWindow(), repository.findSettings())
                .ifPresent(course -> {
            try {
                repository.addCourse(course);
                refreshCourses();
            } catch (DataAccessException exception) {
                showError(exception);
            }
        });
    }

    private HBox createCourseCard(Course course) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("course-management-card");

        Region accent = new Region();
        accent.setMinWidth(5);
        accent.setPrefSize(5, 74);
        accent.getStyleClass().add(courseAccentClass(course.getColor()));

        VBox mainDetails = new VBox(5);
        HBox.setHgrow(mainDetails, Priority.ALWAYS);
        Label name = new Label(course.getCode() + " — " + course.getName());
        name.getStyleClass().add("course-management-name");
        Label schedule = new Label(
                formatMeetingDays(course.getMeetingDays()) + " · "
                        + settings.formatTime(course.getStartTime()) + " – "
                        + settings.formatTime(course.getEndTime())
        );
        schedule.getStyleClass().add("course-management-schedule");

        String details = course.getLocation().isBlank()
                ? I18n.text("courses.noLocation") : course.getLocation();
        if (!course.getInstructor().isBlank()) {
            details += " · " + course.getInstructor();
        }
        Label meta = new Label(details);
        meta.getStyleClass().add("course-management-meta");
        mainDetails.getChildren().addAll(name, schedule, meta);

        Button editButton = new Button(I18n.text("common.edit"));
        editButton.getStyleClass().add("course-edit-button");
        editButton.setOnAction(event -> editCourse(course));
        card.getChildren().addAll(accent, mainDetails, editButton);
        return card;
    }

    private VBox createEmptyState() {
        VBox empty = new VBox(8);
        empty.setAlignment(Pos.CENTER);
        empty.getStyleClass().add("course-management-empty");
        Label title = new Label(I18n.text("courses.empty.title"));
        title.getStyleClass().add("empty-state-title");
        Label copy = new Label(I18n.text("courses.empty.copy"));
        copy.getStyleClass().add("empty-state-copy");
        Button addButton = new Button(I18n.text("common.addCourse"));
        addButton.getStyleClass().add("empty-state-button");
        addButton.setOnAction(event -> onAddCourse());
        empty.getChildren().addAll(title, copy, addButton);
        return empty;
    }

    private void editCourse(Course course) {
        FormDialogs.showEditCourseDialog(
                getWindow(), course, repository.findSettings()
        )
                .ifPresent(updatedCourse -> {
                    try {
                        repository.updateCourse(updatedCourse);
                        refreshCourses();
                    } catch (DataAccessException exception) {
                        showError(exception);
                    }
                });
    }

    private String formatMeetingDays(String meetingDays) {
        return Arrays.stream(meetingDays.split(","))
                .map(this::parseMeetingDay)
                .map(day -> day.getDisplayName(TextStyle.SHORT, I18n.locale()))
                .collect(Collectors.joining(" · "));
    }

    private DayOfWeek parseMeetingDay(String value) {
        return switch (value.trim()) {
            case "MON" -> DayOfWeek.MONDAY;
            case "TUE" -> DayOfWeek.TUESDAY;
            case "WED" -> DayOfWeek.WEDNESDAY;
            case "THU" -> DayOfWeek.THURSDAY;
            case "FRI" -> DayOfWeek.FRIDAY;
            case "SAT" -> DayOfWeek.SATURDAY;
            case "SUN" -> DayOfWeek.SUNDAY;
            default -> DayOfWeek.MONDAY;
        };
    }

    private String courseAccentClass(String color) {
        return CourseColor.fromStorage(color).accentStyleClass();
    }

    private Window getWindow() {
        return courseListContainer.getScene() == null
                ? null : courseListContainer.getScene().getWindow();
    }

    private void showError(DataAccessException exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("CampusFlow");
        alert.setHeaderText(I18n.text("courses.error.save"));
        alert.setContentText(exception.getMessage());
        Window owner = getWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }
}
