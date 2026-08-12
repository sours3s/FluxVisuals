package ru.fluxvisuals.module.visual;

import net.minecraft.client.gui.DrawContext;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.RenderUtils;
import ru.fluxvisuals.render.Theme;
import ru.fluxvisuals.target.TargetUtils;

/** Кастомный прицел с подсветкой при наведении на сущность. */
public class CrosshairModule extends Module {
    private final Setting style = Setting.mode("Style", "Стиль", "Dot", "Cross", "Ring");
    private final Setting color = Setting.color("Color", "Цвет", 0xFFFFFFFF);

    public CrosshairModule() {
        super("Crosshair", "Кастомный прицел", Category.VISUAL);
        addSetting(style);
        addSetting(color);
    }

    @Override
    public boolean shouldList() { return false; }

    @Override
    public void onRender2D(DrawContext g, float tickDelta) {
        int sw = mc().getWindow().getScaledWidth();
        int sh = mc().getWindow().getScaledHeight();
        float cx = sw / 2f, cy = sh / 2f;
        int mode = style.getModeIndex();
        boolean hasTarget = TargetUtils.getEntity(mc()) != null;
        int col = hasTarget ? Theme.accent() : color.getColor();

        switch (mode) {
            case 1 -> {
                float s = hasTarget ? 5f : 4f;
                RenderUtils.drawLine2D(g, cx - s, cy, cx - 2f, cy, 1.5f, col);
                RenderUtils.drawLine2D(g, cx + 2f, cy, cx + s, cy, 1.5f, col);
                RenderUtils.drawLine2D(g, cx, cy - s, cx, cy - 2f, 1.5f, col);
                RenderUtils.drawLine2D(g, cx, cy + 2f, cx, cy + s, 1.5f, col);
            }
            case 2 -> {
                float r = hasTarget ? 6f : 5f;
                RenderUtils.drawCircleOutline(g, cx, cy, r, 1.5f, col, 24);
            }
            default -> {
                float r = hasTarget ? 2.5f : 2f;
                RenderUtils.fill(g, cx - r, cy - r, r * 2, r * 2, col);
            }
        }
    }
}
