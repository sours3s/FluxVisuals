package ru.fluxvisuals.event.impl.player;

import net.minecraft.entity.Entity;

public class AttackEntityEvent {
    private Entity target;
    private boolean synthetic;

    public AttackEntityEvent() {}
    public AttackEntityEvent(Entity target) { this.target = target; }

    public Entity getTarget() { return target; }
    public boolean isSynthetic() { return false; }
}
