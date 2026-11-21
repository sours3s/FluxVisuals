package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum ItemTrackerOutput implements DisplayNamed {
    CHAT(Lang.CHAT),
    NOTIFICATIONS(Lang.ITEM_TRACKER_OUTPUT_NOTIFICATIONS);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    ItemTrackerOutput(Translation translation) {
        this.displayName = translation;
    }
}
