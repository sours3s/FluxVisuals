package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum RemovedEffectType implements DisplayNamed {
    JUMP_BOOST(Lang.REMOVEEFFECTS_POTIONS_JUMP_BOOST),
    BLINDNESS(Lang.REMOVEEFFECTS_POTIONS_BLINDNESS),
    NAUSEA(Lang.REMOVEEFFECTS_POTIONS_NAUSEA),
    DARKNESS(Lang.REMOVEEFFECTS_POTIONS_DARKNESS);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    RemovedEffectType(Translation translation) {
        this.displayName = translation;
    }
}
