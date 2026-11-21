package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum HitSoundType implements DisplayNamed {
    TYPE_1(Lang.HITSOUNDS_TYPE_1),
    TYPE_2(Lang.HITSOUNDS_TYPE_2),
    TYPE_3(Lang.HITSOUNDS_TYPE_3),
    TYPE_4(Lang.HITSOUNDS_TYPE_4),
    MOANS(Lang.HITSOUNDS_TYPE_MOANS);

    final Translation displayName;

    HitSoundType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
