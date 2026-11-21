package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum NoSlowMode implements DisplayNamed {
    CANCEL(Lang.CANCEL),
    MATRIX(Translation.clearText("Matrix")),
    HOLYWORLD(Translation.clearText("Holy World")),
    FUNTIME_CROSSBOW(Translation.clearText("Funtime Crossbow")),
    GRIM_50(Translation.clearText("Grim 50%")),
    GRIM(Translation.clearText("Old Grim"));

    final Translation displayName;

    NoSlowMode(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
