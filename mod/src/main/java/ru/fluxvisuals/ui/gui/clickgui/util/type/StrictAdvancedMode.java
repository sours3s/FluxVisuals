package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum StrictAdvancedMode implements DisplayNamed {
    STRICT_ADVANCED(Translation.clearText("Strict Advanced"));

    final Translation displayName;

    StrictAdvancedMode(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
