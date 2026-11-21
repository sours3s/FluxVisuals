package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum TapeMouseHand implements DisplayNamed {
    RIGHT(Lang.TAPEMOUSE_HANDMODE_RIGHT),
    LEFT(Lang.TAPEMOUSE_HANDMODE_LEFT);

    final Translation displayName;

    TapeMouseHand(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
