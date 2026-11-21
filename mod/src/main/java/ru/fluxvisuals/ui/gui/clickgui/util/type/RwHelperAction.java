package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum RwHelperAction implements DisplayNamed {
    AUTO_WAYPOINT(Lang.RWHELPER_ACTION_AUTO_WAYPOINT),
    FILTER_BANNED_WORDS(Lang.RWHELPER_ACTION_FILTER_BANNED_WORDS),
    CLOSE_SERVER_MENU(Lang.RWHELPER_ACTION_CLOSE_SERVER_MENU);

    final Translation displayName;

    RwHelperAction(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
