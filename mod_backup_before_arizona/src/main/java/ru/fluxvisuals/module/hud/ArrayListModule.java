package ru.fluxvisuals.module.hud;

import net.minecraft.client.gui.DrawContext;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.hud.HudLayout;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.module.ModuleManager;
import ru.fluxvisuals.render.Animation;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.RenderUtils;
import ru.fluxvisuals.render.Theme;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Список активных модулей справа сверху, уложенный в HudLayout (не перекрывает другие). */
public class ArrayListModule extends Module {
    private static final float ROW_H = 12f;

    private final Setting background = Setting.bool("Background", "Фоновая панель", true);
    private final Map<String, Animation> anims = new HashMap<>();

    public ArrayListModule() {
        super("ArrayList", "Список активных модулей", Category.HUD);
        addSetting(background);
        setEnabled(true);
    }

    @Override
    public void onRender2D(DrawContext g, float tickDelta) {
        for (Module m : ModuleManager.INSTANCE.getAll()) {
            Animation a = anims.computeIfAbsent(m.name, n -> new Animation(0, 0.25));
            a.setTarget(m.isEnabled() && m.shouldList() ? 1 : 0);
            a.tick();
        }

        List<Module> visible = ModuleManager.INSTANCE.getAll().stream()
                .filter(m -> anims.containsKey(m.name) && anims.get(m.name).getValue() > 0.01)
                .sorted(Comparator.comparingDouble((Module m) -> -anims.get(m.name).getValue()))
                .toList();
        if (visible.isEmpty()) return;

        float maxW = (float) visible.stream()
                .mapToDouble(m -> RenderUtils.textWidth(m.name) + 12f).max().orElse(0f);
        float total = Math.max(0f, visible.size() * ROW_H - 2f);

        HudLayout.Anchor anchor = HudLayout.Anchor.TOP_RIGHT;
        HudLayout.INSTANCE.push(g, anchor, maxW, total, p -> {
            float right = p[0] + maxW;
            float y = p[1];
            for (Module m : visible) {
                Animation a = anims.get(m.name);
                float alpha = (float) a.getValue();
                float w = RenderUtils.textWidth(m.name) + 12f;
                float rowX = right - w;
                int accent = Theme.categoryColor(m.category);
                int textC = ColorUtils.withAlpha(Theme.text(), (int) (255 * alpha));
                if (background.getBoolean()) {
                    int bgC = ColorUtils.withAlpha(Theme.bg(), (int) (255 * alpha));
                    RenderUtils.drawRoundedRect(g, rowX, y, w, ROW_H, 3f, bgC);
                    RenderUtils.fill(g, rowX, y + 1f, 2f, ROW_H - 2f, ColorUtils.mulAlpha(accent, alpha));
                } else {
                    RenderUtils.fill(g, rowX, y + 1f, 2f, ROW_H - 2f, ColorUtils.mulAlpha(accent, alpha));
                }
                RenderUtils.textShadow(g, m.name, rowX + 6f, y + 1.5f, textC);
                y += ROW_H;
            }
        });
    }
}
