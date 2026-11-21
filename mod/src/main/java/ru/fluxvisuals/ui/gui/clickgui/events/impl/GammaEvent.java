package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;

public class GammaEvent extends CancellableEvent {
    public double gamma;

    public double getGamma() {
        return this.gamma;
    }

    public void setGamma(double d) {
        this.gamma = d;
    }
}
