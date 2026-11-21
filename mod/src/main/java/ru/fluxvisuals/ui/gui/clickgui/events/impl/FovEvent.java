package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;

public class FovEvent extends CancellableEvent {
    public int fov;

    public int getFov() {
        return this.fov;
    }

    public void setFov(int i) {
        this.fov = i;
    }
}
