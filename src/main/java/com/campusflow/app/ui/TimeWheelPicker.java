package com.campusflow.app.ui;

import com.campusflow.app.i18n.I18n;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A compact time picker made from independently scrollable hour, minute,
 * and AM/PM wheels.
 */
public final class TimeWheelPicker extends HBox {
    private final boolean use24HourTime;
    private final int minuteStep;
    private final Spinner<Integer> hourSpinner;
    private final Spinner<Integer> minuteSpinner;
    private final Spinner<String> periodSpinner;

    public TimeWheelPicker(LocalTime initialTime, boolean use24HourTime,
                           int minuteStep) {
        if (minuteStep <= 0 || 60 % minuteStep != 0) {
            throw new IllegalArgumentException("Minute step must divide one hour.");
        }
        this.use24HourTime = use24HourTime;
        this.minuteStep = minuteStep;

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4);
        getStyleClass().add("time-wheel-picker");

        hourSpinner = createHourSpinner();
        minuteSpinner = createMinuteSpinner();
        periodSpinner = use24HourTime ? null : createPeriodSpinner();

        Label separator = new Label(":");
        separator.getStyleClass().add("time-wheel-separator");
        getChildren().addAll(hourSpinner, separator, minuteSpinner);
        if (periodSpinner != null) {
            getChildren().add(periodSpinner);
        }

        setValue(initialTime);
        Tooltip tooltip = new Tooltip(
                I18n.text("timeWheel.tooltip", minuteStep)
        );
        Tooltip.install(this, tooltip);
    }

    public LocalTime getValue() {
        int hour = hourSpinner.getValue();
        if (!use24HourTime) {
            hour %= 12;
            if ("PM".equals(periodSpinner.getValue())) {
                hour += 12;
            }
        }
        return LocalTime.of(hour, minuteSpinner.getValue());
    }

    public void setValue(LocalTime time) {
        LocalTime safeTime = time == null ? LocalTime.NOON : time;
        int minute = safeTime.getMinute()
                - safeTime.getMinute() % minuteStep;
        minuteSpinner.getValueFactory().setValue(minute);

        if (use24HourTime) {
            hourSpinner.getValueFactory().setValue(safeTime.getHour());
        } else {
            int displayHour = safeTime.getHour() % 12;
            hourSpinner.getValueFactory().setValue(
                    displayHour == 0 ? 12 : displayHour
            );
            periodSpinner.getValueFactory().setValue(
                    safeTime.getHour() < 12 ? "AM" : "PM"
            );
        }
    }

    private Spinner<Integer> createHourSpinner() {
        int minimum = use24HourTime ? 0 : 1;
        int maximum = use24HourTime ? 23 : 12;
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        minimum, maximum, use24HourTime ? 12 : 12
                );
        factory.setWrapAround(true);
        factory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer hour) {
                if (hour == null) {
                    return "";
                }
                return use24HourTime ? String.format("%02d", hour)
                        : Integer.toString(hour);
            }

            @Override
            public Integer fromString(String text) {
                return Integer.parseInt(text.trim());
            }
        });
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(factory);
        configureSpinner(spinner, 58, I18n.text("timeWheel.hour"));
        return spinner;
    }

    private Spinner<Integer> createMinuteSpinner() {
        List<Integer> minutes = new ArrayList<>();
        for (int minute = 0; minute < 60; minute += minuteStep) {
            minutes.add(minute);
        }
        SpinnerValueFactory.ListSpinnerValueFactory<Integer> factory =
                new SpinnerValueFactory.ListSpinnerValueFactory<>(
                        FXCollections.observableArrayList(minutes)
                );
        factory.setWrapAround(true);
        factory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer minute) {
                return minute == null ? "" : String.format("%02d", minute);
            }

            @Override
            public Integer fromString(String text) {
                return Integer.parseInt(text.trim());
            }
        });
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(factory);
        configureSpinner(spinner, 58, I18n.text("timeWheel.minute"));
        return spinner;
    }

    private Spinner<String> createPeriodSpinner() {
        SpinnerValueFactory.ListSpinnerValueFactory<String> factory =
                new SpinnerValueFactory.ListSpinnerValueFactory<>(
                        FXCollections.observableArrayList("AM", "PM")
                );
        factory.setWrapAround(true);
        factory.setConverter(new StringConverter<>() {
            @Override
            public String toString(String period) {
                if (period == null) {
                    return "";
                }
                return I18n.text("AM".equals(period) ? "time.am" : "time.pm");
            }

            @Override
            public String fromString(String text) {
                return I18n.text("time.pm").equals(text) ? "PM" : "AM";
            }
        });
        Spinner<String> spinner = new Spinner<>();
        spinner.setValueFactory(factory);
        configureSpinner(spinner, 62, I18n.text("timeWheel.period"));
        return spinner;
    }

    private <T> void configureSpinner(Spinner<T> spinner, double width,
                                      String accessibleText) {
        spinner.setEditable(false);
        spinner.setPrefWidth(width);
        spinner.setMinWidth(width);
        spinner.setMaxWidth(width);
        spinner.setAccessibleText(accessibleText);
        spinner.getStyleClass().add("time-wheel-spinner");
        spinner.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() > 0) {
                spinner.increment();
            } else if (event.getDeltaY() < 0) {
                spinner.decrement();
            }
            event.consume();
        });
    }
}
