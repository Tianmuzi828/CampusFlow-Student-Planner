package com.campusflow.app.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class I18nTest {
    @AfterEach
    void restoreEnglish() {
        I18n.setLanguage(AppLanguage.ENGLISH);
    }

    @Test
    void switchesTextAndDateFormattingWithoutChangingUserData() {
        I18n.setLanguage(AppLanguage.ENGLISH);
        assertEquals("Assignments", I18n.text("assignments.title"));
        assertEquals("Wednesday, July 29", I18n.date(
                LocalDate.of(2026, 7, 29), "date.current.pattern"));

        I18n.setLanguage(AppLanguage.SIMPLIFIED_CHINESE);
        assertEquals("作业", I18n.text("assignments.title"));
        assertEquals("7月29日 星期三", I18n.date(
                LocalDate.of(2026, 7, 29), "date.current.pattern"));
    }

    @Test
    void englishAndChineseBundlesContainTheSameKeys() {
        ResourceBundle english = ResourceBundle.getBundle(
                "com.campusflow.app.i18n.messages", AppLanguage.ENGLISH.locale());
        ResourceBundle chinese = ResourceBundle.getBundle(
                "com.campusflow.app.i18n.messages",
                AppLanguage.SIMPLIFIED_CHINESE.locale());

        assertEquals(english.keySet(), chinese.keySet());
    }

    @Test
    void everyFxmlTranslationReferenceExistsInTheBundle() throws IOException {
        ResourceBundle english = ResourceBundle.getBundle(
                "com.campusflow.app.i18n.messages", AppLanguage.ENGLISH.locale());
        Pattern reference = Pattern.compile("(?:text|promptText)=\"%([^\"]+)\"");

        for (String file : List.of(
                "main-view.fxml", "schedule-view.fxml", "courses-view.fxml",
                "assignments-view.fxml", "settings-view.fxml")) {
            try (InputStream stream = I18nTest.class.getResourceAsStream(
                    "/com/campusflow/app/" + file)) {
                String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                Matcher matcher = reference.matcher(source);
                while (matcher.find()) {
                    String key = matcher.group(1);
                    if (!english.containsKey(key)) {
                        throw new AssertionError(file + " references missing key " + key);
                    }
                }
            }
        }
    }
}
