package ru.fluxvisuals.module.hud;

import net.minecraft.client.gui.DrawContext;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.hud.HudElements;
import ru.fluxvisuals.hud.HudLayout;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.Theme;

/** База для простых текстовых HUD-строк, укладывающихся в раскладку. */
public abstract class SimpleHudModule extends Module {
    protected final Setting position = Setting.mode("Position", "Расположение", "Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right");
    protected final Setting background = Setting.bool("Background", "Фоновая панель", true);

    public SimpleHudModule(String name, String description) {
        super(name, description, Category.HUD);
        addSetting(position);
        addSetting(background);
    }

    protected abstract String getText();

    protected int getAccent() { return Theme.accent(); }

    @Override
    public void onRender2D(DrawContext g, float tickDelta) {
        String text = getText();
        if (text == null || text.isEmpty()) return;
        HudLayout.Anchor anchor = HudLayout.fromIndex(position.getModeIndex());
        float w = HudElements.rowWidth(text);
        HudLayout.INSTANCE.push(g, anchor, w, HudElements.ROW_HEIGHT, p ->
                HudElements.drawRow(g, text, p[0], p[1], w, getAccent(), background.getBoolean()));
    }
}
