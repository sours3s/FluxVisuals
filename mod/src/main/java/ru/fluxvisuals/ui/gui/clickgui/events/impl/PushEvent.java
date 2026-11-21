package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.type.PushType;

public class PushEvent extends CancellableEvent {
    public final PushType type;

    public PushType getType() {
        return this.type;
    }

    public PushEvent(PushType pushType) {
        this.type = pushType;
    }
}
