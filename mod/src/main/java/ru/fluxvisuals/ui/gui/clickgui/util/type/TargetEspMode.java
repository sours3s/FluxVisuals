package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum TargetEspMode implements DisplayNamed {
    CUBE(Lang.TARGETESP_MODE_CUBE),
    GHOSTS(Lang.TARGETESP_MODE_GHOSTS);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    TargetEspMode(Translation translation) {
        this.displayName = translation;
    }
}
