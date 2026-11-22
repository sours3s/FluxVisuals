package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum NoSlowMode implements DisplayNamed {
    CANCEL(Lang.CANCEL),
    MATRIX(Translation.clearText("Matrix")),
    CUSTOM_WORLD(Translation.clearText("Custom World")),
    CUSTOM_CROSSBOW(Translation.clearText("Custom Crossbow")),
    STRICT_50(Translation.clearText("Strict 50%")),
    STRICT(Translation.clearText("Legacy Strict"));

    final Translation displayName;

    NoSlowMode(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
