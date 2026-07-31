package com.campusflow.app.i18n;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public final class I18n {
    private static final String BASE_NAME = "com.campusflow.app.i18n.messages";
    private static AppLanguage language = AppLanguage.ENGLISH;
    private static ResourceBundle bundle = loadBundle(language);

    private I18n() {
    }

    public static synchronized void setLanguage(AppLanguage newLanguage) {
        language = newLanguage == null ? AppLanguage.ENGLISH : newLanguage;
        bundle = loadBundle(language);
        Locale.setDefault(Locale.Category.FORMAT, language.locale());
    }

    public static AppLanguage language() {
        return language;
    }

    public static Locale locale() {
        return language.locale();
    }

    public static ResourceBundle bundle() {
        return bundle;
    }

    public static String text(String key, Object... arguments) {
        String pattern = bundle.containsKey(key) ? bundle.getString(key) : "!" + key + "!";
        if (arguments == null || arguments.length == 0) {
            return pattern;
        }
        MessageFormat formatter = new MessageFormat(pattern, locale());
        return formatter.format(arguments);
    }

    public static String date(LocalDate date, String patternKey) {
        return date.format(DateTimeFormatter.ofPattern(text(patternKey), locale()));
    }

    private static ResourceBundle loadBundle(AppLanguage selectedLanguage) {
        return ResourceBundle.getBundle(
                BASE_NAME,
                selectedLanguage.locale()
        );
    }
}
