package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.type.RenderOverlayType;

public class RenderOverlayEvent extends CancellableEvent {
    public final RenderOverlayType type;

    public RenderOverlayType getType() {
        return this.type;
    }

    public RenderOverlayEvent(RenderOverlayType renderOverlayType) {
        this.type = renderOverlayType;
    }
}
