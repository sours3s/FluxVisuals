package ru.fluxvisuals.module.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.Registries;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.hud.HudLayout;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.RenderUtils;
import ru.fluxvisuals.render.Theme;

/** Активные эффекты зелий с таймерами. */
public class PotionHudModule extends Module {
    private final Setting position = Setting.mode("Position", "Расположение", "Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right");

    public PotionHudModule() {
        super("PotionHUD", "Активные эффекты", Category.HUD);
        addSetting(position);
    }

    @Override
    public void onRender2D(DrawContext g, float tickDelta) {
        var p = mc().player;
        if (p == null) return;
        var effects = p.getActiveStatusEffects();
        if (effects.isEmpty()) return;

        float rowH = 11f;
        float w = 86f;
        float h = effects.size() * rowH + 4f;
        HudLayout.INSTANCE.push(g, HudLayout.fromIndex(position.getModeIndex()), w, h, pos -> {
            float x = pos[0], y = pos[1];
            RenderUtils.drawRoundedRect(g, x, y, w, h, 4f, Theme.bg());
            int i = 0;
            for (var entry : effects.entrySet()) {
                var inst = entry.getValue();
                var effect = inst.getEffectType().getKeyOrValue().right().orElse(null);
                if (effect == null) continue;
                var id = Registries.STATUS_EFFECT.getId(effect);
                String name = id == null ? "?" : id.getPath().replace('_', ' ');
                String time = fmt(inst.getDuration() / 20);
                int amp = inst.getAmplifier() + 1;
                float ry = y + 2f + i * rowH;
                int color = effect.getColor();
                RenderUtils.fill(g, x + 4f, ry + 2f, 6f, 6f, color);
                RenderUtils.textShadow(g, name + (amp > 1 ? " " + amp : ""), x + 14f, ry + 1f, Theme.text());
                RenderUtils.text(g, time, x + w - RenderUtils.textWidth(time) - 5f, ry + 1f, Theme.textDim());
                i++;
            }
        });
    }

    private String fmt(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
