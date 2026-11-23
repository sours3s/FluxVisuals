package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.ui.fluxgui.client.gui.hud.Hud;
import ru.fluxvisuals.ui.fluxgui.client.gui.hud.HudElement;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.RoundedRenderer;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.TextRender;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/**
 * Список активных модулей. Полноценный элемент HUD: жидкое стекло, наш шрифт,
 * стили и перемещение через редактор — как у остальных элементов интерфейса.
 */
@Environment(EnvType.CLIENT)
public final class ArrayListHUD implements HudElement {
    public static final ArrayListHUD INSTANCE = new ArrayListHUD();

    public static final BooleanSetting sortByWidth = new BooleanSetting("Sort by width", true);
    public static final BooleanSetting smooth = new BooleanSetting("Smooth animation", true);

    private static final float Y = 12.0F;
    private static final float RIGHT_INSET = 8.0F;
    private static final float ROW_HEIGHT = 16.0F;
    private static final float ROW_GAP = 3.0F;
    private static final float PAD_X = 9.0F;
    private static final float PAD_Y = 8.0F;
    private static final float MARK_WIDTH = 2.0F;
    private static final float MARK_GAP = 7.0F;
    private static final float MIN_WIDTH = 116.0F;
    private static final float MAX_WIDTH = 260.0F;

    private static final List<Module> VISIBLE = new ArrayList<>();
    private static float currentWidth = MIN_WIDTH;

    private ArrayListHUD() {
    }

    @Override
    public String id() {
        return "arraylist";
    }

    @Override
    public float x() {
        return Hud.DESIGN_WIDTH - currentWidth - RIGHT_INSET;
    }

    @Override
    public float y() {
        return Y;
    }

    @Override
    public float width() {
        return currentWidth;
    }

    @Override
    public float height() {
        return PAD_Y * 2.0F + Math.max(1, VISIBLE.size()) * ROW_HEIGHT + Math.max(0, VISIBLE.size() - 1) * ROW_GAP;
    }

    @Override
    public boolean hasContent() {
        return !active().isEmpty();
    }

    @Override
    public void render(DrawContext context, Hud hud) {
        if (FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
            return;
        }

        for (Module module : FluxVisualsClient.get.manager.getModules()) {
            module.mAnim.update();
        }

        VISIBLE.clear();
        VISIBLE.addAll(active());
        if (VISIBLE.isEmpty()) {
            return;
        }

        if (sortByWidth.get()) {
            VISIBLE.sort(Comparator.comparingDouble((Module module) -> TextRender.width(module.name, Hud.ROW_SIZE)).reversed());
        } else {
            VISIBLE.sort(Comparator.comparingDouble((Module module) -> -module.mAnim.getValue()));
        }

        float targetWidth = MIN_WIDTH;
        for (Module module : VISIBLE) {
            targetWidth = Math.max(targetWidth, PAD_X * 2.0F + MARK_WIDTH + MARK_GAP + TextRender.width(module.name, Hud.ROW_SIZE));
        }
        targetWidth = Math.min(MAX_WIDTH, targetWidth);
        currentWidth = MathHelper.lerp(0.12F, currentWidth, targetWidth);
        if (Math.abs(currentWidth - targetWidth) < 0.05F) {
            currentWidth = targetWidth;
        }

        float left = x();
        float top = y();
        hud.panel(context, left, top, currentWidth, height(), 10.0F);

        int index = 0;
        for (Module module : VISIBLE) {
            float rowY = top + PAD_Y + index * (ROW_HEIGHT + ROW_GAP);
            float alpha = smooth.get() ? MathHelper.clamp((float)module.mAnim.getValue(), 0.0F, 1.0F) : 1.0F;
            int rowColor = Hud.fade(Hud.styledColor(Hud.TEXT_ROW), Hud.animatedOpacity() * alpha);
            RoundedRenderer.drawRoundedRect(
                  context, left + PAD_X, rowY, MARK_WIDTH, ROW_HEIGHT, MARK_WIDTH / 2.0F,
                  Hud.fade(Hud.accent(), Hud.animatedOpacity() * alpha)
            );
            TextRender.draw(context, module.name, left + PAD_X + MARK_WIDTH + MARK_GAP, rowY + 12.0F, Hud.ROW_SIZE, rowColor);
            index++;
        }
    }

    private static List<Module> active() {
        List<Module> result = new ArrayList<>();
        if (FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
            return result;
        }
        for (Module module : FluxVisualsClient.get.manager.getModules()) {
            if (module.enable || module.mAnim.getValue() > 0.001) {
                result.add(module);
            }
        }
        return result;
    }
}
