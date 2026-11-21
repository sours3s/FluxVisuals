package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;

import net.minecraft.entity.Entity;

public class WorldEntityRenderEvent extends CancellableEvent {
    public Entity entity;

    public Entity entity() {
        return this.entity;
    }

    public WorldEntityRenderEvent(Entity entity) {
        this.entity = entity;
    }
}
