package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum AuctionPickaxeType implements DisplayNamed {
    MENDING(Lang.AUCTION_HELPER_PICKAXES_MENDING),
    EFFICIENCY(Lang.AUCTION_HELPER_PICKAXES_EFFICIENCY),
    FORTUNE(Lang.AUCTION_HELPER_PICKAXES_FORTUNE),
    UNBREAKING(Lang.AUCTION_HELPER_PICKAXES_UNBREAKING),
    SILK_TOUCH(Lang.AUCTION_HELPER_PICKAXES_SILK_TOUCH),
    SMELTING(Lang.AUCTION_HELPER_PICKAXES_SMELTING),
    MAGNET(Lang.AUCTION_HELPER_PICKAXES_MAGNET),
    BULLDOZING(Lang.AUCTION_HELPER_PICKAXES_BULLDOZING),
    WITHOUT_HEAVY(Lang.AUCTION_HELPER_PICKAXES_WITHOUT_HEAVY);

    final Translation displayName;

    AuctionPickaxeType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
