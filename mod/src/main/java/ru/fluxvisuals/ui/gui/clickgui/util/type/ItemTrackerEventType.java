package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum ItemTrackerEventType implements DisplayNamed {
    FOOD_USE(Lang.ITEM_TRACKER_EVENT_FOOD),
    TOTEM_POP(Lang.ITEM_TRACKER_EVENT_TOTEM);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    ItemTrackerEventType(Translation translation) {
        this.displayName = translation;
    }
}
