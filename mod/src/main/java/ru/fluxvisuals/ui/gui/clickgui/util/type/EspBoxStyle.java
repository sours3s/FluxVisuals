package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum EspBoxStyle implements DisplayNamed {
    BOX(Lang.ESP_BOX_MODE_BOX),
    CORNER(Lang.ESP_BOX_MODE_CORNER),
    ROUNDED(Lang.ESP_BOX_MODE_ROUNDED);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    EspBoxStyle(Translation translation) {
        this.displayName = translation;
    }
}
