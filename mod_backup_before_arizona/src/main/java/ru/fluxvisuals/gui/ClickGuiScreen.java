package ru.fluxvisuals.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.fluxvisuals.config.ConfigManager;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.module.ModuleManager;
import ru.fluxvisuals.module.misc.ClickGuiModule;
import ru.fluxvisuals.render.Animation;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.RenderUtils;
import ru.fluxvisuals.render.Theme;

import java.util.List;

/**
 * Полноэкранный GUI клиента (настоящий Screen — курсор и ввод обрабатывает ванилла).
 * Открывается правым Shift (модуль ClickGUI), закрывается правым Shift или Escape.
 */
public class ClickGuiScreen extends Screen {
    private static final float PANEL_X = 24f;
    private static final float PANEL_Y = 40f;
    private static final float CAT_W = 92f;
    private static final float CAT_H = 24f;
    private static final float LIST_W = 240f;
    private static final float MOD_ROW_H = 16f;
    private static final float SET_ROW_H = 15f;

    private static final int[] ACCENT_PRESETS = {0xFFA855F7, 0xFFFF5A6E, 0xFF3B82F6, 0xFF22C55E, 0xFFF59E0B, 0xFF14B8A6};

    private final Animation slideIn = new Animation(0, 0.4);
    private int selectedCategory;
    private int expandedIndex = -1;
    private int hoveredCategory = -1;
    private int hoveredModule = -1;
    private int hoveredSetting = -1;
    private boolean wasShiftDown = true; // инициализируем «нажатым», чтобы правый Shift, которым открыли GUI, не закрыл его сразу

    public ClickGuiScreen() {
        super(Text.literal("FluxVisuals"));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        slideIn.setValue(0);
        slideIn.setTarget(1);
    }

    @Override
    public void close() {
        super.close();
        if (ClickGuiModule.INSTANCE != null) ClickGuiModule.INSTANCE.disable();
    }

    // ------------------------------------------------------------------
    // Рендер
    // ------------------------------------------------------------------

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        slideIn.tick();

        // правый Shift закрывает GUI (edge-детекция; надёжнее, чем keyPressed для модификаторов)
        boolean shiftDown = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (shiftDown && !wasShiftDown) {
            close();
            return;
        }
        wasShiftDown = shiftDown;

        float a = (float) slideIn.getValue();
        if (a < 0.01f) return;

        // затемнение фона + мягкая виньетка
        RenderUtils.fill(g, 0, 0, width, height, ColorUtils.withAlpha(0x000000, (int) (150 * a)));

        float slideX = (PANEL_X + CAT_W + LIST_W + 60) * (1f - a);
        float panelX = PANEL_X + slideX;

