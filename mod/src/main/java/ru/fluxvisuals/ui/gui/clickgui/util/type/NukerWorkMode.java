package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum NukerWorkMode implements DisplayNamed {
    EVERYWHERE(Lang.NUKER_WORKMODE_EVERYWHERE),
    ONLY_MINE(Lang.NUKER_WORKMODE_ONLY_MINE);

    public final Translation displayName;

    NukerWorkMode(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
