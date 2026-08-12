package ru.fluxvisuals.module.target;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.Animation;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.RenderUtils;
import ru.fluxvisuals.render.Theme;
import ru.fluxvisuals.target.TargetUtils;

/** Компактная версия TargetHUD: имя + HP. */
public class TargetInfoModule extends Module {
    private final Setting position = Setting.mode("Position", "Расположение", "Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right");
    private final Animation visible = new Animation(0, 0.4);
    private LivingEntity lastTarget;

    public TargetInfoModule() {
        super("TargetInfo", "Компактная информация о цели", Category.TARGET);
        addSetting(position);
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
    public void onRender2D(DrawContext g, float tickDelta) {
        float a = (float) visible.getValue();
        if (a < 0.01f || lastTarget == null || !lastTarget.isAlive()) return;

        String name = lastTarget.getDisplayName().getString();
        float hp = lastTarget.getHealth();
        float maxHp = lastTarget.getMaxHealth();
        float pct = Math.max(0, Math.min(1, hp / Math.max(1, maxHp)));
        String text = name + " §7" + (int) hp + "❤";

        float w = RenderUtils.textWidth(text) + 14f;
        float h = 17f;
        int sw = mc().getWindow().getScaledWidth();
        int sh = mc().getWindow().getScaledHeight();
        float x = 4f, y = 4f;
        switch (position.getModeIndex()) {
            case 1 -> { x = sw - w - 4f; }
            case 2 -> { y = sh - h - 4f; }
            case 3 -> { x = sw - w - 4f; y = sh - h - 4f; }
        }

        RenderUtils.drawRoundedRect(g, x, y, w, h, 4f, ColorUtils.mulAlpha(Theme.bg(), a));
        RenderUtils.drawRoundedRect(g, x, y + h - 2f, w, 2f, 1f, ColorUtils.mulAlpha(ColorUtils.healthColor(pct), a));
        RenderUtils.textShadow(g, text, x + 7f, y + 4f, ColorUtils.mulAlpha(Theme.text(), a));
    }
}
