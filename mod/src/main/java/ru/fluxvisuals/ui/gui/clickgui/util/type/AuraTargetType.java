package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum AuraTargetType implements DisplayNamed {
    PLAYERS(Lang.SELECTTARGETS_PLAYERS),
    MOBS(Lang.SELECTTARGETS_MOBS),
    ANIMALS(Lang.SELECTTARGETS_ANIMALS),
    FRIENDS(Lang.SELECTTARGETS_FRIENDS);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Translation displayName() {
        return this.displayName;
    }

    AuraTargetType(Translation translation) {
        this.displayName = translation;
    }
}
