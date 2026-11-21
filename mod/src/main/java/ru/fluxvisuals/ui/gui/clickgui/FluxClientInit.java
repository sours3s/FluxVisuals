package ru.fluxvisuals.ui.gui.clickgui;

import net.fabricmc.api.ClientModInitializer;

public class FluxClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FluxBootstrap.init();
    }
}
