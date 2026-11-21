package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum ItemCategory implements DisplayNamed {
    ARMOR(Lang.AUCTION_HELPER_EXCLUDES_ARMOR),
    ELYTRA(Lang.AUCTION_HELPER_EXCLUDES_ELYTRA),
    TOTEMS(Lang.AUCTION_HELPER_EXCLUDES_TOTEMS),
    PLAYER_HEADS(Lang.AUCTION_HELPER_EXCLUDES_PLAYER_HEADS),
    SWORDS(Lang.AUCTION_HELPER_EXCLUDES_SWORDS),
    PICKAXE(Lang.AUCTION_HELPER_EXCLUDES_PICKAXE),
    AXE(Lang.AUCTION_HELPER_EXCLUDES_AXE),
    SHOVEL(Lang.AUCTION_HELPER_EXCLUDES_SHOVEL),
    HOE(Lang.AUCTION_HELPER_EXCLUDES_HOE),
    TRIDENT(Lang.AUCTION_HELPER_EXCLUDES_TRIDENT),
    BOWS(Lang.AUCTION_HELPER_EXCLUDES_BOWS),
    CROSSBOWS(Lang.AUCTION_HELPER_EXCLUDES_CROSSBOWS),
    ARROWS(Lang.AUCTION_HELPER_EXCLUDES_ARROWS),
    FLINT_AND_STEEL(Lang.AUCTION_HELPER_EXCLUDES_FLINT_AND_STEEL),
    HORSE_ARMOR(Lang.AUCTION_HELPER_EXCLUDES_HORSE_ARMOR),
    POTIONS(Lang.AUCTION_HELPER_EXCLUDES_POTIONS),
    GLASS_BOTTLES(Lang.AUCTION_HELPER_EXCLUDES_GLASS_BOTTLES),
    DRAGON_BREATH(Lang.AUCTION_HELPER_EXCLUDES_DRAGON_BREATH);

    final Translation displayName;

    ItemCategory(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
