package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.type.TickStage;

public class PlayerTickEvent extends CancellableEvent {
    public final TickStage stage;

    public boolean isPost() {
        return this.stage == TickStage.POST;
    }

    public boolean isPre() {
        return this.stage == TickStage.PRE;
    }

    public PlayerTickEvent(TickStage tickStage) {
        this.stage = tickStage;
    }

    public TickStage getStage() {
        return this.stage;
    }
}
