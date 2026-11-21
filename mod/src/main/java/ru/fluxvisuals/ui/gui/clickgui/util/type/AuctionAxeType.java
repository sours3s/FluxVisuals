package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum AuctionAxeType implements DisplayNamed {
    EFFICIENCY(Lang.AUCTION_HELPER_AXES_EFFICIENCY),
    FORTUNE(Lang.AUCTION_HELPER_AXES_FORTUNE),
    MENDING(Lang.AUCTION_HELPER_AXES_MENDING),
    PINGER(Lang.AUCTION_HELPER_AXES_PINGER),
    MAGNET(Lang.AUCTION_HELPER_AXES_MAGNET),
    LUMBERJACK(Lang.AUCTION_HELPER_AXES_LUMBERJACK),
    BULLDOZING(Lang.AUCTION_HELPER_SHOVELS_BULLDOZING),
    UNBREAKING(Lang.AUCTION_HELPER_AXES_UNBREAKING);

    final Translation displayName;

    AuctionAxeType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
