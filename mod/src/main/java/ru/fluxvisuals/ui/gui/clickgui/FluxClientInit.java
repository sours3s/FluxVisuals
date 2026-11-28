package ru.fluxvisuals.ui.gui.clickgui;

import net.fabricmc.api.ClientModInitializer;
import ru.fluxvisuals.client.FluxVisualsClient;

public class FluxClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (FluxVisualsClient.get == null) {
            return;
        }
        FluxBootstrap.init();
    }
}
