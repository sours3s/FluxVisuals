package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum EspTargetType implements DisplayNamed {
    SELF(Lang.SELECTTARGETS_SELF),
    PLAYERS(Lang.SELECTTARGETS_PLAYERS),
    MOBS(Lang.SELECTTARGETS_MOBS),
    ANIMALS(Lang.SELECTTARGETS_ANIMALS),
    FRIENDS(Lang.SELECTTARGETS_FRIENDS),
    ITEMS(Lang.SELECTTARGETS_ITEMS);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    EspTargetType(Translation translation) {
        this.displayName = translation;
    }
}
