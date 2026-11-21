package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum ModuleTab {
    COMBAT(Lang.COMBAT, "combat"),
    MOVEMENT(Lang.MOVEMENT, "movement"),
    PLAYER(Lang.PLAYER, "player"),
    RENDER(Lang.RENDER, "render"),
    MISC(Lang.MISC, "misc"),
    CONFIGS(Lang.CONFIGS, "cloud");

    public final Translation name;
    public final String iconName;

    public Translation getName() {
        return this.name;
    }

    public String getIconName() {
        return this.iconName;
    }

    ModuleTab(Translation translation, String str) {
        this.name = translation;
        this.iconName = str;
    }
}
