package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum NoFallMode implements DisplayNamed {
    SPOOF_GROUND(Lang.NOFALL_MODE_SPOOF_GROUND),
    NO_GROUND(Lang.NOFALL_MODE_NO_GROUND),
    PACKET(Lang.NOFALL_MODE_PACKET);

    final Translation displayName;

    NoFallMode(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
