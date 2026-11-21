package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.type.VisualEffectType;

public class VisualEffectEvent extends CancellableEvent {
    public final VisualEffectType type;

    public VisualEffectType getType() {
        return this.type;
    }

    public VisualEffectEvent(VisualEffectType visualEffectType) {
        this.type = visualEffectType;
    }
}
