package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum CrosshairStyle implements DisplayNamed {
    DEFAULT(Lang.CROSSHAIR_TYPE_DEFAULT),
    CIRCLE(Lang.CROSSHAIR_TYPE_CIRCLE);

    final Translation displayName;

    CrosshairStyle(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
