package ru.fluxvisuals.ui.gui.clickgui.util.render;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ConfigOrigin;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ThemeMode;

import java.time.Instant;

public class Theme {
    public final String id;
    public String name;
    public String owner;
    public final ConfigOrigin kind;

    public final ThemeMode type;

    public final ThemePalette palette;

    public final Instant createdAt;
    public Instant updatedAt;

    public static Theme of(String str, String str2, String str3, ConfigOrigin configOrigin, ThemeMode themeMode, ThemePalette themePalette, Instant instant, Instant instant2) {
        return new Theme(str, str2, str3, configOrigin, themeMode, themePalette, instant, instant2);
    }

    public Theme rename(String str) {
        if (str == null || str.isBlank()) {
            return this;
        }
        this.name = str;
        return markUpdated();
    }

    public Theme owner(String str) {
        this.owner = str;
        return markUpdated();
    }

    public Theme markUpdated() {
        this.updatedAt = Instant.now();
        return this;
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public String owner() {
        return this.owner;
    }

    public ConfigOrigin kind() {
        return this.kind;
    }

    public ThemeMode type() {
        return this.type;
    }

    public ThemePalette palette() {
        return this.palette;
    }

    public Instant createdAt() {
        return this.createdAt;
    }

    public Instant updatedAt() {
        return this.updatedAt;
    }

    public Theme(String str, String str2, String str3, ConfigOrigin configOrigin, ThemeMode themeMode, ThemePalette themePalette, Instant instant, Instant instant2) {
        this.id = str;
        this.name = str2;
        this.owner = str3;
        this.kind = configOrigin;
        this.type = themeMode;
        this.palette = themePalette;
        this.createdAt = instant;
        this.updatedAt = instant2;
    }
}
