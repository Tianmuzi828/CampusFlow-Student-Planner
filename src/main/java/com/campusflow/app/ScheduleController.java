package com.campusflow.app;

import com.campusflow.app.data.CampusFlowRepository;
import com.campusflow.app.data.DataAccessException;
import com.campusflow.app.i18n.AppLanguage;
import com.campusflow.app.i18n.I18n;
import com.campusflow.app.model.AppSettings;
import com.campusflow.app.model.Course;
import com.campusflow.app.model.CourseColor;
import com.campusflow.app.service.ScheduleTimeScale;
import com.campusflow.app.ui.FormDialogs;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

public class ScheduleController {
    @FXML private Label weekRangeLabel;
    @FXML private Label courseCountLabel;
    @FXML private Label precisionHintLabel;
    @FXML private GridPane dayHeaderGrid;
    @FXML private GridPane calendarGrid;
    @FXML private ScrollPane calendarScrollPane;

    private CampusFlowRepository repository;
    private LocalDate weekStart;
    private AppSettings settings = AppSettings.defaults();

    @FXML
    private void initialize() {
        weekStart = startOfWeek(LocalDate.now());
        configureColumns(dayHeaderGrid);
        configureColumns(calendarGrid);
        calendarGrid.widthProperty().addListener(
                (observable, oldWidth, newWidth) ->
                        synchronizeHeaderWidth(newWidth.doubleValue())
        );
        calendarScrollPane.setVvalue(0.07);
    }

    private void synchronizeHeaderWidth(double gridWidth) {
        if (gridWidth <= 0) {
            return;
        }
        double contentWidth = Math.max(dayHeaderGrid.getMinWidth(), gridWidth);
        dayHeaderGrid.setPrefWidth(contentWidth);
        dayHeaderGrid.setMaxWidth(contentWidth);
    }

    public void setRepository(CampusFlowRepository repository) {
        this.repository = repository;
        settings = repository.findSettings();
        weekStart = startOfWeek(LocalDate.now());
        renderWeek();
    }

    public void refreshSchedule() {
        if (repository != null) {
            LocalDate anchorDate = weekStart == null ? LocalDate.now() : weekStart;
            settings = repository.findSettings();
            weekStart = startOfWeek(anchorDate);
        }
        renderWeek();
    }

    @FXML
    private void onPreviousWeek() {
        weekStart = weekStart.minusWeeks(1);
        renderWeek();
    }

    @FXML
    private void onThisWeek() {
        weekStart = startOfWeek(LocalDate.now());
        renderWeek();
    }

    @FXML
    private void onNextWeek() {
        weekStart = weekStart.plusWeeks(1);
        renderWeek();
    }

    private void renderWeek() {
        if (repository == null) {
            return;
        }

        try {
            List<Course> courses = repository.findAllCourses();
            renderHeader();
            renderTimeGrid();
            renderCourses(courses);
            precisionHintLabel.setText(I18n.text(
                    "schedule.precision", settings.getTimeIntervalMinutes()));
            courseCountLabel.setText(I18n.text(courses.size() == 1
                    ? "schedule.count.one" : "schedule.count.many", courses.size()));
        } catch (DataAccessException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("CampusFlow");
            alert.setHeaderText(I18n.text("schedule.error.load"));
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }

    private void renderHeader() {
        dayHeaderGrid.getChildren().clear();
        LocalDate weekEnd = weekStart.plusDays(6);
        String range;
        if (weekStart.getYear() == weekEnd.getYear()) {
            range = I18n.date(weekStart, "date.week.start.pattern") + " – "
                    + I18n.date(weekEnd, "date.week.end.pattern");
        } else {
            range = I18n.date(weekStart, "date.week.full.pattern") + " – "
                    + I18n.date(weekEnd, "date.week.full.pattern");
        }
        weekRangeLabel.setText(range);

        Label timeHeader = new Label(I18n.text("schedule.timeHeader"));
        timeHeader.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        timeHeader.setAlignment(Pos.CENTER);
        timeHeader.getStyleClass().add("calendar-time-header");
        dayHeaderGrid.add(timeHeader, 0, 0);

        LocalDate today = LocalDate.now();
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            LocalDate date = weekStart.plusDays(dayIndex);
            VBox header = new VBox(2);
            header.setAlignment(Pos.CENTER);
            header.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            header.getStyleClass().add("calendar-day-header");
            if (date.equals(today)) {
                header.getStyleClass().add("calendar-day-header-today");
            }

            Label dayName = new Label(date.getDayOfWeek().getDisplayName(
                    TextStyle.SHORT, I18n.locale()).toUpperCase(I18n.locale()));
            dayName.getStyleClass().add("calendar-day-name");
            Label dayNumber = new Label(Integer.toString(date.getDayOfMonth()));
            dayNumber.getStyleClass().add("calendar-day-number");
            header.getChildren().addAll(dayName, dayNumber);
            dayHeaderGrid.add(header, dayIndex + 1, 0);
        }
    }

