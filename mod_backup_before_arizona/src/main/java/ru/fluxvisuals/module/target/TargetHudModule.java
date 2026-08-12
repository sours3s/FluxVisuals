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

/** Панель с информацией о прицеленной сущности (полностью настраивается). */
public class TargetHudModule extends Module {
    private final Setting position = Setting.mode("Position", "Расположение", "Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right");
    private final Setting scale = Setting.float_("Scale", "Масштаб панели", 1f, 0.5f, 1.6f);
    private final Setting barStyle = Setting.mode("HP bar", "Стиль полосы HP", "Gradient", "Solid", "Vertical");
    private final Setting showArmor = Setting.bool("Armor", "Показывать броню", true);
    private final Setting showDistance = Setting.bool("Distance", "Показывать дистанцию", true);
    private final Setting showName = Setting.bool("Name", "Показывать имя", true);

    private final Animation visible = new Animation(0, 0.35);
    private LivingEntity lastTarget;

    public TargetHudModule() {
        super("TargetHUD", "Информация о прицеленной сущности", Category.TARGET);
        addSetting(position);
        addSetting(scale);
        addSetting(barStyle);
        addSetting(showArmor);
        addSetting(showDistance);
        addSetting(showName);
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

        float s = scale.getFloat();
        float w = 150f * s, h = 34f * s;
        int sw = mc().getWindow().getScaledWidth();
        int sh = mc().getWindow().getScaledHeight();
        float x = 4f, y = 4f;
        switch (position.getModeIndex()) {
            case 1 -> { x = sw - w - 4f; }
            case 2 -> { y = sh - h - 4f; }
            case 3 -> { x = sw - w - 4f; y = sh - h - 4f; }
        }

        float hp = lastTarget.getHealth();
        float maxHp = lastTarget.getMaxHealth();
        float pct = Math.max(0, Math.min(1, hp / Math.max(1, maxHp)));
        int hpColor = ColorUtils.healthColor(pct);

        // фон и рамка
        RenderUtils.drawRoundedRect(g, x, y, w, h, 5f * s, ColorUtils.mulAlpha(Theme.bg(), a));
        RenderUtils.drawRoundedOutline(g, x, y, w, h, 5f * s, 1f, ColorUtils.mulAlpha(Theme.border(), a));

        // имя
        if (showName.getBoolean()) {
            String name = lastTarget.getDisplayName().getString();
            RenderUtils.textShadow(g, name, x + 8f * s, y + 6f * s, ColorUtils.mulAlpha(Theme.text(), a));
        }
        // дистанция
        if (showDistance.getBoolean()) {
            String dist = String.format("%.1fm", TargetUtils.distanceTo(mc(), lastTarget));
            RenderUtils.textShadow(g, dist, x + w - RenderUtils.textWidth(dist) - 8f * s, y + 6f * s,
                    ColorUtils.mulAlpha(Theme.textDim(), a));
        }

        // полоса HP
        float barW = 134f * s, barH = 5f * s, by = y + 20f * s;
        RenderUtils.drawRoundedRect(g, x + 8f * s, by, barW, barH, 2.5f * s, ColorUtils.mulAlpha(0xFF000000, 0.35f * a));
        int fill = switch (barStyle.getModeIndex()) {
            case 1 -> hpColor;
            case 2 -> ColorUtils.mix(hpColor, Theme.accent(), 0.5f);
            default -> ColorUtils.mix(hpColor, ColorUtils.withAlpha(hpColor, 0), 0.25f);
        };
        RenderUtils.drawRoundedRect(g, x + 8f * s, by, barW * pct, barH, 2.5f * s, ColorUtils.mulAlpha(fill, a));

        // полоса брони
        if (showArmor.getBoolean()) {
            float armorPct = Math.max(0, Math.min(1, lastTarget.getArmor() / 20f));
            RenderUtils.drawRoundedRect(g, x + 8f * s, by + barH + 2f * s, barW, 2f * s, 1f * s, ColorUtils.mulAlpha(0xFF000000, 0.35f * a));
            RenderUtils.drawRoundedRect(g, x + 8f * s, by + barH + 2f * s, barW * armorPct, 2f * s, 1f * s, ColorUtils.mulAlpha(0xFF8FB8FF, a));
        }
    }
}
