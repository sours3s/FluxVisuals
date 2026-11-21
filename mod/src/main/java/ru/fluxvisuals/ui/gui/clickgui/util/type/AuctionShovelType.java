package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum AuctionShovelType implements DisplayNamed {
    BULLDOZING(Lang.AUCTION_HELPER_SHOVELS_BULLDOZING),
    MAGNET(Lang.AUCTION_HELPER_SHOVELS_MAGNET),
    UNBREAKING(Lang.AUCTION_HELPER_SHOVELS_UNBREAKING),
    MENDING(Lang.AUCTION_HELPER_SHOVELS_MENDING),
    FORTUNE(Lang.AUCTION_HELPER_SHOVELS_FORTUNE),
    EFFICIENCY(Lang.AUCTION_HELPER_SHOVELS_EFFICIENCY);

    final Translation displayName;

    AuctionShovelType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
