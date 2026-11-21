package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum TargetSelectionType implements DisplayNamed {
    SELF(Lang.SELECTTARGETS_SELF),
    PLAYERS(Lang.SELECTTARGETS_PLAYERS),
    MOBS(Lang.SELECTTARGETS_MOBS),
    ANIMALS(Lang.SELECTTARGETS_ANIMALS),
    FRIENDS(Lang.SELECTTARGETS_FRIENDS);

    final Translation displayName;

    TargetSelectionType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
