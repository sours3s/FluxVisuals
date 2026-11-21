package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum MoveCorrectionMode implements DisplayNamed {
    NONE(Lang.ATTACKAURA_MOVE_CORRECTION_NONE),
    FREE(Lang.ATTACKAURA_MOVE_CORRECTION_FREE),
    TARGET(Lang.ATTACKAURA_MOVE_CORRECTION_TARGET),
    FOCUS(Lang.ATTACKAURA_MOVE_CORRECTION_FOCUS);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Translation displayName() {
        return this.displayName;
    }

    MoveCorrectionMode(Translation translation) {
        this.displayName = translation;
    }
}
