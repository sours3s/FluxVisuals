package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;

import net.minecraft.client.gui.screen.Screen;

public class ScreenOpenEvent extends CancellableEvent {
    public final Screen screen;

    public Screen screen() {
        return this.screen;
    }

    public ScreenOpenEvent(Screen screen) {
        this.screen = screen;
    }
}
