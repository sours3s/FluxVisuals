package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum AuctionSphereType implements DisplayNamed {
    CHIMERA(Lang.AUCTION_HELPER_SPHERES_CHIMERA),
    ANDROMEDA(Lang.AUCTION_HELPER_SPHERES_ANDROMEDA),
    PANDORA(Lang.AUCTION_HELPER_SPHERES_PANDORA),
    TITAN(Lang.AUCTION_HELPER_SPHERES_TITAN);

    final Translation displayName;

    AuctionSphereType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
