package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum TotemActivationMode implements DisplayNamed {
    AUTO(Lang.COMBAT_AUTOTOTEM_ACTIVATION_MODE_AUTO),
    BUTTON(Lang.COMBAT_AUTOTOTEM_ACTIVATION_MODE_BUTTON);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Translation displayName() {
        return this.displayName;
    }

    TotemActivationMode(Translation translation) {
        this.displayName = translation;
    }
}
