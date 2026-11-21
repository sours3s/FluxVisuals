package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.type.OverlayEffectType;

public class OverlayEffectEvent extends CancellableEvent {
    OverlayEffectType type;

    public OverlayEffectEvent(OverlayEffectType overlayEffectType) {
        this.type = overlayEffectType;
    }

    public OverlayEffectType getType() {
        return this.type;
    }
}
