package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum TargetPearlMode implements DisplayNamed {
    AURA(Lang.TARGETPEARL_TARGET_AURA),
    ALL(Lang.TARGETPEARL_TARGET_ALL);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Translation displayName() {
        return this.displayName;
    }

    TargetPearlMode(Translation translation) {
        this.displayName = translation;
    }
}
