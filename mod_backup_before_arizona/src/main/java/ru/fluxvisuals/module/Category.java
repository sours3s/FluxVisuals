package ru.fluxvisuals.module;

/** Категории модулей для ClickGUI и ArrayList. */
public enum Category {
    HUD("HUD"),
    TARGET("TARGET"),
    VISUAL("VISUAL"),
    MISC("MISC");

    public final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }
}
