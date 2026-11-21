package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ConfigOrigin;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.StaffProfile;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Theme;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ThemeMode;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ThemeCard {
    public String name;
    public ThemeMode type;

    public ConfigOrigin kind;

    public ThemePalette palette;
    public final String cloudId;
    public final String date;

    public StaffProfile author;

    public ThemeCard(Theme theme, GlTexture glTexture) {
        this.name = theme.name();
        this.type = theme.type();
        this.kind = theme.kind();
        this.palette = theme.palette();
        this.date = formatDate(Timestamp.from(theme.createdAt()).getTime());
        this.cloudId = theme.id();
        this.author = new StaffProfile(theme.owner(), "", glTexture, null);
    }

    public String formatDate(long j) {
        return new SimpleDateFormat("dd.MM.yyyy").format(new Date(j));
    }

    public String name() {
        return this.name;
    }

    public ThemeMode type() {
        return this.type;
    }

    public ConfigOrigin kind() {
        return this.kind;
    }

    public ThemePalette palette() {
        return this.palette;
    }

    public String cloudId() {
        return this.cloudId;
    }

    public String date() {
        return this.date;
    }

    public StaffProfile author() {
        return this.author;
    }

    public ThemeCard(String str, String str2) {
        this.cloudId = str;
        this.date = str2;
    }

    public ThemeCard name(String str) {
        this.name = str;
        return this;
    }
}
