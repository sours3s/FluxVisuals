package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;

import net.minecraft.entity.Entity;

public class EntityRenderEvent extends CancellableEvent {
    public final Entity entity;

    public Entity getEntity() {
        return this.entity;
    }

    public EntityRenderEvent(Entity entity) {
        this.entity = entity;
    }
}
