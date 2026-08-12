package ru.fluxvisuals.module.target;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.Animation;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.Theme;
import ru.fluxvisuals.render.WorldRenderUtils;
import ru.fluxvisuals.target.TargetUtils;

/**
 * Кастомный никнейм над прицеленной сущностью. Только цель прицела — никакого
 * отображения игроков сквозь стены (антибан).
 */
public class NametagsModule extends Module {
    private final Setting colorMode = Setting.mode("Color", "Цвет имени", "HP", "Static");
    private final Setting staticColor = Setting.color("Static color", "Цвет имени (Static)", 0xFFFFFFFF);
    private final Setting hpBar = Setting.bool("HP bar", "Полоска HP", true);

    private final Animation visible = new Animation(0, 0.3);
    private LivingEntity lastTarget;

    public NametagsModule() {
        super("Nametags", "Никнеймы над целями", Category.TARGET);
        addSetting(colorMode);
        addSetting(staticColor);
        addSetting(hpBar);
    }

    @Override
    public void onTick() {
        LivingEntity target = TargetUtils.getLivingTarget(mc());
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

        float hp = lastTarget.getHealth();
        float maxHp = lastTarget.getMaxHealth();
        float pct = Math.max(0, Math.min(1, hp / Math.max(1, maxHp)));

        int nameColor = colorMode.getModeIndex() == 1 ? staticColor.getColor() : ColorUtils.healthColor(pct);
        nameColor = ColorUtils.mulAlpha(nameColor, a);

        Vec3d head = WorldRenderUtils.entityPos(lastTarget).add(0, lastTarget.getHeight() + 0.5, 0);
        String name = lastTarget.getDisplayName().getString();
        WorldRenderUtils.drawBillboardText(ms, consumers, camera, mc().textRenderer, name, head, nameColor);

        if (hpBar.getBoolean()) {
            float w = 30f, h = 3f;
            Vec3d barPos = head.add(0, -0.3, 0);
            WorldRenderUtils.drawBillboardBar(ms, consumers, camera, barPos, w, h, ColorUtils.mulAlpha(0x40000000, a));
            Vec3d fillPos = barPos.add(-w / 2f + w * pct / 2f, 0, 0);
            WorldRenderUtils.drawBillboardBar(ms, consumers, camera, fillPos, w * pct, h,
                    ColorUtils.mulAlpha(ColorUtils.healthColor(pct), a));
        }
    }
}
