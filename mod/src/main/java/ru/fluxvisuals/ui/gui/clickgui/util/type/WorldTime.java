package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum WorldTime implements DisplayNamed {
    DAY(Lang.WORLD_TWEAKS_DAY, 1000),
    NIGHT(Lang.WORLD_TWEAKS_NIGHT, 18000),
    SUNSET(Lang.WORLD_TWEAKS_SUNSET, 12000),
    MIDNIGHT(Lang.WORLD_TWEAKS_MIDNIGHT, 13000),
    SUNRISE(Lang.WORLD_TWEAKS_SUNRISE, 23000);

    public final Translation displayName;
    public final int ticks;

    WorldTime(Translation translation, int i) {
        this.displayName = translation;
        this.ticks = i;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public int ticks() {
        return this.ticks;
    }
}