    private void renderTimeGrid() {
        calendarGrid.getChildren().clear();
        calendarGrid.getRowConstraints().clear();
        LocalTime visibleStart = settings.getCalendarStartTime();
        LocalTime visibleEnd = settings.getCalendarEndTime();
        int slotMinutes = settings.getTimeIntervalMinutes();
        double slotHeight = 72.0 * slotMinutes / 60.0;
        int slotsPerHour = 60 / slotMinutes;
        for (int slot = 0; slot < ScheduleTimeScale.totalSlots(
                visibleStart, visibleEnd, slotMinutes); slot++) {
            RowConstraints row = new RowConstraints(
                    slotHeight, slotHeight, slotHeight
            );
            calendarGrid.getRowConstraints().add(row);
        }

        for (int hour = visibleStart.getHour(); hour < visibleEnd.getHour(); hour++) {
            int rowIndex = (hour - visibleStart.getHour()) * slotsPerHour;
            Label timeLabel = new Label(formatHour(hour));
            timeLabel.setMaxWidth(Double.MAX_VALUE);
            timeLabel.setAlignment(Pos.TOP_RIGHT);
            timeLabel.setPadding(new Insets(0, 9, 0, 0));
            timeLabel.getStyleClass().add("calendar-time-label");
            calendarGrid.add(timeLabel, 0, rowIndex, 1, slotsPerHour);
            GridPane.setValignment(timeLabel, VPos.TOP);

            for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
                Region hourCell = new Region();
                hourCell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                hourCell.getStyleClass().add("calendar-hour-cell");
                DayOfWeek displayedDay = weekStart.plusDays(dayIndex).getDayOfWeek();
                if (displayedDay == DayOfWeek.SATURDAY
                        || displayedDay == DayOfWeek.SUNDAY) {
                    hourCell.getStyleClass().add("calendar-weekend-cell");
                }
                calendarGrid.add(
                        hourCell, dayIndex + 1, rowIndex, 1, slotsPerHour
                );
                GridPane.setHgrow(hourCell, Priority.ALWAYS);
                GridPane.setVgrow(hourCell, Priority.ALWAYS);
            }
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        if (!today.isBefore(weekStart)
                && !today.isAfter(weekStart.plusDays(6))
                && ScheduleTimeScale.isVisible(
                        now, now.plusMinutes(1), visibleStart, visibleEnd)) {
            Region currentTimeLine = new Region();
            currentTimeLine.setMaxHeight(2);
            currentTimeLine.setMouseTransparent(true);
            currentTimeLine.getStyleClass().add("calendar-current-time-line");
            calendarGrid.add(
                    currentTimeLine, 1,
                    ScheduleTimeScale.slotFor(
                            now, visibleStart, visibleEnd, slotMinutes
                    ), 7, 1
            );
            GridPane.setHgrow(currentTimeLine, Priority.ALWAYS);
            GridPane.setHalignment(currentTimeLine, HPos.CENTER);
            GridPane.setValignment(currentTimeLine, VPos.CENTER);
        }
    }

