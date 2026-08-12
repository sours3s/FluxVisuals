package ru.fluxvisuals.module.target;

import net.minecraft.client.gui.DrawContext;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.RenderUtils;
import ru.fluxvisuals.target.HitTracker;

import java.util.ArrayList;
import java.util.List;

/** Маркер у прицела при попадании. */
public class HitmarkersModule extends Module {
    private static final long LIFETIME = 450;

    private final Setting style = Setting.mode("Style", "Стиль маркера", "Cross", "Dot", "Lines");
    private final Setting color = Setting.color("Color", "Цвет маркера", 0xFFFFFFFF);

    private final List<Long> markers = new ArrayList<>();

    public HitmarkersModule() {
        super("Hitmarkers", "Маркер при попадании", Category.TARGET);
        addSetting(style);
        addSetting(color);
        HitTracker.register((entity, pos, dmg) -> {
            if (isEnabled()) markers.add(System.currentTimeMillis());
        });
    }

    @Override
    public void onRender2D(DrawContext g, float tickDelta) {
        long now = System.currentTimeMillis();
        markers.removeIf(t -> now - t > LIFETIME);
        if (markers.isEmpty()) return;

        int sw = mc().getWindow().getScaledWidth();
        int sh = mc().getWindow().getScaledHeight();
        float cx = sw / 2f, cy = sh / 2f;
        int mode = style.getModeIndex();

        for (long t : markers) {
            float age = (now - t) / (float) LIFETIME;
            float alpha = 1f - age;
            float size = 3f + age * 7f;
            int col = ColorUtils.mulAlpha(color.getColor(), alpha);

            switch (mode) {
                case 1 -> RenderUtils.drawCircleOutline(g, cx, cy, size, 1.5f, col, 24);
                case 2 -> {
                    RenderUtils.drawLine2D(g, cx - size, cy, cx - size * 0.35f, cy, 1.5f, col);
                    RenderUtils.drawLine2D(g, cx + size * 0.35f, cy, cx + size, cy, 1.5f, col);
                    RenderUtils.drawLine2D(g, cx, cy - size, cx, cy - size * 0.35f, 1.5f, col);
                    RenderUtils.drawLine2D(g, cx, cy + size * 0.35f, cx, cy + size, 1.5f, col);
                }
                default -> {
                    RenderUtils.drawLine2D(g, cx - size, cy - size, cx - size * 0.3f, cy - size * 0.3f, 1.5f, col);
                    RenderUtils.drawLine2D(g, cx + size * 0.3f, cy - size * 0.3f, cx + size, cy - size, 1.5f, col);
                    RenderUtils.drawLine2D(g, cx + size * 0.3f, cy + size * 0.3f, cx + size, cy + size, 1.5f, col);
                    RenderUtils.drawLine2D(g, cx - size, cy + size, cx - size * 0.3f, cy + size * 0.3f, 1.5f, col);
                }
            }
        }
    }
}
