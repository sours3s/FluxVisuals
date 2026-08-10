package ru.fluxvisuals.api.themes;

import lombok.Getter;

import java.awt.*;

@Getter
public enum ThemePresets {
    PURPLE("Purple", new Color(138, 43, 226), new Color(75, 0, 130)),
    BLUE("Blue", new Color(0, 119, 182), new Color(0, 180, 216)),
    Lavanda("Lavanda",new Color(146, 76, 228),new Color(179, 128, 213)),
    RED("Orange", new Color(255, 5, 14), new Color(136, 3, 3)),
    GREEN("Green", new Color(34, 139, 34), new Color(50, 205, 50)),
    CUSTOM("Custom", new Color(0), new Color(0));

    private final String name;
    private final Color main, secondary;

    ThemePresets(String name, Color main, Color secondary) {
        this.name = name;
        this.main = main;
        this.secondary = secondary;
    }
}
