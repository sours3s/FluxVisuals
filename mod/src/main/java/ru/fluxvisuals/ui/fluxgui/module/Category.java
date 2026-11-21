package ru.fluxvisuals.ui.fluxgui.module;

/** Категории GUI FluxVisuals, соответствующие категориям наших модулей. */
public enum Category {
    PLAYER("Игрок"),
    VISUALS("Визуальные"),
    MISC("Разное"),
    INTERFACE("Интерфейс"),
    UTILITY("Утилиты"),
    SERVER_EVENTS("События"),
    MINES("Шахты"),
    CONFIGS("Конфиги"),
    THEMES("Темы"),
    MARKS("Метки"),
    FRIENDS("Друзья"),
    EVENTS("Чат"),
    MESSENGER("Мессенджер"),
    COSMETICS("Косметика");

    private final String name;

    Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
