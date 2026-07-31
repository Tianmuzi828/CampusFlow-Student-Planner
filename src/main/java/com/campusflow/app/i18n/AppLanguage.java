package com.campusflow.app.i18n;

import java.util.Locale;

public enum AppLanguage {
    ENGLISH(Locale.ENGLISH, "English"),
    SIMPLIFIED_CHINESE(Locale.SIMPLIFIED_CHINESE, "简体中文");

    private final Locale locale;
    private final String nativeName;

    AppLanguage(Locale locale, String nativeName) {
        this.locale = locale;
        this.nativeName = nativeName;
    }

    public Locale locale() {
        return locale;
    }

    public static AppLanguage fromStorage(String value) {
        if (value == null || value.isBlank()) {
            return ENGLISH;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException exception) {
            return ENGLISH;
        }
    }

    @Override
    public String toString() {
        return nativeName;
    }
}
