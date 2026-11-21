package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum VelocityMode implements DisplayNamed {
    CANCEL(Translation.clearText("Cancel")),
    JUMP_RESET(Translation.clearText("Jump Reset")),
    GRIM(Translation.clearText("Grim 2344"));

    final Translation displayName;

    VelocityMode(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