        drawCategories(g, panelX, PANEL_Y, a, mouseX, mouseY);
        drawModules(g, panelX + CAT_W + 8, PANEL_Y, a, mouseX, mouseY);
    }

    private void drawCategories(DrawContext g, float x, float y, float alpha, int mx, int my) {
        // фон панели категорий
        float h = Category.values().length * (CAT_H + 5f) + 10f;
        RenderUtils.drawShadow(g, x, y, CAT_W, h, 6f, 90);
        RenderUtils.fill(g, x, y, CAT_W, h, ColorUtils.mulAlpha(Theme.bg(), alpha));
        RenderUtils.fill(g, x, y, CAT_W, 2f, ColorUtils.mulAlpha(Theme.accent(), alpha));

        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            float ty = y + 8f + i * (CAT_H + 5f);
            boolean selected = i == selectedCategory;
            boolean hovered = mx >= x + 4f && mx <= x + CAT_W - 4f && my >= ty && my <= ty + CAT_H;
            if (hovered) hoveredCategory = i;

            if (selected) {
                RenderUtils.fill(g, x + 4f, ty, CAT_W - 8f, CAT_H, ColorUtils.mulAlpha(Theme.bgBright(), alpha));
                RenderUtils.fill(g, x + 4f, ty, 2.5f, CAT_H, ColorUtils.mulAlpha(Theme.categoryColor(cats[i]), alpha));
            } else if (hovered) {
                RenderUtils.fill(g, x + 4f, ty, CAT_W - 8f, CAT_H, ColorUtils.mulAlpha(0xFFFFFFFF, 12 * alpha));
            }
            RenderUtils.textShadow(g, cats[i].displayName, x + 14f, ty + (CAT_H - 9f) / 2f,
                    ColorUtils.mulAlpha(selected ? Theme.text() : Theme.textDim(), alpha));
        }
    }

    private void drawModules(DrawContext g, float x, float y, float alpha, int mx, int my) {
        List<Module> modules = ModuleManager.INSTANCE.getByCategory(Category.values()[selectedCategory]);
        float total = 8f;
        for (Module m : modules) {
            total += MOD_ROW_H + 3f;
            if (expandedIndex == modules.indexOf(m) && !m.getSettings().isEmpty())
                total += m.getSettings().size() * SET_ROW_H;
        }
        total += 8f;

        RenderUtils.drawShadow(g, x, y, LIST_W, total, 6f, 90);
        RenderUtils.fill(g, x, y, LIST_W, total, ColorUtils.mulAlpha(Theme.bg(), alpha));

        float curY = y + 6f;
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            float rowH = MOD_ROW_H;
            boolean expanded = expandedIndex == i && !m.getSettings().isEmpty();
            if (expanded) for (Setting s : m.getSettings()) rowH += SET_ROW_H;

            boolean hovered = mx >= x + 4f && mx <= x + LIST_W - 4f && my >= curY && my <= curY + MOD_ROW_H;
            if (hovered) hoveredModule = i;

            // строка модуля
            int accent = m.isEnabled() ? Theme.categoryColor(m.category) : Theme.textDim();
            if (m.isEnabled()) {
                RenderUtils.fill(g, x + 4f, curY, LIST_W - 8f, MOD_ROW_H, ColorUtils.mulAlpha(Theme.bgBright(), alpha));
                RenderUtils.fill(g, x + 4f, curY, 2.5f, MOD_ROW_H, ColorUtils.mulAlpha(accent, alpha));
            } else if (hovered) {
                RenderUtils.fill(g, x + 4f, curY, LIST_W - 8f, MOD_ROW_H, ColorUtils.mulAlpha(0xFFFFFFFF, 10 * alpha));
            }
            RenderUtils.textShadow(g, m.name, x + 13f, curY + (MOD_ROW_H - 9f) / 2f,
                    ColorUtils.mulAlpha(Theme.text(), alpha));
            RenderUtils.text(g, m.isEnabled() ? "ON" : "OFF", x + LIST_W - 34f, curY + (MOD_ROW_H - 9f) / 2f,
                    ColorUtils.mulAlpha(accent, alpha));

            // стрелка «настройки», если есть
            if (!m.getSettings().isEmpty()) {
                RenderUtils.text(g, expanded ? "▾" : "▸", x + LIST_W - 16f, curY + (MOD_ROW_H - 9f) / 2f,
                        ColorUtils.mulAlpha(Theme.textDim(), alpha));
            }
            curY += MOD_ROW_H + 3f;

            // настройки
            if (expanded) {
                for (Setting s : m.getSettings()) {
                    boolean sh = mx >= x + 8f && mx <= x + LIST_W - 8f && my >= curY && my <= curY + SET_ROW_H;
                    if (sh) hoveredSetting = s.hashCode();
                    RenderUtils.fill(g, x + 8f, curY, LIST_W - 16f, SET_ROW_H, ColorUtils.mulAlpha(0x000000, (int) (60 * alpha)));
                    RenderUtils.text(g, s.name, x + 14f, curY + (SET_ROW_H - 9f) / 2f, ColorUtils.mulAlpha(Theme.textDim(), alpha));
                    RenderUtils.textShadow(g, displayValue(s), x + LIST_W - 90f, curY + (SET_ROW_H - 9f) / 2f,
                            ColorUtils.mulAlpha(Theme.text(), alpha));
                    curY += SET_ROW_H;
                }
            }
        }
    }

    private String displayValue(Setting s) {
        return switch (s.type) {
            case BOOLEAN -> s.getBoolean() ? "§a✓" : "§c✗";
            case INT -> String.valueOf(s.getInt());
            case FLOAT -> String.format("%.1f", s.getFloat());
            case MODE -> s.getMode();
            case COLOR -> String.format("#%06X", s.getColor() & 0xFFFFFF);
            case STRING -> s.getString();
        };
    }

    // ------------------------------------------------------------------
    // Ввод
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mx0 = click.comp_4798(); // x
        double my0 = click.comp_4799(); // y
        int button = click.button();
        float x = PANEL_X + (PANEL_X + CAT_W + LIST_W + 60) * (1f - (float) slideIn.getValue());
        float mx = (float) mx0, my = (float) my0;

        // категории
        if (mx >= x && mx <= x + CAT_W) {
            for (int i = 0; i < Category.values().length; i++) {
                float ty = PANEL_Y + 8f + i * (CAT_H + 5f);
                if (my >= ty && my <= ty + CAT_H) {
                    if (button == 0) { selectedCategory = i; expandedIndex = -1; }
                    return true;
                }
            }
        }

        // модули
        float modX = x + CAT_W + 8f;
        if (mx >= modX && mx <= modX + LIST_W) {
            List<Module> modules = ModuleManager.INSTANCE.getByCategory(Category.values()[selectedCategory]);
            float curY = PANEL_Y + 6f;
            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);
                float rowH = MOD_ROW_H;
                boolean expanded = expandedIndex == i && !m.getSettings().isEmpty();
                if (expanded) for (Setting s : m.getSettings()) rowH += SET_ROW_H;

                if (my >= curY && my <= curY + MOD_ROW_H) {
                    if (button == 0) m.toggle();
                    else if (button == 1 && !m.getSettings().isEmpty())
                        expandedIndex = expandedIndex == i ? -1 : i;
                    return true;
                }
                curY += MOD_ROW_H + 3f;

                if (expanded) {
                    for (Setting s : m.getSettings()) {
                        if (my >= curY && my <= curY + SET_ROW_H) {
                            if (button == 0) clickSetting(s);
                            else if (button == 1) clickSetting(s); // у правого клика — уменьшение в toggleSetting
                            return true;
                        }
                        curY += SET_ROW_H;
                    }
                }
            }
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyInput key) {
        if (key.isEscape()) {
            close();
            return true;
        }
        return super.keyPressed(key);
    }

    @Override
    public boolean mouseReleased(Click click) {
        return true;
    }

    private void clickSetting(Setting s) {
        switch (s.type) {
            case BOOLEAN -> s.setBoolean(!s.getBoolean());
            case INT -> s.setInt(Math.min((int) s.max, s.getInt() + 1));
            case FLOAT -> s.setFloat(Math.min(s.max, s.getFloat() + 0.1f));
            case MODE -> s.cycleMode();
            case COLOR -> {
                int cur = ConfigManager.INSTANCE.accent;
                int idx = 0;
                for (int i = 0; i < ACCENT_PRESETS.length; i++) if (ACCENT_PRESETS[i] == cur) { idx = i; break; }
                ConfigManager.INSTANCE.accent = ACCENT_PRESETS[(idx + 1) % ACCENT_PRESETS.length];
            }
            default -> {}
        }
    }

}
