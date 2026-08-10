package ru.fluxvisuals.target;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;

/** Сущность, на которую наведён прицел (crosshair target). Только это — никакого «сквозь стены». */
public final class TargetUtils {
    private TargetUtils() {}

    public static Entity getEntity(MinecraftClient mc) {
        if (mc.crosshairTarget instanceof EntityHitResult ehr) {
            return ehr.getEntity();
        }
        return null;
    }

    public static LivingEntity getLivingTarget(MinecraftClient mc) {
        Entity e = getEntity(mc);
        if (e instanceof LivingEntity le) return le;
        return null;
    }

    public static double distanceTo(MinecraftClient mc, Entity entity) {
        if (mc.player == null || entity == null) return Double.MAX_VALUE;
        return mc.player.distanceTo(entity);
    }
}
