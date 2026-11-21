package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum NukerDiggingMode implements DisplayNamed {
    EVERYONE(Lang.NUKER_DIGGINGMODE_EVERYONE),
    ORE_PRIORITY(Lang.NUKER_DIGGINGMODE_ORE_PRIORITY),
    ONLY_ORE(Lang.NUKER_DIGGINGMODE_ONLY_ORE);

    public final Translation displayName;

    NukerDiggingMode(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
