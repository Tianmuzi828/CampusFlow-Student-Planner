package com.campusflow.app.model;

import com.campusflow.app.i18n.I18n;

public enum Priority {
    LOW("priority.low", "priority-low"),
    MEDIUM("priority.medium", "priority-medium"),
    HIGH("priority.high", "priority-high");

    private final String textKey;
    private final String cssClass;

    Priority(String textKey, String cssClass) {
        this.textKey = textKey;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return I18n.text(textKey);
    }

    public String getCssClass() {
        return cssClass;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
