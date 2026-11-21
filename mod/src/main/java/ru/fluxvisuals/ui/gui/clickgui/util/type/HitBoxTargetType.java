package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum HitBoxTargetType implements DisplayNamed {
    PLAYERS(Lang.HITBOX_TARGET_PLAYERS),
    MOBS(Lang.HITBOX_TARGET_MOBS),
    ANIMALS(Lang.HITBOX_TARGET_ANIMALS);

    final Translation displayName;

    HitBoxTargetType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
