package ru.fluxvisuals.module.target;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.Animation;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.Theme;
import ru.fluxvisuals.render.WorldRenderUtils;
import ru.fluxvisuals.target.TargetUtils;

/**
 * Подсветка ТОЛЬКО текущей цели прицела. Никаких боксов «вокруг всех игроков
 * сквозь стены» — поэтому бана неоткуда взяться.
 */
public class EspModule extends Module {
    private final Setting colorMode = Setting.mode("Color", "Цвет", "HP", "Static", "Accent");
    private final Setting staticColor = Setting.color("Static color", "Цвет (Static)", 0xFFFFFFFF);

    private final Animation visible = new Animation(0, 0.3);
    private Entity lastTarget;

    public EspModule() {
        super("ESP", "Подсветка цели прицела", Category.TARGET);
        addSetting(colorMode);
        addSetting(staticColor);
    }

    @Override
    public void onTick() {
        Entity target = TargetUtils.getEntity(mc());
        if (target != null && target.isAlive()) {
            visible.setTarget(1);
            lastTarget = target;
        } else {
            visible.setTarget(0);
        }
        visible.tick();
    }

    @Override
    public void onRender3D(MatrixStack ms, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        float a = (float) visible.getValue();
        if (a < 0.01f || lastTarget == null || !lastTarget.isAlive()) return;

        int color;
        switch (colorMode.getModeIndex()) {
            case 1 -> color = staticColor.getColor();
            case 2 -> color = Theme.accent();
            default -> {
                if (lastTarget instanceof LivingEntity le) {
                    color = ColorUtils.healthColor(Math.max(0, Math.min(1, le.getHealth() / Math.max(1, le.getMaxHealth()))));
                } else {
                    color = Theme.accent();
                }
            }
        }
        color = ColorUtils.mulAlpha(color, a);
        WorldRenderUtils.drawBoxOutline3D(ms, consumers, camera, lastTarget.getBoundingBox(), color);
    }
}
