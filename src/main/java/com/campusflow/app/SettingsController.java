package com.campusflow.app;

import com.campusflow.app.data.CampusFlowRepository;
import com.campusflow.app.data.DataAccessException;
import com.campusflow.app.i18n.AppLanguage;
import com.campusflow.app.i18n.I18n;
import com.campusflow.app.model.AppSettings;
import com.campusflow.app.model.Assignment;
import com.campusflow.app.model.Course;
import com.campusflow.app.model.Priority;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SettingsController {
    @FXML private TextField displayNameField;
    @FXML private TextField semesterNameField;
    @FXML private DatePicker semesterStartPicker;
    @FXML private DatePicker semesterEndPicker;
    @FXML private ComboBox<DayOfWeek> weekStartBox;
    @FXML private ComboBox<String> timeFormatBox;
    @FXML private ComboBox<LocalTime> calendarStartBox;
    @FXML private ComboBox<LocalTime> calendarEndBox;
    @FXML private ComboBox<Integer> timeIntervalBox;
    @FXML private ComboBox<LocalTime> defaultDeadlineBox;
    @FXML private ComboBox<Priority> defaultPriorityBox;
    @FXML private CheckBox showCompletedCheckBox;
    @FXML private ComboBox<AppLanguage> languageBox;
    @FXML private ComboBox<AppSettings.Theme> themeBox;
    @FXML private ComboBox<AppSettings.AccentColor> accentColorBox;
    @FXML private ComboBox<AppSettings.Density> densityBox;
    @FXML private CheckBox oneDayReminderCheckBox;
    @FXML private CheckBox oneHourReminderCheckBox;
    @FXML private CheckBox overdueReminderCheckBox;
    @FXML private Label databasePathLabel;
    @FXML private Label saveStatusLabel;

    private CampusFlowRepository repository;
    private Consumer<AppSettings> settingsSaved = settings -> { };
    private Runnable dataChanged = () -> { };

    @FXML
    private void initialize() {
        weekStartBox.setItems(FXCollections.observableArrayList(
                DayOfWeek.MONDAY, DayOfWeek.SUNDAY
        ));
        weekStartBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DayOfWeek day) {
                if (day == null) {
                    return "";
                }
                return day.getDisplayName(java.time.format.TextStyle.FULL, I18n.locale());
            }

            @Override
            public DayOfWeek fromString(String text) {
                for (DayOfWeek day : DayOfWeek.values()) {
                    if (toString(day).equalsIgnoreCase(text)) {
                        return day;
                    }
                }
                return null;
            }
        });
        timeFormatBox.setItems(FXCollections.observableArrayList(
                I18n.text("settings.timeFormat.12"),
                I18n.text("settings.timeFormat.24")
        ));
        timeIntervalBox.setItems(FXCollections.observableArrayList(5, 10, 15, 30));
        timeIntervalBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer minutes) {
                return minutes == null ? "" : I18n.text("settings.minutes", minutes);
            }

            @Override
            public Integer fromString(String text) {
                return Integer.parseInt(text.replaceAll("\\D", ""));
            }
        });
        defaultPriorityBox.setItems(FXCollections.observableArrayList(Priority.values()));
        languageBox.setItems(FXCollections.observableArrayList(AppLanguage.values()));
        themeBox.setItems(FXCollections.observableArrayList(AppSettings.Theme.values()));
        accentColorBox.setItems(FXCollections.observableArrayList(
                AppSettings.AccentColor.values()
        ));
        densityBox.setItems(FXCollections.observableArrayList(AppSettings.Density.values()));

        List<LocalTime> displayHours = new ArrayList<>();
        for (int hour = 5; hour <= 23; hour++) {
            displayHours.add(LocalTime.of(hour, 0));
        }
        calendarStartBox.setItems(FXCollections.observableArrayList(displayHours));
        calendarEndBox.setItems(FXCollections.observableArrayList(displayHours));
        configureTimeConverter(calendarStartBox);
        configureTimeConverter(calendarEndBox);
        configureTimeConverter(defaultDeadlineBox);

        timeIntervalBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                rebuildDeadlineChoices(newValue);
            }
        });
    }

    public void configure(CampusFlowRepository repository,
                          Consumer<AppSettings> settingsSaved,
                          Runnable dataChanged) {
        this.repository = repository;
        this.settingsSaved = settingsSaved;
        this.dataChanged = dataChanged;
        databasePathLabel.setText(repository.getDatabaseManager()
                .getDatabaseFile().toAbsolutePath().toString());
        loadSettings();
    }

    public void loadSettings() {
        if (repository == null) {
            return;
        }
        AppSettings settings = repository.findSettings();
        displayNameField.setText(settings.getDisplayName());
        semesterNameField.setText(settings.getSemesterName());
        semesterStartPicker.setValue(settings.getSemesterStart());
        semesterEndPicker.setValue(settings.getSemesterEnd());
        weekStartBox.setValue(settings.getWeekStartsOn());
        timeFormatBox.setValue(settings.isUse24HourTime()
                ? I18n.text("settings.timeFormat.24")
                : I18n.text("settings.timeFormat.12"));
        calendarStartBox.setValue(settings.getCalendarStartTime());
        calendarEndBox.setValue(settings.getCalendarEndTime());
        timeIntervalBox.setValue(settings.getTimeIntervalMinutes());
        rebuildDeadlineChoices(settings.getTimeIntervalMinutes());
        defaultDeadlineBox.setValue(settings.getDefaultDeadlineTime());
        defaultPriorityBox.setValue(settings.getDefaultPriority());
        showCompletedCheckBox.setSelected(settings.isShowCompletedAssignments());
        languageBox.setValue(settings.getLanguage());
        themeBox.setValue(settings.getTheme());
        accentColorBox.setValue(settings.getAccentColor());
        densityBox.setValue(settings.getDensity());
        oneDayReminderCheckBox.setSelected(settings.isRemindOneDayBefore());
        oneHourReminderCheckBox.setSelected(settings.isRemindOneHourBefore());
        overdueReminderCheckBox.setSelected(settings.isRemindOverdue());
        setStatus(I18n.text("settings.saved"), false);
    }

    @FXML
    private void onSaveSettings() {
        String validationError = validateSettings();
        if (validationError != null) {
            setStatus(validationError, true);
            return;
        }

        AppSettings settings = new AppSettings(
                displayNameField.getText().trim(),
                semesterNameField.getText().trim(),
                semesterStartPicker.getValue(),
                semesterEndPicker.getValue(),
                weekStartBox.getValue(),
                timeFormatBox.getValue().equals(I18n.text("settings.timeFormat.24")),
                calendarStartBox.getValue(),
                calendarEndBox.getValue(),
                timeIntervalBox.getValue(),
                defaultDeadlineBox.getValue(),
                defaultPriorityBox.getValue(),
                showCompletedCheckBox.isSelected(),
                themeBox.getValue(),
                accentColorBox.getValue(),
                densityBox.getValue(),
                oneDayReminderCheckBox.isSelected(),
                oneHourReminderCheckBox.isSelected(),
                overdueReminderCheckBox.isSelected(),
                languageBox.getValue()
        );
        try {
            repository.saveSettings(settings);
            settingsSaved.accept(settings);
            setStatus(I18n.text("settings.status.allSaved"), false);
        } catch (DataAccessException exception) {
            setStatus(exception.getMessage(), true);
        }
    }

    @FXML
    private void onCopyDatabasePath() {
        ClipboardContent content = new ClipboardContent();
        content.putString(databasePathLabel.getText());
        Clipboard.getSystemClipboard().setContent(content);
        setStatus(I18n.text("settings.status.pathCopied"), false);
    }

    @FXML
    private void onExportData() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18n.text("settings.dialog.export"));
        java.io.File directory = chooser.showDialog(getWindow());
        if (directory == null) {
            return;
        }

        try {
            Path folder = directory.toPath();
            Files.writeString(folder.resolve("campusflow-courses.csv"),
                    coursesCsv(repository.findAllCourses()), StandardCharsets.UTF_8);
            Files.writeString(folder.resolve("campusflow-assignments.csv"),
                    assignmentsCsv(repository.findAllAssignments(), repository.findAllCourses()),
                    StandardCharsets.UTF_8);
            setStatus(I18n.text("settings.status.exported"), false);
        } catch (IOException | DataAccessException exception) {
            showError(I18n.text("settings.error.export"), exception.getMessage());
        }
    }

    @FXML
    private void onBackupDatabase() {
        FileChooser chooser = databaseFileChooser(I18n.text("settings.dialog.backupSave"));
        chooser.setInitialFileName("campusflow-backup.db");
        java.io.File destination = chooser.showSaveDialog(getWindow());
        if (destination == null) {
            return;
        }
        try {
            repository.getDatabaseManager().backupTo(destination.toPath());
            setStatus(I18n.text("settings.status.backupCreated"), false);
        } catch (DataAccessException exception) {
            showError(I18n.text("settings.error.backup"), exception.getMessage());
        }
    }

    @FXML
    private void onRestoreDatabase() {
        FileChooser chooser = databaseFileChooser(I18n.text("settings.dialog.backupOpen"));
        java.io.File source = chooser.showOpenDialog(getWindow());
        if (source == null || !confirm(
                I18n.text("settings.dialog.restore.header"),
                I18n.text("settings.dialog.restore.content")
        )) {
            return;
        }
        try {
            repository.getDatabaseManager().restoreFrom(source.toPath());
            loadSettings();
            AppSettings restored = repository.findSettings();
            settingsSaved.accept(restored);
            dataChanged.run();
            setStatus(I18n.text("settings.status.backupRestored"), false);
        } catch (DataAccessException exception) {
            showError(I18n.text("settings.error.restore"), exception.getMessage());
        }
    }

    @FXML
    private void onClearData() {
        if (!confirm(
                I18n.text("settings.dialog.clear.header"),
                I18n.text("settings.dialog.clear.content")
        ) || !confirm(
                I18n.text("settings.dialog.clearAgain.header"),
                I18n.text("settings.dialog.clearAgain.content")
        )) {
            return;
        }
        try {
            repository.clearPlannerData();
            dataChanged.run();
            setStatus(I18n.text("settings.status.dataCleared"), false);
        } catch (DataAccessException exception) {
            showError(I18n.text("settings.error.clear"), exception.getMessage());
        }
    }

    private String validateSettings() {
        if (displayNameField.getText().isBlank()
                || semesterNameField.getText().isBlank()) {
            return I18n.text("settings.validation.names");
        }
        if (semesterStartPicker.getValue() == null
                || semesterEndPicker.getValue() == null
                || semesterEndPicker.getValue().isBefore(semesterStartPicker.getValue())) {
            return I18n.text("settings.validation.dates");
        }
        if (weekStartBox.getValue() == null || timeFormatBox.getValue() == null
                || calendarStartBox.getValue() == null
                || calendarEndBox.getValue() == null
                || timeIntervalBox.getValue() == null
                || defaultDeadlineBox.getValue() == null
                || defaultPriorityBox.getValue() == null
                || languageBox.getValue() == null
                || themeBox.getValue() == null
                || accentColorBox.getValue() == null
                || densityBox.getValue() == null) {
            return I18n.text("settings.validation.complete");
        }
        if (!calendarEndBox.getValue().isAfter(calendarStartBox.getValue())) {
            return I18n.text("settings.validation.calendarTime");
        }
        return null;
    }

    private void rebuildDeadlineChoices(int interval) {
        LocalTime selected = defaultDeadlineBox.getValue();
        List<LocalTime> times = new ArrayList<>();
        for (int minutes = 0; minutes < 24 * 60; minutes += interval) {
            times.add(LocalTime.MIDNIGHT.plusMinutes(minutes));
        }
        defaultDeadlineBox.setItems(FXCollections.observableArrayList(times));
        if (selected != null && times.contains(selected)) {
            defaultDeadlineBox.setValue(selected);
        } else if (!times.isEmpty()) {
            defaultDeadlineBox.setValue(times.getLast());
        }
    }

    private void configureTimeConverter(ComboBox<LocalTime> comboBox) {
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalTime time) {
                return time == null ? "" : time.format(timeFormatter());
            }

            @Override
            public LocalTime fromString(String text) {
                return LocalTime.parse(text, timeFormatter());
            }
        });
    }

    private DateTimeFormatter timeFormatter() {
        String pattern = I18n.language() == AppLanguage.SIMPLIFIED_CHINESE
                ? "a h:mm" : "h:mm a";
        return DateTimeFormatter.ofPattern(pattern, I18n.locale());
    }

    private FileChooser databaseFileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        I18n.text("settings.dialog.databaseFilter"), "*.db")
        );
        return chooser;
    }

    private boolean confirm(String header, String content) {
        ButtonType continueButton = new ButtonType(
                I18n.text("common.continue"), ButtonBar.ButtonData.OK_DONE
        );
        ButtonType cancelButton = new ButtonType(
                I18n.text("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE
        );
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION, content,
                continueButton, cancelButton
        );
        alert.setTitle("CampusFlow");
        alert.setHeaderText(header);
        if (getWindow() != null) {
            alert.initOwner(getWindow());
        }
        return alert.showAndWait().orElse(cancelButton) == continueButton;
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("CampusFlow");
        alert.setHeaderText(header);
        alert.setContentText(content);
        if (getWindow() != null) {
            alert.initOwner(getWindow());
        }
        alert.showAndWait();
    }

    private void setStatus(String text, boolean error) {
        saveStatusLabel.setText(text);
        saveStatusLabel.getStyleClass().removeAll(
                "settings-status-success", "settings-status-error"
        );
        saveStatusLabel.getStyleClass().add(error
                ? "settings-status-error" : "settings-status-success");
    }

    private String coursesCsv(List<Course> courses) {
        StringBuilder csv = new StringBuilder(
                "id,code,name,instructor,location,meeting_days,start_time,end_time,color\n"
        );
        for (Course course : courses) {
            csv.append(course.getId()).append(',')
                    .append(csv(course.getCode())).append(',')
                    .append(csv(course.getName())).append(',')
                    .append(csv(course.getInstructor())).append(',')
                    .append(csv(course.getLocation())).append(',')
                    .append(csv(course.getMeetingDays())).append(',')
                    .append(course.getStartTime()).append(',')
                    .append(course.getEndTime()).append(',')
                    .append(csv(course.getColor())).append('\n');
        }
        return csv.toString();
    }

    private String assignmentsCsv(List<Assignment> assignments, List<Course> courses) {
        Map<Integer, Course> coursesById = new HashMap<>();
        for (Course course : courses) {
            coursesById.put(course.getId(), course);
        }
        StringBuilder csv = new StringBuilder(
                "id,course_code,course_name,title,description,due_date,due_time,priority,status\n"
        );
        for (Assignment assignment : assignments) {
            Course course = coursesById.get(assignment.getCourseId());
            csv.append(assignment.getId()).append(',')
                    .append(csv(course == null ? "" : course.getCode())).append(',')
                    .append(csv(course == null ? "" : course.getName())).append(',')
                    .append(csv(assignment.getTitle())).append(',')
                    .append(csv(assignment.getDescription())).append(',')
                    .append(assignment.getDueDate()).append(',')
                    .append(assignment.getDueTime()).append(',')
                    .append(assignment.getPriority()).append(',')
                    .append(assignment.getStatus()).append('\n');
        }
        return csv.toString();
    }

    private String csv(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private Window getWindow() {
        return displayNameField.getScene() == null
                ? null : displayNameField.getScene().getWindow();
    }
}
