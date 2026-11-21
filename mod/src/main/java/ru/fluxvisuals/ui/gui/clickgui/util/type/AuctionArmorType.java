package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum AuctionArmorType implements DisplayNamed {
    WITHOUT_THORNS(Lang.AUCTION_HELPER_ARMOR_WITHOUT_THORNS),
    PROTECTION5(Lang.AUCTION_HELPER_ARMOR_PROTECTION5),
    UNBREAKING5(Lang.AUCTION_HELPER_ARMOR_UNBREAKING5),
    MENDING(Lang.AUCTION_HELPER_ARMOR_MENDING);

    final Translation displayName;

    AuctionArmorType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
