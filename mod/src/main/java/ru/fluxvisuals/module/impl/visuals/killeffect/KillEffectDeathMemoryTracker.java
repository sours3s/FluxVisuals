package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.function.Consumer;
import net.minecraft.entity.LivingEntity;

public class KillEffectDeathMemoryTracker {
    public KillEffectDeathMemoryTracker(long time, Consumer<LivingEntity> callback) {}

    public void tick() {}
    public void remember(LivingEntity entity, boolean b) {}
    public void forget(int id) {}
    public void clear() {}
}
