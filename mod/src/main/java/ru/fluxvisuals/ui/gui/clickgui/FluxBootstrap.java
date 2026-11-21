package ru.fluxvisuals.ui.gui.clickgui;

public class FluxBootstrap {
    public static void init() {
        try {
            new FluxVisuals();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
