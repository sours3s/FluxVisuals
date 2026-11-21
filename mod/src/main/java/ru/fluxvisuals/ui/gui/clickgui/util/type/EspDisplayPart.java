package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum EspDisplayPart implements DisplayNamed {
    BOX(Lang.ESP_DISPLAY_BOX),
    TAGS(Lang.ESP_DISPLAY_TAGS),
    HEALTH(Lang.ESP_DISPLAY_HEALTH),
    OFFHAND(Lang.ESP_DISPLAY_OFFHAND),
    SHULKER(Lang.ESP_DISPLAY_SHULKER);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    EspDisplayPart(Translation translation) {
        this.displayName = translation;
    }
}
