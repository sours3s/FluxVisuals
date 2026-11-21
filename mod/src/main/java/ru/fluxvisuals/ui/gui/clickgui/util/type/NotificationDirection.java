package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum NotificationDirection implements DisplayNamed {
    AUTO(Lang.WIDGET_NOTIFICATIONS_DIRECTION_AUTO),
    UPWARD(Lang.WIDGET_NOTIFICATIONS_DIRECTION_UPWARD),
    DOWNWARD(Lang.WIDGET_NOTIFICATIONS_DIRECTION_DOWNWARD);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    NotificationDirection(Translation translation) {
        this.displayName = translation;
    }
}
