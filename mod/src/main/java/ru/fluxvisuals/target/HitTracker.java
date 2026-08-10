package ru.fluxvisuals.target;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Детектор попаданий по прицеленной сущности (по убыванию HP во время замаха).
 * Никаких пакетов — только чтение уже полученных данных.
 */
public final class HitTracker {
    public interface Listener {
        void onHit(LivingEntity entity, Vec3d hitPos, float damage);
    }

    private static final List<Listener> LISTENERS = new ArrayList<>();
    private static float lastTargetHealth = -1;

    private HitTracker() {}

    public static void register(Listener listener) {
        LISTENERS.add(listener);
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            lastTargetHealth = -1;
            return;
        }
        Entity target = TargetUtils.getEntity(mc);
        if (target instanceof LivingEntity le && le.isAlive() && TargetUtils.distanceTo(mc, le) <= 5.5f) {
            float h = le.getHealth();
            if (lastTargetHealth < 0) lastTargetHealth = h;
            if (lastTargetHealth > h + 0.01f && mc.player.handSwinging) {
                float dmg = lastTargetHealth - h;
                Vec3d hitPos = new Vec3d(le.getX(), le.getY() + le.getHeight() / 2, le.getZ());
                for (Listener l : LISTENERS) {
                    try { l.onHit(le, hitPos, dmg); } catch (Exception ignored) {}
                }
            }
            lastTargetHealth = h;
        } else {
            lastTargetHealth = -1;
        }
    }
}
