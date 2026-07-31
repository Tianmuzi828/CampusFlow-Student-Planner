package com.campusflow.app.ui;

import com.campusflow.app.CampusFlowApplication;
import com.campusflow.app.i18n.I18n;
import com.campusflow.app.model.Assignment;
import com.campusflow.app.model.AssignmentStatus;
import com.campusflow.app.model.AppSettings;
import com.campusflow.app.model.Course;
import com.campusflow.app.model.CourseColor;
import com.campusflow.app.model.Priority;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public final class FormDialogs {
    private FormDialogs() {
    }

    public static Optional<Course> showCourseDialog(Window owner) {
        return showCourseDialog(owner, AppSettings.defaults());
    }

    public static Optional<Course> showCourseDialog(Window owner,
                                                    AppSettings settings) {
        return showCourseEditor(owner, null, settings);
    }

    public static Optional<Course> showEditCourseDialog(Window owner,
                                                        Course course) {
        return showEditCourseDialog(owner, course, AppSettings.defaults());
    }

    public static Optional<Course> showEditCourseDialog(Window owner,
                                                        Course course,
                                                        AppSettings settings) {
        return showCourseEditor(owner, course, settings);
    }

    private static Optional<Course> showCourseEditor(Window owner,
                                                     Course existingCourse,
                                                     AppSettings settings) {
        boolean editing = existingCourse != null;
        Dialog<Course> dialog = new Dialog<>();
        configureDialog(
                dialog,
                owner,
                I18n.text(editing ? "form.course.edit.title" : "form.course.add.title"),
                editing
                        ? I18n.text("form.course.edit.header")
                        : I18n.text("form.course.add.header")
        );

        ButtonType saveButtonType = new ButtonType(
                I18n.text(editing ? "form.course.saveChanges" : "form.course.save"),
                ButtonBar.ButtonData.OK_DONE
        );
        ButtonType cancelButtonType = new ButtonType(
                I18n.text("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE
        );
        dialog.getDialogPane().getButtonTypes().addAll(
                saveButtonType, cancelButtonType
        );

        TextField codeField = textField(I18n.text("form.course.code.prompt"));
        TextField nameField = textField(I18n.text("form.course.name.prompt"));
        TextField instructorField = textField(I18n.text("form.course.instructor.prompt"));
        TextField locationField = textField(I18n.text("form.course.location.prompt"));
        if (editing) {
            codeField.setText(existingCourse.getCode());
            nameField.setText(existingCourse.getName());
            instructorField.setText(existingCourse.getInstructor());
            locationField.setText(existingCourse.getLocation());
        }

        HBox dayButtonsBox = new HBox(6);
        dayButtonsBox.setAlignment(Pos.CENTER_LEFT);
        List<ToggleButton> dayButtons = new ArrayList<>();
        String[][] dayChoices = {
                {I18n.text("day.short.mon"), "MON"},
                {I18n.text("day.short.tue"), "TUE"},
                {I18n.text("day.short.wed"), "WED"},
                {I18n.text("day.short.thu"), "THU"},
                {I18n.text("day.short.fri"), "FRI"},
                {I18n.text("day.short.sat"), "SAT"},
                {I18n.text("day.short.sun"), "SUN"}
        };
        for (String[] dayChoice : dayChoices) {
            ToggleButton button = new ToggleButton(dayChoice[0]);
            button.setUserData(dayChoice[1]);
            button.getStyleClass().add("day-toggle");
            button.setPrefWidth(42);
            dayButtons.add(button);
            dayButtonsBox.getChildren().add(button);
        }
        if (editing) {
            for (ToggleButton button : dayButtons) {
                button.setSelected(existingCourse.getMeetingDays()
                        .contains(button.getUserData().toString()));
            }
        } else {
            dayButtons.get(0).setSelected(true);
            dayButtons.get(2).setSelected(true);
        }

        TimeWheelPicker startTimePicker = new TimeWheelPicker(
                editing ? existingCourse.getStartTime() : LocalTime.of(10, 0),
                settings.isUse24HourTime(),
                settings.getTimeIntervalMinutes()
        );
        TimeWheelPicker endTimePicker = new TimeWheelPicker(
                editing ? existingCourse.getEndTime() : LocalTime.of(11, 0),
                settings.isUse24HourTime(),
                settings.getTimeIntervalMinutes()
        );

        ComboBox<CourseColor> colorBox = new ComboBox<>(
                FXCollections.observableArrayList(CourseColor.values())
        );
        colorBox.setValue(editing
                ? CourseColor.fromStorage(existingCourse.getColor())
                : CourseColor.BLUE);
        colorBox.getStyleClass().add("input-control");
        colorBox.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        GridPane form = createFormGrid();
        addFormRow(form, 0, I18n.text("form.course.code"), codeField);
        addFormRow(form, 1, I18n.text("form.course.name"), nameField);
        addFormRow(form, 2, I18n.text("form.course.instructor"), instructorField);
        addFormRow(form, 3, I18n.text("form.course.location"), locationField);
        addFormRow(form, 4, I18n.text("form.course.days"), dayButtonsBox);

        Label timeSeparator = new Label(I18n.text("form.course.to"));
        timeSeparator.getStyleClass().add("time-range-separator");
        HBox timeRow = new HBox(
                10, startTimePicker, timeSeparator, endTimePicker
        );
        timeRow.setAlignment(Pos.CENTER_LEFT);
        addFormRow(form, 5, I18n.text("form.course.time"), timeRow);
        addFormRow(form, 6, I18n.text("form.course.color"), colorBox);
        form.add(errorLabel, 0, 7, 2, 1);
        dialog.getDialogPane().setContent(form);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String validationError = validateCourse(
                    codeField, nameField, dayButtons,
                    startTimePicker, endTimePicker
            );
            if (validationError != null) {
                errorLabel.setText(validationError);
                errorLabel.setManaged(true);
                errorLabel.setVisible(true);
                event.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button != saveButtonType) {
                return null;
            }
            String meetingDays = dayButtons.stream()
                    .filter(ToggleButton::isSelected)
                    .map(toggle -> toggle.getUserData().toString())
                    .collect(Collectors.joining(","));
            return new Course(
                    editing ? existingCourse.getId() : 0,
                    codeField.getText().trim().toUpperCase(Locale.ENGLISH),
                    nameField.getText().trim(),
                    instructorField.getText().trim(),
                    locationField.getText().trim(),
                    meetingDays,
                    startTimePicker.getValue(),
                    endTimePicker.getValue(),
                    colorBox.getValue().storageValue()
            );
        });

        return dialog.showAndWait();
    }

    public static Optional<Assignment> showAssignmentDialog(
            Window owner, List<Course> courses) {
        return showAssignmentDialog(owner, courses, AppSettings.defaults());
    }

    public static Optional<Assignment> showAssignmentDialog(
            Window owner, List<Course> courses, AppSettings settings) {
        Dialog<Assignment> dialog = new Dialog<>();
        configureDialog(dialog, owner, I18n.text("form.assignment.title"),
                I18n.text("form.assignment.header"));

        ButtonType saveButtonType = new ButtonType(
                I18n.text("form.assignment.save"), ButtonBar.ButtonData.OK_DONE
        );
        ButtonType cancelButtonType = new ButtonType(
                I18n.text("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE
        );
        dialog.getDialogPane().getButtonTypes().addAll(
                saveButtonType, cancelButtonType
        );

        TextField titleField = textField(I18n.text("form.assignment.name.prompt"));
        ComboBox<Course> courseBox = new ComboBox<>(
                FXCollections.observableArrayList(courses)
        );
        courseBox.setValue(courses.getFirst());
        courseBox.getStyleClass().add("input-control");
        courseBox.setMaxWidth(Double.MAX_VALUE);

        DatePicker dueDatePicker = new DatePicker(LocalDate.now().plusDays(1));
        dueDatePicker.getStyleClass().add("input-control");
        dueDatePicker.setMaxWidth(Double.MAX_VALUE);

        TimeWheelPicker dueTimePicker = new TimeWheelPicker(
                settings.getDefaultDeadlineTime(),
                settings.isUse24HourTime(),
                settings.getTimeIntervalMinutes()
        );

        ComboBox<Priority> priorityBox = new ComboBox<>(
                FXCollections.observableArrayList(Priority.values())
        );
        priorityBox.setValue(settings.getDefaultPriority());
        priorityBox.getStyleClass().add("input-control");
        priorityBox.setMaxWidth(Double.MAX_VALUE);

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText(I18n.text("form.assignment.description.prompt"));
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        descriptionArea.getStyleClass().add("input-control");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        GridPane form = createFormGrid();
        addFormRow(form, 0, I18n.text("form.assignment.name"), titleField);
        addFormRow(form, 1, I18n.text("form.assignment.course"), courseBox);
        addFormRow(form, 2, I18n.text("form.assignment.date"), dueDatePicker);
        addFormRow(form, 3, I18n.text("form.assignment.time"), dueTimePicker);
        addFormRow(form, 4, I18n.text("form.assignment.priority"), priorityBox);
        addFormRow(form, 5, I18n.text("form.assignment.description"), descriptionArea);
        form.add(errorLabel, 0, 6, 2, 1);
        dialog.getDialogPane().setContent(form);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (titleField.getText().isBlank()) {
                errorLabel.setText(I18n.text("form.assignment.validation.title"));
                errorLabel.setManaged(true);
                errorLabel.setVisible(true);
                event.consume();
            } else if (courseBox.getValue() == null
                    || dueDatePicker.getValue() == null
                    || dueTimePicker.getValue() == null) {
                errorLabel.setText(I18n.text("form.assignment.validation.deadline"));
                errorLabel.setManaged(true);
                errorLabel.setVisible(true);
                event.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button != saveButtonType) {
                return null;
            }
            return new Assignment(
                    0,
                    courseBox.getValue().getId(),
                    titleField.getText().trim(),
                    descriptionArea.getText().trim(),
                    dueDatePicker.getValue(),
                    dueTimePicker.getValue(),
                    priorityBox.getValue(),
                    AssignmentStatus.NOT_STARTED
            );
        });

        return dialog.showAndWait();
    }

    private static void configureDialog(Dialog<?> dialog, Window owner,
                                        String title, String header) {
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.getDialogPane().getStyleClass().add("campus-dialog");
        dialog.getDialogPane().setPrefWidth(540);
        String stylesheet = CampusFlowApplication.class
                .getResource("styles.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(stylesheet);
    }

    private static GridPane createFormGrid() {
        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(12);
        form.setPadding(new Insets(6, 0, 2, 0));
        form.getStyleClass().add("form-grid");
        return form;
    }

    private static void addFormRow(GridPane form, int row, String labelText,
                                   Node input) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        form.add(label, 0, row);
        form.add(input, 1, row);
        GridPane.setFillWidth(input, true);
    }

    private static TextField textField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.getStyleClass().add("input-control");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    static List<LocalTime> deadlineTimes() {
        return deadlineTimes(5);
    }

    static List<LocalTime> deadlineTimes(int intervalMinutes) {
        List<LocalTime> times = new ArrayList<>();
        for (int minutes = 0; minutes < 24 * 60; minutes += intervalMinutes) {
            times.add(LocalTime.MIDNIGHT.plusMinutes(minutes));
        }
        return List.copyOf(times);
    }

    private static String validateCourse(TextField codeField,
                                         TextField nameField,
                                         List<ToggleButton> dayButtons,
                                         TimeWheelPicker startTimePicker,
                                         TimeWheelPicker endTimePicker) {
        if (codeField.getText().isBlank() || nameField.getText().isBlank()) {
            return I18n.text("form.course.validation.name");
        }
        if (dayButtons.stream().noneMatch(ToggleButton::isSelected)) {
            return I18n.text("form.course.validation.days");
        }
        if (!endTimePicker.getValue().isAfter(startTimePicker.getValue())) {
            return I18n.text("form.course.validation.time");
        }
        return null;
    }

}