    private void renderCourses(List<Course> courses) {
        int visibleOccurrences = 0;
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            DayOfWeek day = weekStart.plusDays(dayIndex).getDayOfWeek();
            for (Course course : courses) {
                if (!course.meetsOn(day)
                        || !ScheduleTimeScale.isVisible(
                                course.getStartTime(), course.getEndTime(),
                                settings.getCalendarStartTime(),
                                settings.getCalendarEndTime())) {
                    continue;
                }

                int startSlot = ScheduleTimeScale.slotFor(
                        course.getStartTime(),
                        settings.getCalendarStartTime(),
                        settings.getCalendarEndTime(),
                        settings.getTimeIntervalMinutes()
                );
                int span = ScheduleTimeScale.visibleSpan(
                        course.getStartTime(), course.getEndTime(),
                        settings.getCalendarStartTime(),
                        settings.getCalendarEndTime(),
                        settings.getTimeIntervalMinutes()
                );
                VBox courseCard = createCourseCard(course);
                calendarGrid.add(courseCard, dayIndex + 1, startSlot, 1, span);
                GridPane.setMargin(courseCard, new Insets(2, 4, 2, 4));
                GridPane.setHgrow(courseCard, Priority.ALWAYS);
                GridPane.setVgrow(courseCard, Priority.ALWAYS);
                visibleOccurrences++;
            }
        }

        if (visibleOccurrences == 0) {
            Label emptyLabel = new Label(
                    I18n.text("schedule.empty")
            );
            emptyLabel.setWrapText(true);
            emptyLabel.getStyleClass().add("calendar-empty-label");
            calendarGrid.add(emptyLabel, 1, 4, 7, 5);
            GridPane.setHalignment(emptyLabel, HPos.CENTER);
        }
    }

    private VBox createCourseCard(Course course) {
        VBox card = new VBox(2);
        card.setMinHeight(0);
        card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        card.getStyleClass().addAll(
                "calendar-course", calendarColorClass(course.getColor())
        );

        Label code = new Label(course.getCode());
        code.getStyleClass().add("calendar-course-code");
        card.getChildren().add(code);

        long duration = course.getDurationMinutes();
        if (duration >= 30) {
            Label name = new Label(course.getName());
            name.setWrapText(true);
            name.getStyleClass().add("calendar-course-name");
            card.getChildren().add(name);
        }
        if (duration >= 50) {
            String details = settings.formatTime(course.getStartTime()) + " – "
                    + settings.formatTime(course.getEndTime());
            if (!course.getLocation().isBlank()) {
                details += "\n" + course.getLocation();
            }
            Label meta = new Label(details);
            meta.getStyleClass().add("calendar-course-meta");
            card.getChildren().add(meta);
        }

        String tooltipText = course.getCode() + " — " + course.getName()
                + "\n" + settings.formatTime(course.getStartTime())
                + " – " + settings.formatTime(course.getEndTime())
                + (course.getLocation().isBlank()
                ? "" : "\n" + course.getLocation());
        Tooltip.install(card, new Tooltip(tooltipText));
        card.setOnMouseClicked(event -> editCourse(course));
        return card;
    }

    private void editCourse(Course course) {
        FormDialogs.showEditCourseDialog(
                calendarGrid.getScene() == null
                        ? null : calendarGrid.getScene().getWindow(),
                course,
                repository.findSettings()
        ).ifPresent(updatedCourse -> {
            try {
                repository.updateCourse(updatedCourse);
                renderWeek();
            } catch (DataAccessException exception) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("CampusFlow");
                alert.setHeaderText(I18n.text("schedule.error.update"));
                alert.setContentText(exception.getMessage());
                alert.showAndWait();
            }
        });
    }

    private void configureColumns(GridPane grid) {
        grid.getColumnConstraints().clear();
        ColumnConstraints timeColumn = new ColumnConstraints(60, 60, 60);
        grid.getColumnConstraints().add(timeColumn);
        for (int day = 0; day < 7; day++) {
            ColumnConstraints dayColumn = new ColumnConstraints(
                    96, 110, Double.MAX_VALUE
            );
            dayColumn.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(dayColumn);
        }
    }

    private LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(settings.getWeekStartsOn()));
    }

    private String formatHour(int hour) {
        return settings.isUse24HourTime()
                ? LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("HH:mm"))
                : LocalTime.of(hour, 0).format(
                        DateTimeFormatter.ofPattern(
                                I18n.language() == AppLanguage.SIMPLIFIED_CHINESE
                                        ? "a h时" : "h a",
                                I18n.locale()));
    }

    private String calendarColorClass(String color) {
        return CourseColor.fromStorage(color).calendarStyleClass();
    }
}
