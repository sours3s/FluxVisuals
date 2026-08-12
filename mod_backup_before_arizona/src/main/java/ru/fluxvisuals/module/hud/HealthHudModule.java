package ru.fluxvisuals.module.hud;

import net.minecraft.client.gui.DrawContext;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.hud.HudLayout;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.RenderUtils;
import ru.fluxvisuals.render.Theme;

/** Текущее здоровье: число + полоска. */
public class HealthHudModule extends Module {
    private final Setting position = Setting.mode("Position", "Расположение", "Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right");

    public HealthHudModule() {
        super("HealthHUD", "Здоровье игрока", Category.HUD);
        addSetting(position);
    }

    @Override
    public void onRender2D(DrawContext g, float tickDelta) {
        var p = mc().player;
        if (p == null) return;
        float max = p.getMaxHealth();
        float hp = p.getHealth();
        float w = 84f, h = 16f;
        HudLayout.INSTANCE.push(g, HudLayout.fromIndex(position.getModeIndex()), w, h, pos -> {
            float x = pos[0], y = pos[1];
            RenderUtils.drawRoundedRect(g, x, y, w, h, 4f, Theme.bg());
            RenderUtils.textShadow(g, "HP " + (int) hp + "/" + (int) max, x + 5f, y + 2.5f, Theme.text());
            float barW = w - 10f, barH = 3f;
            float by = y + h - 6f;
            RenderUtils.drawRoundedRect(g, x + 5f, by, barW, barH, 1.5f, ColorUtils.withAlpha(0x000000, 110));
            float pct = Math.max(0, Math.min(1, hp / max));
            RenderUtils.drawRoundedRect(g, x + 5f, by, barW * pct, barH, 1.5f, ColorUtils.healthColor(pct));
        });
    }
}
