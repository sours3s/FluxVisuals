package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum AuctionElytraType implements DisplayNamed {
    UNBREAKING5(Lang.AUCTION_HELPER_ELYTRA_UNBREAKING5),
    MENDING(Lang.AUCTION_HELPER_ELYTRA_MENDING);

    final Translation displayName;

    AuctionElytraType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
