package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum TpLootStage {
    APPROACH(Lang.CREEPERFARM_PHASE_APPROACH),
    LOADING_CHUNKS(Lang.CREEPERFARM_PHASE_LOADING_CHUNKS),
    LOOTING(Lang.CREEPERFARM_PHASE_LOOTING),
    UNLOADING(Lang.CREEPERFARM_PHASE_UNLOADING);

    public final Translation name;

    TpLootStage(Translation translation) {
        this.name = translation;
    }

    public Translation getName() {
        return this.name;
    }
}
