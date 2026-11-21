package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum GrimAdvancedMode implements DisplayNamed {
    GRIM_ADVANCED(Translation.clearText("Grim Advanced"));

    final Translation displayName;

    GrimAdvancedMode(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
