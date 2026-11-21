package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum ClientSoundType implements DisplayNamed {
    TYPE_1(Lang.CLIENTSOUNDS_TYPE_1),
    TYPE_2(Lang.CLIENTSOUNDS_TYPE_2),
    TYPE_3(Lang.CLIENTSOUNDS_TYPE_3),
    TYPE_4(Lang.CLIENTSOUNDS_TYPE_4),
    TYPE_5(Lang.CLIENTSOUNDS_TYPE_5),
    TYPE_6(Lang.CLIENTSOUNDS_TYPE_6),
    TYPE_7(Lang.CLIENTSOUNDS_TYPE_7);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    ClientSoundType(Translation translation) {
        this.displayName = translation;
    }
}
