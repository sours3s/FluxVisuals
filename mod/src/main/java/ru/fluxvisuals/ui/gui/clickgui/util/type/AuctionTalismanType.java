package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum AuctionTalismanType implements DisplayNamed {
    ECHIDNA(Lang.AUCTION_HELPER_TALISMANS_ECHIDNA),
    DAEDALUS(Lang.AUCTION_HELPER_TALISMANS_DAEDALUS),
    DESTROYER(Lang.AUCTION_HELPER_TALISMANS_DESTROYER),
    PUNISHER(Lang.AUCTION_HELPER_TALISMANS_PUNISHER);

    final Translation displayName;

    AuctionTalismanType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
