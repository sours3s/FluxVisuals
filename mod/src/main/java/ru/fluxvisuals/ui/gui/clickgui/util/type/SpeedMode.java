package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum SpeedMode implements DisplayNamed {
    MATRIX(Lang.SPEED_MODE_MATRIX),
    COLLISION(Lang.SPEED_MODE_COLLISION),
    CUSTOM_WORLD(Translation.clearText("Custom World"));

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Translation displayName() {
        return this.displayName;
    }

    SpeedMode(Translation translation) {
        this.displayName = translation;
    }
}
