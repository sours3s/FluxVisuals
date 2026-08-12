package ru.fluxvisuals.module.target;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.Animation;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.WorldRenderUtils;
import ru.fluxvisuals.target.HitTracker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Эффект в точке удара + всплывающий урон. */
public class HitEffectsModule extends Module {
    private static final long LIFETIME = 800;

    private final Setting type = Setting.mode("Type", "Тип эффекта", "Ring", "Cross", "Both");
    private final Setting color = Setting.color("Color", "Цвет эффекта", 0xFFFFFFFF);
    private final Setting damagePopup = Setting.bool("Damage", "Показывать урон", true);

    private final List<Effect> effects = new ArrayList<>();

    public HitEffectsModule() {
        super("HitEffects", "Эффект при попадании в сущность", Category.TARGET);
        addSetting(type);
        addSetting(color);
        addSetting(damagePopup);
        HitTracker.register((entity, pos, dmg) -> {
            if (isEnabled()) effects.add(new Effect(pos, dmg));
        });
    }

    @Override
    public void onRender3D(MatrixStack ms, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        long now = System.currentTimeMillis();
        effects.removeIf(e -> now - e.born > LIFETIME);
        int mode = type.getModeIndex();
        for (Effect e : effects) {
            double t = (now - e.born) / (double) LIFETIME;
            double k = Animation.easeOutCubic(t);
            float radius = (float) (0.1 + k * 0.7);
            int col = ColorUtils.mulAlpha(color.getColor(), (float) (1 - t));
            if (mode != 1) {
                WorldRenderUtils.drawRing3D(ms, consumers, camera, e.pos, radius, 0f, col, 40);
            }
            if (mode != 0) {
                float r = radius;
                WorldRenderUtils.drawLine3D(ms, consumers, camera, e.pos, e.pos.add(r, 0, 0), col);
                WorldRenderUtils.drawLine3D(ms, consumers, camera, e.pos, e.pos.add(-r, 0, 0), col);
                WorldRenderUtils.drawLine3D(ms, consumers, camera, e.pos, e.pos.add(0, r, 0), col);
                WorldRenderUtils.drawLine3D(ms, consumers, camera, e.pos, e.pos.add(0, -r, 0), col);
                WorldRenderUtils.drawLine3D(ms, consumers, camera, e.pos, e.pos.add(0, 0, r), col);
                WorldRenderUtils.drawLine3D(ms, consumers, camera, e.pos, e.pos.add(0, 0, -r), col);
            }
            if (damagePopup.getBoolean()) {
                Vec3d txtPos = e.pos.add(0, 0.15 + k * 0.5, 0);
                WorldRenderUtils.drawBillboardText(ms, consumers, camera, mc().textRenderer,
                        "-" + (int) e.damage, txtPos, col);
            }
        }
    }

    private static class Effect {
        final Vec3d pos;
        final float damage;
        final long born = System.currentTimeMillis();

        Effect(Vec3d pos, float damage) {
            this.pos = pos;
            this.damage = damage;
        }
    }
}
