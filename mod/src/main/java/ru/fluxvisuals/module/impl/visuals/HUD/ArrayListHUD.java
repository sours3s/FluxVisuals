package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;

/** Список активных модулей в стиле исходного FluxVisuals: 12 px строки, правый верхний угол. */
@Environment(EnvType.CLIENT)
public final class ArrayListHUD {
    public static final BooleanSetting sortByWidth = new BooleanSetting("Sort by width", true);
    public static final BooleanSetting smooth = new BooleanSetting("Smooth animation", true);

    private static final int ROW_HEIGHT = 12;
    private static final int PADDING = 4;

    private static final List<Module> visible = new ArrayList<>();

    private ArrayListHUD() {
    }

    public static boolean hasContent() {
        if (FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null) {
            for (Module module : FluxVisualsClient.get.manager.getModules()) {
                if (module.enable || module.mAnim.getValue() > 0.001) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void arraylist(DrawContext context) {
        if (FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
            return;
        }

        visible.clear();
        for (Module module : FluxVisualsClient.get.manager.getModules()) {
            module.mAnim.update();
            if (module.enable || module.mAnim.getValue() > 0.001) {
                visible.add(module);
            }
        }
        if (visible.isEmpty()) {
            return;
        }

        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        if (sortByWidth.get()) {
            visible.sort(Comparator.comparingDouble((Module m) -> font.getWidth(m.name)).reversed());
        } else {
            visible.sort(Comparator.comparingDouble((Module m) -> -m.mAnim.getValue()));
        }

        int y = PADDING;
        int scaledWidth = context.getScaledWindowWidth();
        for (Module module : visible) {
            float alpha = (float) (smooth.get() ? module.mAnim.getValue() : 1.0);
            if (alpha <= 0.01F) {
                continue;
            }
            int rowWidth = font.getWidth(module.name) + 12;
            int x = scaledWidth - PADDING - rowWidth;
            int bottom = y + ROW_HEIGHT;

            context.fill(x, y, x + rowWidth, bottom,
                  DonorHudTheme.withAlpha(DonorHudTheme.BG, (int) (DonorHudTheme.BG_ALPHA * alpha)));
            int accent = DonorHudTheme.mulAlpha(DonorHudTheme.category(module.category), alpha);
            context.fill(x, y, x + 2, bottom, accent);
            context.drawTextWithShadow(font, module.name, x + 6, y + 1,
                  DonorHudTheme.mulAlpha(DonorHudTheme.TEXT, alpha));
            y += ROW_HEIGHT;
        }
    }

    public static void renderEmpty(DrawContext context) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int rowWidth = font.getWidth("ArrayList") + 12;
        int x = context.getScaledWindowWidth() - PADDING - rowWidth;
        context.fill(x, PADDING, x + rowWidth, PADDING + ROW_HEIGHT,
              DonorHudTheme.withAlpha(DonorHudTheme.BG, 90));
        context.fill(x, PADDING, x + 2, PADDING + ROW_HEIGHT, DonorHudTheme.ACCENT);
        context.drawTextWithShadow(font, "ArrayList", x + 6, PADDING + 1,
              DonorHudTheme.withAlpha(DonorHudTheme.TEXT, 120));
    }
}
