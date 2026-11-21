package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum NoPushTarget implements DisplayNamed {
    PLAYERS(Lang.PLAYER_NOPUSH_TARGET_PLAYERS),
    WATER(Lang.PLAYER_NOPUSH_TARGET_WATER),
    BLOCKS(Lang.PLAYER_NOPUSH_TARGET_BLOCKS);

    final Translation displayName;

    NoPushTarget(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
