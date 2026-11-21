package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum MineHelperAction implements DisplayNamed {
    NEXT_MINE(Lang.MINE_HELPER_HELP_TYPE_NEXT_MINE),
    CLEAN_INVENTORY(Lang.MINE_HELPER_HELP_TYPE_CLEAN_INVENTORY),
    SAVE_PICKAXE(Lang.MINE_HELPER_HELP_TYPE_SAVE_PICKAXE);

    public final Translation displayName;

    MineHelperAction(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
