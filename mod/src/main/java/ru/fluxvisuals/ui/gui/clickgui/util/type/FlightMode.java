package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum FlightMode implements DisplayNamed {
    VANILLA(Lang.FLIGHT_MODE_VANILLA),
    CREATIVE(Lang.FLIGHT_MODE_CREATIVE),
    GRIM(Translation.clearText("Grim Blocks"));

    final Translation displayName;

    FlightMode(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
