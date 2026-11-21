package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.Event;

import net.minecraft.entity.Entity;

public final class CameraEntityRenderEvent implements Event {
    public final Entity entity;
    public final float tickDelta;

    public CameraEntityRenderEvent(Entity entity, float f) {
        this.entity = entity;
        this.tickDelta = f;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "entity=" + this.entity + ", " + "tickDelta=" + this.tickDelta + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.entity, this.tickDelta);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CameraEntityRenderEvent)) return false;
        CameraEntityRenderEvent o = (CameraEntityRenderEvent) obj;
        return java.util.Objects.equals(this.entity, o.entity) && java.util.Objects.equals(this.tickDelta, o.tickDelta);
    }
public Entity entity() {
        return this.entity;
    }

    public float tickDelta() {
        return this.tickDelta;
    }
}
