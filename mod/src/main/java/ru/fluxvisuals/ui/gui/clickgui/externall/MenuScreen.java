package ru.fluxvisuals.ui.gui.clickgui.externall;

import ru.fluxvisuals.ui.gui.clickgui.externall.Client;
import ru.fluxvisuals.ui.gui.clickgui.externall.lang.Lang;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.*;
import ru.fluxvisuals.ui.gui.clickgui.externall.animation.Easings;
import ru.fluxvisuals.ui.gui.clickgui.externall.animation.Animation;
import ru.fluxvisuals.ui.gui.clickgui.externall.colors.ColorUtil;
import ru.fluxvisuals.ui.gui.clickgui.externall.math.Keyboard;
import ru.fluxvisuals.ui.gui.clickgui.externall.math.MathUtil;
import ru.fluxvisuals.ui.gui.clickgui.externall.other.GuiSounds;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.Draw;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.GuiBackgroundRenderer;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.Render2D;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.RenderUtil;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.Scissor;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.ScreenBlur;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.font.Font;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class MenuScreen extends Screen {

    public static float S = 1.0F;

    private static final Identifier STRELOCHKA_TEX = Identifier.of("client", "textures/strelochka.png");

    private final GuiBackgroundRenderer backgroundRenderer = new GuiBackgroundRenderer();
    private final Animation globalAnim = new Animation().run(0, 0);

    // 5 Categories mapped to 5 columns (FluxVisuals categories)
    private static final Category[] CATEGORIES = {
            Category.Visuals,
            Category.Utils,
            Category.Configs,
            Category.Friends,
            Category.Cosmetics
    };

    // Scrolling per column
    private final float[] scrollTargets = new float[5];
    private final float[] scrollCurrents = new float[5];
    private final float[] maxScrolls = new float[5];

    // Animations per module and setting
    private final Map<String, Animation> moduleHoverAnims = new HashMap<>();
    private final Map<String, Animation> moduleExpandAnims = new HashMap<>();
    private final Map<String, Animation> moduleToggleAnims = new HashMap<>();
    private final Map<String, Animation> chipHoverAnims = new HashMap<>();
    private final Map<String, Animation> dropdownAnims = new HashMap<>();

    // Search bar state
    public static boolean searchActive = false;
    private static String searchQuery = "";
    private long searchTypeTime = System.currentTimeMillis();
    private static final int SEARCH_LIMIT = 24;

    // Interaction states
    public static SliderSetting draggingSlider = null;
    public static float sliderTrackX = 0;
    public static float sliderTrackW = 0;

    public static BindSettings activeBind = null;
    public static Module activeModuleBind = null;
    public static MultiBooleanSetting openMultiDropdown = null;
    public static ModeSetting openModeDropdown = null;

    public static StringSetting activeStringSetting = null;
    public static String stringEditBuffer = "";

    public static HueSetting draggingColor = null;
    public static int draggingColorBar = 0;
    public static float colorBarX = 0;
    public static float colorBarW = 0;

    // Window offset (optional dragging)
    public static float windowOffsetX = 0;
    public static float windowOffsetY = 0;
    public static float targetWindowOffsetX = 0;
    public static float targetWindowOffsetY = 0;
    private boolean draggingWindow = false;
    private float dragStartX = 0;
    private float dragStartY = 0;

    private boolean exit = false;
    private float scaleFix = 1.0F;
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private long openTime = 0;
    private long lastFrameTime = 0;

    public static boolean isTyping() {
        return searchActive || activeBind != null || activeModuleBind != null || activeStringSetting != null;
    }

    public MenuScreen() {
        super(Text.literal("ClickGUI"));
    }

    /** Вызывается обёрткой {@code FluxClickGuiScreen}, так как Minecraft инициализирует только её. */
    public void initializeForWrapper() {
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        Scissor.reset();
        openTime = System.currentTimeMillis();
        exit = false;
        searchActive = false;
        activeBind = null;
        activeModuleBind = null;
        activeStringSetting = null;
        openMultiDropdown = null;
        openModeDropdown = null;
        draggingSlider = null;
        draggingColor = null;
        draggingWindow = false;
        targetWindowOffsetX = windowOffsetX;
        targetWindowOffsetY = windowOffsetY;
        lastFrameTime = System.currentTimeMillis();
        GuiSounds.open();
        globalAnim.run(1, 220).setEasing(Easings.SINE_OUT);
    }

    private Animation getAnim(Map<String, Animation> map, String key) {
        return map.computeIfAbsent(key, k -> {
            Animation anim = new Animation().run(1, 220).setEasing(Easings.SINE_OUT);
            return anim;
        });
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        closeCheck();
        globalAnim.update();
        updateMovementKeys();

        MinecraftClient mc = MinecraftClient.getInstance();
        float targetScale = 2F;
        float currentScale = (float) mc.getWindow().getScaleFactor();
        scaleFix = targetScale / currentScale;

        int screenWidth = (int) (mc.getWindow().getScaledWidth() / scaleFix);
        int screenHeight = (int) (mc.getWindow().getScaledHeight() / scaleFix);

        float scaledMouseX = (float) (mouseX / scaleFix);
        float scaledMouseY = (float) (mouseY / scaleFix);
        lastMouseX = scaledMouseX;
        lastMouseY = scaledMouseY;

        float globalAlpha = globalAnim.get();

        // S is handled by clickgui settings, default to 1.0F
        S = 1.0F;

        Scissor.reset();

        // Render Background blur & effects
        backgroundRenderer.render(scaledMouseX, scaledMouseY, screenWidth, screenHeight, globalAlpha, S, exit);
        Client.get().render2D().flushAll();

        ScreenBlur.capture(2);

        if (context != null) context.getMatrices().pushMatrix();
        Render2D.beginOverlay();
        try {
            renderFigmaColumns(context, screenWidth, screenHeight, scaledMouseX, scaledMouseY, delta, globalAlpha);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Scissor.reset();
            Client.get().render2D().flushAll();
            Render2D.endOverlay();
            if (context != null) context.getMatrices().popMatrix();
        }
    }

    private void renderFigmaColumns(DrawContext context, int screenWidth, int screenHeight,
                                    float mouseX, float mouseY, float delta, float globalAlpha) {

        long now = System.currentTimeMillis();
        if (lastFrameTime == 0) lastFrameTime = now;
        float dt = (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;
        dt = MathHelper.clamp(dt, 0.001f, 0.05f);

        // Window offset interpolation
        float winFactor = 1.0f - (float) Math.exp(-18.0f * dt);
        windowOffsetX += (targetWindowOffsetX - windowOffsetX) * winFactor;
        windowOffsetY += (targetWindowOffsetY - windowOffsetY) * winFactor;
        if (Math.abs(targetWindowOffsetX - windowOffsetX) < 0.05f) windowOffsetX = targetWindowOffsetX;
        if (Math.abs(targetWindowOffsetY - windowOffsetY) < 0.05f) windowOffsetY = targetWindowOffsetY;

        // Smooth scroll interpolation per column
        float scrollFactor = 1.0f - (float) Math.exp(-14.0f * dt);
        for (int i = 0; i < 5; i++) {
            scrollCurrents[i] += (scrollTargets[i] - scrollCurrents[i]) * scrollFactor;
            if (Math.abs(scrollTargets[i] - scrollCurrents[i]) < 0.05f) {
                scrollCurrents[i] = scrollTargets[i];
            }
        }

        // Layout Dimensions (Compact, pixel-perfect 2x reduction from original full-screen size)
        float colW = 96.0f * S;
        float colGap = 9.0f * S;
        float totalColsW = 5 * colW + 4 * colGap;

        float colH = Math.min(screenHeight - 50.0f * S, 285.0f * S);
        float startX = (screenWidth - totalColsW) / 2.0f + windowOffsetX;
        float colY = Math.max(10.0f * S, (screenHeight - colH - 30.0f * S) / 2.0f + windowOffsetY);

        // Slide animation on open/close
        if (exit) {
            colY += 16.0f * S - 16.0f * S * globalAlpha;
        } else {
            colY += -16.0f * S + 16.0f * S * globalAlpha;
        }

        float colRadius = 8.0f * S;
        int colBg = ColorUtil.getColor(17, 16, 18, (int) (245 * globalAlpha));
        int colOutline = ColorUtil.getColor(36, 34, 42, (int) (180 * globalAlpha));

        boolean searching = searchQuery != null && !searchQuery.trim().isEmpty();
        String q = searching ? searchQuery.trim().toLowerCase(Locale.ROOT) : "";

        // 1. Render the 5 Columns
        for (int i = 0; i < 5; i++) {
            Category category = CATEGORIES[i];
            float cx = startX + i * (colW + colGap);

            // Column Card Shadow & Blur & Background
            RenderUtil.Render2D.glow(cx, colY, colW, colH, ColorUtil.getColor(0, 0.22f * globalAlpha), colRadius, 10, 1);
            RenderUtil.Blur.blur(cx, colY, colW, colH, 1.0f * globalAlpha, colRadius, colBg);
            RenderUtil.Render2D.rect(cx, colY, colW, colH, colBg, colRadius);
            RenderUtil.Render2D.outline(cx, colY, colW, colH, Math.max(0.6f, 0.6f * S), colOutline, colRadius);

            // Render Header (Centered Category Icon + Title)
            renderColumnHeader(cx, colY, colW, category, globalAlpha);

            // Render Modules in Column
            renderColumnModules(cx, colY, colW, colH, i, category, q, mouseX, mouseY, globalAlpha);
        }

        // 2. Render Bottom Search Bar
        renderBottomSearchBar(startX, colY, totalColsW, colH, screenWidth, mouseX, mouseY, globalAlpha);
    }

    private void renderColumnHeader(float cx, float cy, float colW, Category category, float globalAlpha) {
        float headerH = 24.0f * S;
        float centerY = cy + headerH / 2.0f;

        String icon = category.getIcon();
        Font iconFont = Fonts.sf_medium;
        float iconSize = 7.0f * S;

        String name = category.getName();
        Font textFont = Fonts.sf_bold;
        float textFontSize = 5.6f * S;

        float iconW = iconFont.getWidth(icon, iconSize);
        float textW = textFont.getWidth(name, textFontSize);
        float gap = 3.5f * S;
        float totalHeaderW = iconW + gap + textW;

        float headerStartX = cx + (colW - totalHeaderW) / 2.0f;
        int col = ColorUtil.getColor(255, 255, 255, (int) (globalAlpha * 255));

        iconFont.draw(icon, headerStartX, centerY - iconFont.getHeight(iconSize) / 2.0f + 0.2f * S, iconSize, col);
        textFont.draw(name, headerStartX + iconW + gap, centerY - textFont.getHeight(textFontSize) / 2.0f + 0.2f * S, textFontSize, col);
    }

    private void renderColumnModules(float cx, float cy, float colW, float colH,
                                     int colIndex, Category category, String q,
                                     float mouseX, float mouseY, float globalAlpha) {

        float headerH = 24.0f * S;
        float listY = cy + headerH;
        float listH = colH - headerH - 8.0f * S;

        // Collect modules for category
        List<Module> modules = new ArrayList<>();
        if (Client.get() != null && Client.get().moduleManager() != null) {
            for (Module m : Client.get().moduleManager().getModules()) {
                if (m.category == category) {
                    if (q.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(q) || m.name.toLowerCase(Locale.ROOT).contains(q)) {
                        modules.add(m);
                    }
                }
            }
        }

        // Enable scissor clipping for column
        Scissor.enable(cx + 2.0f * S, listY, colW - 4.0f * S, listH, 2.0f);

        float curY = listY - scrollCurrents[colIndex] + 3.0f * S;
        float modX = cx + 8.0f * S;
        float modW = colW - 16.0f * S;
        float totalContentH = 6.0f * S;

        for (Module m : modules) {
            float modH = 16.0f * S;

            Animation hoverAnim = getAnim(moduleHoverAnims, m.name + ":hover");
            Animation expandAnim = getAnim(moduleExpandAnims, m.name + ":expand");
            Animation toggleAnim = getAnim(moduleToggleAnims, m.name + ":toggle");

            boolean isHover = MathUtil.isHovered(mouseX, mouseY, modX, curY, modW, modH)
                    && mouseY >= listY && mouseY <= listY + listH;

            hoverAnim.setDirection(isHover ? 1 : -1);
            expandAnim.setDirection(m.isEnabled() ? 1 : -1);
            toggleAnim.setDirection(m.isEnabled() ? 1 : -1);

            float hoverProgress = hoverAnim.getOutput();
            float expandProgress = expandAnim.getOutput();
            float toggleProgress = toggleAnim.getOutput();

            // Render Module Header Row
            renderModuleRow(m, modX, curY, modW, modH, category, isHover, hoverProgress, expandProgress, toggleProgress, globalAlpha);

            curY += modH;
            totalContentH += modH;

            // Render Settings if Expanded
            float settingsH = calculateSettingsHeight(m, modW, S);
            if (expandProgress > 0.001f && settingsH > 0) {
                float renderSettingsH = settingsH * expandProgress;

                // Scissor settings area for clean slide animation
                Scissor.enable(modX, curY, modW, renderSettingsH, 2.0f);
                try {
                    renderModuleSettings(m, modX, curY, modW, mouseX, mouseY, globalAlpha, S);
                } finally {
                    Scissor.disable();
                }

                curY += renderSettingsH + 3.0f * S;
                totalContentH += renderSettingsH + 3.0f * S;
            } else {
                curY += 2.0f * S;
                totalContentH += 2.0f * S;
            }
        }

        Scissor.disable();

        maxScrolls[colIndex] = Math.max(0, totalContentH - listH);
        if (scrollTargets[colIndex] > maxScrolls[colIndex]) {
            scrollTargets[colIndex] = maxScrolls[colIndex];
        }
    }

    private void renderModuleRow(Module m, float x, float y, float w, float h,
                                 Category category, boolean isHover,
                                 float hoverProgress, float expandProgress, float toggleProgress,
                                 float globalAlpha) {

        float centerY = y + h / 2.0f;

        // Module Colors: Bright white if enabled, muted grey if disabled
        int disabledCol = ColorUtil.getColor(114, 113, 122, (int) (globalAlpha * 255));
        int hoverCol = ColorUtil.getColor(160, 158, 172, (int) (globalAlpha * 255));
        int enabledCol = ColorUtil.getColor(255, 255, 255, (int) (globalAlpha * 255));

        int baseTextCol = ColorUtil.overCol(disabledCol, hoverCol, hoverProgress);
        int textColor = ColorUtil.overCol(baseTextCol, enabledCol, toggleProgress);

        // Module Icon
        String icon = category.getIcon();
        Font iconFont = Fonts.sf_medium;
        float iconSize = 6.2f * S;
        float iconW = iconFont.getWidth(icon, iconSize);
        float iconX = x + 1.0f * S;
        iconFont.draw(icon, iconX, centerY - iconFont.getHeight(iconSize) / 2.0f + 0.2f * S, iconSize, textColor);

        // Module Name
        String name = m.name != null ? m.name : m.name;
        Font nameFont = Fonts.sf_bold;
        float nameSize = 5.2f * S;
        float textX = iconX + iconW + 3.0f * S;
        nameFont.draw(name, textX, centerY - nameFont.getHeight(nameSize) / 2.0f + 0.2f * S, nameSize, textColor);

        // Toggle Switch on Right
        float switchW = 12.5f * S;
        float switchH = 7.0f * S;
        float switchX = x + w - switchW;
        float switchY = centerY - switchH / 2.0f;
        drawToggleSwitch(switchX, switchY, switchW, switchH, toggleProgress, globalAlpha);

        // Arrow (left of the switch)
        boolean hasSettings = !m.getSettings().isEmpty() || m.getKey() > 0;
        if (hasSettings) {
            float arrowSize = 5.8f * S;
            float arrowX = switchX - 8.5f * S;
            drawArrowIcon(arrowX, centerY, arrowSize, expandProgress, textColor);
        }
    }

    private void renderModuleSettings(Module m, float x, float y, float w,
                                      float mouseX, float mouseY, float globalAlpha, float scale) {

        float curY = y + 1.5f * scale;

        for (ru.fluxvisuals.module.api.setting.Setting setting : m.getSettingsForAPI()) {
            // FluxVisuals settings don't have getVisible(), always show
            // 
            if (setting instanceof ModeSetting mode) {
                curY += renderModeSetting(mode, x, curY, w, mouseX, mouseY, globalAlpha, scale);
            } else if (setting instanceof MultiBooleanSetting multi) {
                curY += renderMultiBooleanSetting(multi, x, curY, w, mouseX, mouseY, globalAlpha, scale);
            } else if (setting instanceof SliderSetting slider) {
                curY += renderSliderSetting(slider, x, curY, w, mouseX, mouseY, globalAlpha, scale);
            } else if (setting instanceof BooleanSetting bool) {
                curY += renderBooleanSetting(bool, x, curY, w, mouseX, mouseY, globalAlpha, scale);
            } else if (setting instanceof BindSettings bind) {
                curY += renderBindSetting(bind, x, curY, w, mouseX, mouseY, globalAlpha, scale);
            } else if (setting instanceof StringSetting str) {
                curY += renderStringSetting(str, x, curY, w, mouseX, mouseY, globalAlpha, scale);
            } else if (setting instanceof HueSetting color) {
                curY += renderColorSetting(color, x, curY, w, mouseX, mouseY, globalAlpha, scale);
            }
        }

        // Render Module Keybind row at bottom
        curY += renderModuleKeybindRow(m, x, curY, w, mouseX, mouseY, globalAlpha, scale);
    }

    private float renderModeSetting(ModeSetting mode, float x, float y, float w,
                                    float mouseX, float mouseY, float globalAlpha, float scale) {

        // 1. Setting Title on top
        String title = Lang.tr(mode.name);
        float titleSize = 4.8f * scale;
        int titleCol = ColorUtil.getColor(148, 147, 158, (int) (globalAlpha * 255));
        Fonts.sf_medium.draw(title, x, y + 1.0f * scale, titleSize, titleCol);

        float titleH = Fonts.sf_medium.getHeight(titleSize) + 1.5f * scale;
        float boxY = y + titleH;
        float boxH = 13.0f * scale;
        float boxR = 3.0f * scale;

        String animKey = "" + ":" + mode.name;
        Animation openAnim = getAnim(dropdownAnims, animKey);
        boolean isOpen = (openModeDropdown == mode);
        openAnim.setDirection(isOpen ? 1 : -1);
        float openProgress = openAnim.getOutput();

        boolean isBarHover = MathUtil.isHovered(mouseX, mouseY, x, boxY, w, boxH);

        // Dropdown box background (smooth transition when open/hovered)
        int idleBoxBg = isBarHover
                ? ColorUtil.getColor(25, 24, 32, (int) (globalAlpha * 255))
                : ColorUtil.getColor(20, 19, 25, (int) (globalAlpha * 255));
        int activeBoxBg = ColorUtil.getColor(28, 27, 35, (int) (globalAlpha * 255));
        int boxBg = ColorUtil.overCol(idleBoxBg, activeBoxBg, openProgress);

        RenderUtil.Render2D.rect(x, boxY, w, boxH, boxBg, boxR);

        // Selected value text
        String val = Lang.tr(mode.currentMode);
        float textFontSize = 4.5f * scale;
        float textX = x + 7.0f * scale;
        float maxTextW = w - 20.0f * scale;
        float textY = boxY + (boxH - Fonts.sf_medium.getHeight(textFontSize)) / 2.0f + 0.2f * scale;
        int textColor = ColorUtil.getColor(235, 234, 245, (int) (globalAlpha * 255));

        Fonts.sf_medium.drawFadingText(val, textX, textY, maxTextW, textColor, textFontSize);

        // Arrow on the right (smoothly flips on open)
        float arrowSize = 4.6f * scale;
        float arrowX = x + w - 8.5f * scale;
        float arrowY = boxY + boxH / 2.0f;
        int arrowCol = (isBarHover || openProgress > 0.05f)
                ? ColorUtil.getColor(240, 238, 250, (int) (globalAlpha * 255))
                : ColorUtil.getColor(160, 158, 172, (int) (globalAlpha * 255));
        drawArrowIcon(arrowX, arrowY, arrowSize, openProgress, arrowCol);

        float totalH = titleH + boxH + 1.5f * scale;

        // 2. Expanded Options List with smooth slide & fade animation
        if (openProgress > 0.001f) {
            float menuY = boxY + boxH + 1.5f * scale;
            float itemH = 11.5f * scale;
            float fullMenuH = mode.modes.size() * itemH + 2.0f * scale;
            float renderMenuH = fullMenuH * openProgress;
            float menuR = 3.0f * scale;

            Scissor.enable(x - 1.0f * scale, menuY, w + 2.0f * scale, renderMenuH, 2.0f);
            try {
                int menuAlpha = (int) (globalAlpha * 255 * Math.min(1.0f, openProgress * 1.4f));
                int menuBg = ColorUtil.getColor(17, 16, 21, menuAlpha);
                RenderUtil.Render2D.rect(x, menuY, w, fullMenuH, menuBg, menuR);

                for (int i = 0; i < mode.modes.size(); i++) {
                    String opt = mode.modes.get(i);
                    float rowY = menuY + 1.0f * scale + i * itemH;
                    boolean isRowHover = isOpen && MathUtil.isHovered(mouseX, mouseY, x + 1.0f * scale, rowY, w - 2.0f * scale, itemH);
                    boolean isSelected = opt.equalsIgnoreCase(mode.currentMode);

                    if (isRowHover) {
                        int hoverBg = ColorUtil.getColor(30, 29, 38, menuAlpha);
                        RenderUtil.Render2D.rect(x + 2.0f * scale, rowY + 0.5f * scale, w - 4.0f * scale, itemH - 1.0f * scale, hoverBg, 2.0f * scale);
                    }

                    String optName = Lang.tr(opt);
                    float optFontSize = 4.4f * scale;
                    int optCol = isSelected
                            ? ColorUtil.getColor(255, 255, 255, menuAlpha)
                            : ColorUtil.getColor(154, 153, 165, menuAlpha);
                    float optTextY = rowY + (itemH - Fonts.sf_medium.getHeight(optFontSize)) / 2.0f + 0.2f * scale;
                    Fonts.sf_medium.draw(optName, x + 6.5f * scale, optTextY, optFontSize, optCol);

                    // Checkmark for selected item
                    if (isSelected) {
                        float chkSize = 6.0f * scale;
                        float chkX = x + w - chkSize - 6.0f * scale;
                        float chkY = rowY + (itemH - chkSize) / 2.0f;
                        drawCheckmark(chkX, chkY, chkSize, scale, ColorUtil.getColor(255, 255, 255, menuAlpha));
                    }
                }
            } finally {
                Scissor.disable();
            }

            totalH += (fullMenuH + 1.5f * scale) * openProgress;
        }

        return totalH;
    }

    private float renderMultiBooleanSetting(MultiBooleanSetting multi, float x, float y, float w,
                                            float mouseX, float mouseY, float globalAlpha, float scale) {

        // 1. Title
        String title = Lang.tr(multi.name);
        float titleSize = 4.8f * scale;
        int titleCol = ColorUtil.getColor(148, 147, 158, (int) (globalAlpha * 255));
        Fonts.sf_medium.draw(title, x, y + 1.0f * scale, titleSize, titleCol);

        float titleH = Fonts.sf_medium.getHeight(titleSize) + 1.5f * scale;
        float boxY = y + titleH;
        float boxH = 13.0f * scale;
        float boxR = 3.0f * scale;

        String animKey = "" + ":" + multi.name;
        Animation openAnim = getAnim(dropdownAnims, animKey);
        boolean isOpen = (openMultiDropdown == multi);
        openAnim.setDirection(isOpen ? 1 : -1);
        float openProgress = openAnim.getOutput();

        boolean isBarHover = MathUtil.isHovered(mouseX, mouseY, x, boxY, w, boxH);

        // Box background (no light outline, smooth transition)
        int idleBoxBg = isBarHover
                ? ColorUtil.getColor(25, 24, 32, (int) (globalAlpha * 255))
                : ColorUtil.getColor(20, 19, 25, (int) (globalAlpha * 255));
        int activeBoxBg = ColorUtil.getColor(28, 27, 35, (int) (globalAlpha * 255));
        int boxBg = ColorUtil.overCol(idleBoxBg, activeBoxBg, openProgress);

        RenderUtil.Render2D.rect(x, boxY, w, boxH, boxBg, boxR);

        // Summary of enabled options
        List<String> enabled = new ArrayList<>();
        for (BooleanSetting b : multi.settings) {
            if (b.get()) {
                enabled.add(Lang.tr(b.name));
            }
        }
        String summary;
        if (enabled.isEmpty()) {
            summary = "Ничего";
        } else if (enabled.size() == multi.settings.size()) {
            summary = "Все";
        } else {
            summary = String.join(", ", enabled);
        }

        float textFontSize = 4.5f * scale;
        float textX = x + 7.0f * scale;
        float maxTextW = w - 20.0f * scale;
        float textY = boxY + (boxH - Fonts.sf_medium.getHeight(textFontSize)) / 2.0f + 0.2f * scale;
        int textColor = enabled.isEmpty()
                ? ColorUtil.getColor(128, 126, 138, (int) (globalAlpha * 255))
                : ColorUtil.getColor(235, 234, 245, (int) (globalAlpha * 255));

        Fonts.sf_medium.drawFadingText(summary, textX, textY, maxTextW, textColor, textFontSize);

        // Arrow on the right (smoothly flips on open)
        float arrowSize = 4.6f * scale;
        float arrowX = x + w - 8.5f * scale;
        float arrowY = boxY + boxH / 2.0f;
        int arrowCol = (isBarHover || openProgress > 0.05f)
                ? ColorUtil.getColor(240, 238, 250, (int) (globalAlpha * 255))
                : ColorUtil.getColor(160, 158, 172, (int) (globalAlpha * 255));
        drawArrowIcon(arrowX, arrowY, arrowSize, openProgress, arrowCol);

        float totalH = titleH + boxH + 1.5f * scale;

        // 2. Expanded Options List with smooth slide & fade animation
        if (openProgress > 0.001f) {
            float menuY = boxY + boxH + 1.5f * scale;
            float itemH = 11.5f * scale;
            float fullMenuH = multi.settings.size() * itemH + 2.0f * scale;
            float renderMenuH = fullMenuH * openProgress;
            float menuR = 3.0f * scale;

            Scissor.enable(x - 1.0f * scale, menuY, w + 2.0f * scale, renderMenuH, 2.0f);
            try {
                int menuAlpha = (int) (globalAlpha * 255 * Math.min(1.0f, openProgress * 1.4f));
                int menuBg = ColorUtil.getColor(17, 16, 21, menuAlpha);
                RenderUtil.Render2D.rect(x, menuY, w, fullMenuH, menuBg, menuR);

                int i = 0;
                for (BooleanSetting opt : multi.settings) {
                    float rowY = menuY + 1.0f * scale + i * itemH;
                    boolean isRowHover = isOpen && MathUtil.isHovered(mouseX, mouseY, x + 1.0f * scale, rowY, w - 2.0f * scale, itemH);

                    if (isRowHover) {
                        int hoverBg = ColorUtil.getColor(30, 29, 38, menuAlpha);
                        RenderUtil.Render2D.rect(x + 2.0f * scale, rowY + 0.5f * scale, w - 4.0f * scale, itemH - 1.0f * scale, hoverBg, 2.0f * scale);
                    }

                    String optName = Lang.tr(opt.name);
                    float optFontSize = 4.4f * scale;
                    int optCol = opt.get()
                            ? ColorUtil.getColor(255, 255, 255, menuAlpha)
                            : ColorUtil.getColor(154, 153, 165, menuAlpha);
                    float optTextY = rowY + (itemH - Fonts.sf_medium.getHeight(optFontSize)) / 2.0f + 0.2f * scale;
                    Fonts.sf_medium.draw(optName, x + 6.5f * scale, optTextY, optFontSize, optCol);

                    // Checkbox / Checkmark on the right (compact)
                    float chkSize = 5.6f * scale;
                    float chkX = x + w - chkSize - 6.0f * scale;
                    float chkY = rowY + (itemH - chkSize) / 2.0f;
                    float chkR = 1.8f * scale;

                    if (opt.get()) {
                        RenderUtil.Render2D.rect(chkX, chkY, chkSize, chkSize, ColorUtil.getColor(255, 255, 255, menuAlpha), chkR);
                        drawCheckmark(chkX, chkY, chkSize, scale, ColorUtil.getColor(17, 16, 21, menuAlpha));
                    } else {
                        RenderUtil.Render2D.rect(chkX, chkY, chkSize, chkSize, ColorUtil.getColor(26, 25, 32, menuAlpha), chkR);
                        RenderUtil.Render2D.outline(chkX, chkY, chkSize, chkSize, Math.max(0.5f, 0.5f * scale), ColorUtil.getColor(48, 47, 58, (int) (menuAlpha * 0.8f)), chkR);
                    }

                    i++;
                }
            } finally {
                Scissor.disable();
            }

            totalH += (fullMenuH + 1.5f * scale) * openProgress;
        }

        return totalH;
    }

    private float renderSliderSetting(SliderSetting slider, float x, float y, float w,
                                      float mouseX, float mouseY, float globalAlpha, float scale) {

        float labelSize = 4.8f * scale;
        float labelH = Fonts.sf_bold.getHeight(labelSize);

        // Line 1: Label on left (bold, crisp white)
        String name = Lang.tr(slider.name);
        int labelCol = ColorUtil.getColor(245, 245, 250, (int) (globalAlpha * 255));
        Fonts.sf_bold.draw(name, x, y + 1.0f * scale, labelSize, labelCol);

        // Line 1: Value on right (bold white number + subtle muted unit suffix)
        String numStr = getSliderNumberStr(slider);
        String unit = getSliderUnit(slider);

        float numW = Fonts.sf_bold.getWidth(numStr, 4.6f * scale);
        float unitW = unit.isEmpty() ? 0 : Fonts.sf_medium.getWidth(unit, 4.2f * scale);
        float totalValW = numW + unitW;
        float valX = x + w - totalValW;

        Fonts.sf_bold.draw(numStr, valX, y + 1.0f * scale, 4.6f * scale, ColorUtil.getColor(245, 245, 250, (int) (globalAlpha * 255)));
        if (!unit.isEmpty()) {
            Fonts.sf_medium.draw(unit, valX + numW, y + 1.2f * scale, 4.2f * scale, ColorUtil.getColor(135, 134, 145, (int) (globalAlpha * 255)));
        }

        // Line 2: Slider Track + Hollow Ring Knob
        float trackY = y + labelH + 2.6f * scale;
        float trackH = 1.4f * scale;

        float percent = (slider.current - slider.minimum) / (slider.maximum - slider.minimum);
        percent = MathHelper.clamp(percent, 0.0f, 1.0f);

        // Knob dimensions: compact, delicate ring matching reference image
        float knobD = 2.6f * scale;
        float knobR = knobD / 2.0f;
        float holeD = 1.0f * scale;
        float holeR = holeD / 2.0f;

        float usableW = w - 2.0f * knobR;
        float knobCenterX = x + knobR + usableW * percent;
        float knobCenterY = trackY + trackH / 2.0f;

        // Track background: thin, subtle dark line
        int trackBg = ColorUtil.getColor(28, 27, 34, (int) (globalAlpha * 255));
        RenderUtil.Render2D.rect(x, trackY, w, trackH, trackBg, trackH / 2.0f);

        // Filled track: white bar seamlessly connecting to the ring
        float fillW = knobCenterX - x;
        int white = ColorUtil.getColor(255, 255, 255, (int) (globalAlpha * 255));
        if (fillW > 0.5f) {
            RenderUtil.Render2D.rect(x, trackY, fillW, trackH, white, trackH / 2.0f);
        }

        // Outer white circle
        RenderUtil.Render2D.rect(knobCenterX - knobR, knobCenterY - knobR, knobD, knobD, white, knobR);

        // Inner dark hole (card background color, cutting out the center)
        int holeCol = ColorUtil.getColor(17, 16, 18, (int) (245 * globalAlpha));
        RenderUtil.Render2D.rect(knobCenterX - holeR, knobCenterY - holeR, holeD, holeD, holeCol, holeR);

        return labelH + 6.5f * scale;
    }

    private float renderBooleanSetting(BooleanSetting bool, float x, float y, float w,
                                       float mouseX, float mouseY, float globalAlpha, float scale) {

        float rowH = 9.6f * scale;
        float centerY = y + rowH / 2.0f;

        // Label on left
        String name = Lang.tr(bool.name);
        float labelSize = 4.8f * scale;
        int labelCol = ColorUtil.getColor(220, 219, 230, (int) (globalAlpha * 255));
        Fonts.sf_medium.draw(name, x, centerY - Fonts.sf_medium.getHeight(labelSize) / 2.0f + 0.2f * scale, labelSize, labelCol);

        // Checkbox on right (compact and proportional to text)
        float boxSize = 5.6f * scale;
        float boxX = x + w - boxSize - 0.5f * scale;
        float boxY = centerY - boxSize / 2.0f;
        float boxR = 1.8f * scale;

        if (bool.get()) {
            int boxBg = ColorUtil.getColor(255, 255, 255, (int) (globalAlpha * 255));
            RenderUtil.Render2D.rect(boxX, boxY, boxSize, boxSize, boxBg, boxR);
            int checkCol = ColorUtil.getColor(17, 16, 21, (int) (globalAlpha * 255));
            drawCheckmark(boxX, boxY, boxSize, scale, checkCol);
        } else {
            int boxBg = ColorUtil.getColor(26, 25, 32, (int) (globalAlpha * 255));
            int boxBorder = ColorUtil.getColor(48, 47, 58, (int) (globalAlpha * 200));
            RenderUtil.Render2D.rect(boxX, boxY, boxSize, boxSize, boxBg, boxR);
            RenderUtil.Render2D.outline(boxX, boxY, boxSize, boxSize, Math.max(0.5f, 0.5f * scale), boxBorder, boxR);
        }

        return rowH + 0.4f * scale;
    }

    private float renderBindSetting(BindSettings bind, float x, float y, float w,
                                    float mouseX, float mouseY, float globalAlpha, float scale) {

        float rowH = 9.6f * scale;
        float centerY = y + rowH / 2.0f;

        // Label on left
        String name = Lang.tr(bind.name);
        float labelSize = 4.8f * scale;
        int labelCol = ColorUtil.getColor(220, 219, 230, (int) (globalAlpha * 255));
        Fonts.sf_medium.draw(name, x, centerY - Fonts.sf_medium.getHeight(labelSize) / 2.0f + 0.2f * scale, labelSize, labelCol);

        // Keycap badge on right
        boolean isListening = (activeBind == bind);
        String keyStr = isListening ? "..." : (bind.get() > 0 ? Keyboard.keyName(bind.get()) : "NONE");
        drawKeycap(x, centerY, w, keyStr, isListening, bind.get() > 0, globalAlpha, scale);

        return rowH + 0.4f * scale;
    }

    private float renderModuleKeybindRow(Module m, float x, float y, float w,
                                         float mouseX, float mouseY, float globalAlpha, float scale) {

        float rowH = 9.6f * scale;
        float centerY = y + rowH / 2.0f;

        // Label on left: "Keybind"
        float labelSize = 4.8f * scale;
        int labelCol = ColorUtil.getColor(220, 219, 230, (int) (globalAlpha * 255));
        Fonts.sf_medium.draw("Keybind", x, centerY - Fonts.sf_medium.getHeight(labelSize) / 2.0f + 0.2f * scale, labelSize, labelCol);

        // Keycap badge on right: e.g. [ R ] or [ N ]
        boolean isListening = (activeModuleBind == m);
        String keyStr = isListening ? "..." : (m.getKey() > 0 ? Keyboard.keyName(m.getKey()) : "NONE");
        drawKeycap(x, centerY, w, keyStr, isListening, m.getKey() > 0, globalAlpha, scale);

        return rowH + 1.4f * scale;
    }

    private float renderStringSetting(StringSetting str, float x, float y, float w,
                                      float mouseX, float mouseY, float globalAlpha, float scale) {

        float rowH = 10.0f * scale;
        float centerY = y + rowH / 2.0f;

        // Label on left
        String name = Lang.tr(str.name);
        float labelSize = 4.8f * scale;
        int labelCol = ColorUtil.getColor(220, 219, 230, (int) (globalAlpha * 255));
        Fonts.sf_medium.draw(name, x, centerY - Fonts.sf_medium.getHeight(labelSize) / 2.0f + 0.2f * scale, labelSize, labelCol);

        // Input Box on right
        boolean isEditing = (activeStringSetting == str);
        String text = isEditing ? stringEditBuffer + "_" : str.get();
        float boxW = Math.min(50.0f * scale, w - Fonts.sf_medium.getWidth(name, labelSize) - 6.0f * scale);
        float boxH = 9.5f * scale;
        float boxX = x + w - boxW;
        float boxY = centerY - boxH / 2.0f;

        int boxBg = ColorUtil.getColor(28, 27, 34, (int) (globalAlpha * 255));
        int boxBorder = isEditing ? ColorUtil.getColor(255, 255, 255, (int) (globalAlpha * 255)) : ColorUtil.getColor(48, 47, 56, (int) (globalAlpha * 220));
        RenderUtil.Render2D.rect(boxX, boxY, boxW, boxH, boxBg, 2.0f * scale);
        RenderUtil.Render2D.outline(boxX, boxY, boxW, boxH, Math.max(0.5f, 0.5f * scale), boxBorder, 2.0f * scale);

        Fonts.sf_medium.draw(text, boxX + 3.0f * scale, centerY - Fonts.sf_medium.getHeight(4.4f * scale) / 2.0f + 0.2f * scale, 4.4f * scale, ColorUtil.getColor(220, 218, 235, (int) (globalAlpha * 255)));

        return rowH + 0.5f * scale;
    }

    private float renderColorSetting(HueSetting color, float x, float y, float w,
                                     float mouseX, float mouseY, float globalAlpha, float scale) {

        float rowH = 10.0f * scale;
        float centerY = y + rowH / 2.0f;

        // Label on left
        String name = Lang.tr(color.name);
        float labelSize = 4.8f * scale;
        int labelCol = ColorUtil.getColor(220, 219, 230, (int) (globalAlpha * 255));
        Fonts.sf_medium.draw(name, x, centerY - Fonts.sf_medium.getHeight(labelSize) / 2.0f + 0.2f * scale, labelSize, labelCol);

        // Color Preview Badge on right
        float badgeW = 11.0f * scale;
        float badgeH = 7.0f * scale;
        float badgeX = x + w - badgeW;
        float badgeY = centerY - badgeH / 2.0f;

        int curCol = color.getRGB();
        RenderUtil.Render2D.rect(badgeX, badgeY, badgeW, badgeH, curCol, 2.0f * scale);
        RenderUtil.Render2D.outline(badgeX, badgeY, badgeW, badgeH, Math.max(0.5f, 0.5f * scale), ColorUtil.getColor(255, 255, 255, (int) (globalAlpha * 200)), 2.0f * scale);

        return rowH + 0.5f * scale;
    }

    private void renderBottomSearchBar(float startX, float colY, float totalColsW, float colH,
                                       int screenWidth, float mouseX, float mouseY, float globalAlpha) {

        float searchW = 120.0f * S;
        float searchH = 20.0f * S;
        float searchX = (screenWidth - searchW) / 2.0f;
        float searchY = colY + colH + 7.0f * S;
        float searchRadius = searchH / 2.0f;

        boolean isHover = MathUtil.isHovered(mouseX, mouseY, searchX, searchY, searchW, searchH);
        int searchBg = ColorUtil.getColor(23, 21, 26, (int) (245 * globalAlpha));
        int searchOutline = (searchActive || isHover)
                ? ColorUtil.getColor(255, 255, 255, (int) (220 * globalAlpha))
                : ColorUtil.getColor(44, 42, 52, (int) (200 * globalAlpha));

        // Pill Card Shadow & Blur & Background
        RenderUtil.Render2D.glow(searchX, searchY, searchW, searchH, ColorUtil.getColor(0, 0.20f * globalAlpha), searchRadius, 8, 1);
        RenderUtil.Blur.blur(searchX, searchY, searchW, searchH, 1.0f * globalAlpha, searchRadius, searchBg);
        RenderUtil.Render2D.rect(searchX, searchY, searchW, searchH, searchBg, searchRadius);
        RenderUtil.Render2D.outline(searchX, searchY, searchW, searchH, Math.max(0.5f, 0.6f * S), searchOutline, searchRadius);

        float centerY = searchY + searchH / 2.0f;

        // Search text / placeholder on left
        if (searchQuery.isEmpty() && !searchActive) {
            int placeholderCol = ColorUtil.getColor(117, 116, 127, (int) (globalAlpha * 255));
            Fonts.sf_medium.draw("Search", searchX + 9.0f * S, centerY - Fonts.sf_medium.getHeight(4.8f * S) / 2.0f + 0.2f * S, 4.8f * S, placeholderCol);
        } else {
            String cursor = (searchActive && (System.currentTimeMillis() / 450) % 2 == 0) ? "|" : "";
            String display = searchQuery + cursor;
            int textCol = ColorUtil.getColor(235, 234, 245, (int) (globalAlpha * 255));
            Fonts.sf_medium.draw(display, searchX + 9.0f * S, centerY - Fonts.sf_medium.getHeight(4.8f * S) / 2.0f + 0.2f * S, 4.8f * S, textCol);
        }

        // Search Magnifying Glass Icon on right
        float iconSize = 5.5f * S;
        float iconX = searchX + searchW - 13.0f * S;
        int iconCol = (searchActive || isHover)
                ? ColorUtil.getColor(240, 238, 250, (int) (globalAlpha * 255))
                : ColorUtil.getColor(154, 153, 163, (int) (globalAlpha * 255));
        Fonts.gui.draw("A", iconX, centerY - Fonts.gui.getHeight(iconSize) / 2.0f + 0.2f * S, iconSize, iconCol);
    }

    // --- Helper Drawing Primitives ---

    private void drawPill(float x, float y, float w, float h, float r,
                          String text, boolean active, boolean hover,
                          float globalAlpha, float scale) {

        int bg;
        int textCol;

        if (active) {
            bg = ColorUtil.getColor(255, 255, 255, (int) (globalAlpha * 255));
            textCol = ColorUtil.getColor(17, 16, 18, (int) (globalAlpha * 255));
        } else {
            bg = hover
                    ? ColorUtil.getColor(48, 47, 56, (int) (globalAlpha * 255))
                    : ColorUtil.getColor(32, 31, 38, (int) (globalAlpha * 255));
            textCol = hover
                    ? ColorUtil.getColor(235, 234, 245, (int) (globalAlpha * 255))
                    : ColorUtil.getColor(160, 158, 172, (int) (globalAlpha * 255));
        }

        RenderUtil.Render2D.rect(x, y, w, h, bg, r);

        float textW = Fonts.sf_medium.getWidth(text, 4.5f * scale);
        float textH = Fonts.sf_medium.getHeight(4.5f * scale);
        float tx = x + Math.max(2.0f * scale, (w - textW) / 2.0f);
        float ty = y + (h - textH) / 2.0f + 0.2f * scale;

        Fonts.sf_medium.draw(text, tx, ty, 4.5f * scale, textCol);
    }

    private void drawKeycap(float x, float centerY, float w, String keyStr,
                            boolean isListening, boolean hasBind, float globalAlpha, float scale) {

        float kw = Fonts.sf_medium.getWidth(keyStr, 4.2f * scale);
        float badgeW = Math.max(12.0f * scale, kw + 6.0f * scale);
        float badgeH = 8.0f * scale;
        float badgeX = x + w - badgeW;
        float badgeY = centerY - badgeH / 2.0f;
        float badgeR = 2.0f * scale;

        int bg;
        int textCol;

        if (isListening) {
            bg = ColorUtil.getColor(255, 255, 255, (int) (globalAlpha * 255));
            textCol = ColorUtil.getColor(17, 16, 18, (int) (globalAlpha * 255));
            RenderUtil.Render2D.rect(badgeX, badgeY, badgeW, badgeH, bg, badgeR);
            RenderUtil.Render2D.outline(badgeX, badgeY, badgeW, badgeH, Math.max(0.5f, 0.5f * scale), ColorUtil.getColor(0, 0, 0, (int) (globalAlpha * 255)), badgeR);
        } else if (hasBind) {
            bg = ColorUtil.getColor(255, 255, 255, (int) (globalAlpha * 255));
            textCol = ColorUtil.getColor(17, 16, 18, (int) (globalAlpha * 255));
            RenderUtil.Render2D.rect(badgeX, badgeY, badgeW, badgeH, bg, badgeR);
        } else {
            bg = ColorUtil.getColor(34, 33, 40, (int) (globalAlpha * 255));
            textCol = ColorUtil.getColor(114, 113, 122, (int) (globalAlpha * 255));
            RenderUtil.Render2D.rect(badgeX, badgeY, badgeW, badgeH, bg, badgeR);
            RenderUtil.Render2D.outline(badgeX, badgeY, badgeW, badgeH, Math.max(0.5f, 0.5f * scale), ColorUtil.getColor(50, 49, 58, (int) (globalAlpha * 200)), badgeR);
        }

        float tx = badgeX + (badgeW - kw) / 2.0f;
        float ty = centerY - Fonts.sf_medium.getHeight(4.2f * scale) / 2.0f + 0.2f * scale;
        Fonts.sf_medium.draw(keyStr, tx, ty, 4.2f * scale, textCol);
    }

    private void drawToggleSwitch(float x, float y, float w, float h, float progress, float globalAlpha) {
        float r = h / 2.0f;
        float pad = 1.1f * S;
        float knobD = h - pad * 2.0f;
        float knobR = knobD / 2.0f;
        float knobY = y + pad;

        float knobXLeft = x + pad;
        float knobXRight = x + w - knobD - pad;
        float knobX = knobXLeft + (knobXRight - knobXLeft) * progress;

        // Background: Subtle dark pill when off, pure solid white pill when on (Image 2 style)
        int offBg = ColorUtil.getColor(26, 25, 32, (int) (globalAlpha * 255));
        int onBg = ColorUtil.getColor(255, 255, 255, (int) (globalAlpha * 255));
        int bg = ColorUtil.overCol(offBg, onBg, progress);

        // Knob: Muted gray circle when off (Image 3), dark cutout hole when on (Image 2)
        int offKnob = ColorUtil.getColor(80, 80, 88, (int) (globalAlpha * 255));
        int onKnob = ColorUtil.getColor(17, 16, 18, (int) (245 * globalAlpha));
        int knobCol = ColorUtil.overCol(offKnob, onKnob, progress);

        RenderUtil.Render2D.rect(x, y, w, h, bg, r);
        RenderUtil.Render2D.rect(knobX, knobY, knobD, knobD, knobCol, knobR);
    }

    private static void drawArrowIcon(float cx, float cy, float size, float progress, int color) {
        float rot = progress * 180.0f;
        int[] colors = {color, color, color, color};
        float[] radii = {0f, 0f, 0f, 0f};
        Client.get().render2D().getTexturePipeline().drawTexture(
                STRELOCHKA_TEX,
                cx - size / 2.0f, cy - size / 2.0f, size, size,
                0f, 0f, 1f, 1f,
                colors, radii, 1.0f, rot
        );
    }

    private static void drawChevron(float cx, float cy, float size, float progress, int color, float scale) {
        drawArrowIcon(cx, cy, size, progress, color);
    }

    private static void drawTriangle(float cx, float cy, float size, boolean pointingUp, int color) {
        drawArrowIcon(cx, cy, size, pointingUp ? 1.0f : 0.0f, color);
    }

    private static void drawTriangle(float cx, float cy, float size, float progress, int color) {
        drawArrowIcon(cx, cy, size, progress, color);
    }

    private static void drawCheckmark(float boxX, float boxY, float boxSize, float scale, int color) {
        float t = Math.max(0.65f, 0.85f * scale);
        float x1 = boxX + boxSize * 0.28f;
        float y1 = boxY + boxSize * 0.52f;
        float x2 = boxX + boxSize * 0.44f;
        float y2 = boxY + boxSize * 0.69f;
        float x3 = boxX + boxSize * 0.72f;
        float y3 = boxY + boxSize * 0.35f;

        drawLine(x1, y1, x2, y2, t, color);
        drawLine(x2, y2, x3, y3, t, color);
    }

    private static void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dist = (float) Math.hypot(dx, dy);
        if (dist <= 0.001f) return;
        int steps = Math.max(2, (int) Math.ceil(dist / Math.max(0.2f, thickness * 0.35f)));
        float rad = thickness / 2.0f;
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            RenderUtil.Render2D.rect(x1 + dx * t - rad, y1 + dy * t - rad, thickness, thickness, color, rad);
        }
    }

    public static String getSliderNumberStr(SliderSetting setting) {
        float val = setting.current;
        if (setting.increment == 1.0f || (val == (int) val)) {
            return String.valueOf((int) val);
        } else if (setting.increment < 0.1f) {
            return String.format(Locale.US, "%.2f", val);
        } else {
            return String.format(Locale.US, "%.1f", val);
        }
    }

    public static String getSliderUnit(SliderSetting setting) {
        String name = (setting.name + " " + Lang.tr(setting.name)).toLowerCase(Locale.ROOT);
        if (name.contains("(мс)") || name.contains("(сек)") || name.contains("(ms)") || name.contains("(s)")) {
            return "";
        }
        if (name.contains("дистанц") || name.contains("расстоян") || name.contains("reach") || name.contains("range") || name.contains("дист") || name.contains("дальност")) {
            return "m";
        } else if (name.contains("кд") || name.contains("задержк") || name.contains("delay") || name.contains("мс") || name.contains("ms")) {
            return "ms";
        } else if (name.contains("шанс") || name.contains("процент") || name.contains("chance") || name.contains("percent") || name.contains("%")) {
            return "%";
        } else if (name.contains("время") || name.contains("таймер") || name.contains("time") || name.contains("timer")) {
            return "s";
        }
        return "";
    }

    public static String formatSliderValue(SliderSetting setting) {
        return getSliderNumberStr(setting) + getSliderUnit(setting);
    }

    private float calculateSettingsHeight(Module m, float w, float scale) {
        float totalH = 1.5f * scale;

        for (ru.fluxvisuals.module.api.setting.Setting setting : m.getSettingsForAPI()) {
            
            if (setting instanceof ModeSetting mode) {
                float titleH = Fonts.sf_medium.getHeight(4.8f * scale) + 1.5f * scale;
                float boxH = 13.0f * scale;
                float total = titleH + boxH + 1.5f * scale;
                String animKey = "" + ":" + mode.name;
                Animation openAnim = dropdownAnims.get(animKey);
                float prog = (openAnim != null) ? openAnim.getOutput() : (openModeDropdown == mode ? 1.0f : 0.0f);
                if (prog > 0.001f) {
                    float itemH = 11.5f * scale;
                    float menuH = mode.modes.size() * itemH + 2.0f * scale;
                    total += (menuH + 1.5f * scale) * prog;
                }
                totalH += total;
            } else if (setting instanceof MultiBooleanSetting multi) {
                float titleH = Fonts.sf_medium.getHeight(4.8f * scale) + 1.5f * scale;
                float boxH = 13.0f * scale;
                float total = titleH + boxH + 1.5f * scale;
                String animKey = "" + ":" + multi.name;
                Animation openAnim = dropdownAnims.get(animKey);
                float prog = (openAnim != null) ? openAnim.getOutput() : (openMultiDropdown == multi ? 1.0f : 0.0f);
                if (prog > 0.001f) {
                    float itemH = 11.5f * scale;
                    float menuH = multi.settings.size() * itemH + 2.0f * scale;
                    total += (menuH + 1.5f * scale) * prog;
                }
                totalH += total;
            } else if (setting instanceof SliderSetting) {
                totalH += Fonts.sf_bold.getHeight(4.8f * scale) + 6.5f * scale;
            } else if (setting instanceof BooleanSetting) {
                totalH += 10.0f * scale;
            } else if (setting instanceof BindSettings) {
                totalH += 10.0f * scale;
            } else if (setting instanceof StringSetting) {
                totalH += 10.5f * scale;
            } else if (setting instanceof HueSetting) {
                totalH += 10.5f * scale;
            }
        }

        // Add module keybind row
        totalH += 11.0f * scale;

        return totalH;
    }

    // --- Mouse & Keyboard Input Handling ---

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float mouseX = (float) (click.comp_4798() / scaleFix);
        float mouseY = (float) (click.comp_4799() / scaleFix);

        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = (int) (mc.getWindow().getScaledWidth() / scaleFix);
        int screenHeight = (int) (mc.getWindow().getScaledHeight() / scaleFix);

        // Handle Active Keybinding
        if (activeBind != null) {
            if (activeBind.allowMouse && click.button() != 0 && click.button() != 1) {
                activeBind.set(click.button());
                GuiSounds.bindSet();
            }
            activeBind = null;
            return true;
        }

        if (activeModuleBind != null) {
            if (click.button() != 0 && click.button() != 1) {
                activeModuleBind.setKey(click.button());
                GuiSounds.bindSet();
            } else {
                GuiSounds.bindReset();
            }
            activeModuleBind = null;
            return true;
        }

        // Layout Dimensions
        float colW = 96.0f * S;
        float colGap = 9.0f * S;
        float totalColsW = 5 * colW + 4 * colGap;
        float colH = Math.min(screenHeight - 50.0f * S, 285.0f * S);
        float startX = (screenWidth - totalColsW) / 2.0f + windowOffsetX;
        float colY = Math.max(10.0f * S, (screenHeight - colH - 30.0f * S) / 2.0f + windowOffsetY);

        // 1. Bottom Search Bar Click
        float searchW = 120.0f * S;
        float searchH = 19.0f * S;
        float searchX = (screenWidth - searchW) / 2.0f;
        float searchY = colY + colH + 7.0f * S;

        if (MathUtil.isHovered(mouseX, mouseY, searchX, searchY, searchW, searchH)) {
            searchActive = !searchActive;
            if (searchActive) {
                searchTypeTime = System.currentTimeMillis();
                GuiSounds.editStart();
            } else {
                searchQuery = "";
                resetScrolls();
                GuiSounds.searchClear();
            }
            return true;
        } else if (click.button() == 0 && searchActive) {
            searchActive = false;
        }

        // 2. Column Clicking
        float headerH = 24.0f * S;
        float listY = colY + headerH;
        float listH = colH - headerH - 8.0f * S;

        boolean searching = searchQuery != null && !searchQuery.trim().isEmpty();
        String q = searching ? searchQuery.trim().toLowerCase(Locale.ROOT) : "";

        for (int i = 0; i < 5; i++) {
            Category category = CATEGORIES[i];
            float cx = startX + i * (colW + colGap);

            if (mouseX < cx || mouseX > cx + colW) continue;

            // Check if clicking inside module list area
            if (mouseY >= listY && mouseY <= listY + listH) {
                float curY = listY - scrollCurrents[i] + 3.0f * S;
                float modX = cx + 8.0f * S;
                float modW = colW - 16.0f * S;

                List<Module> modules = new ArrayList<>();
                if (Client.get() != null && Client.get().moduleManager() != null) {
                    for (Module m : Client.get().moduleManager().getAll()) {
                        if (m.category == category) {
                            if (q.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(q) || m.getBigName().toLowerCase(Locale.ROOT).contains(q)) {
                                modules.add(m);
                            }
                        }
                    }
                }

                for (Module m : modules) {
                    float modH = 16.0f * S;

                    // Click on module row header
                    if (MathUtil.isHovered(mouseX, mouseY, modX, curY, modW, modH)) {
                        float switchW = 12.5f * S;
                        float switchX = modX + modW - switchW;
                        float chevronX = switchX - 8.5f * S;

                        if (click.button() == 1 || MathUtil.isHovered(mouseX, mouseY, chevronX - 3.0f * S, curY, 11.0f * S, modH)) {
                            // Toggle expand
                            m.setExpanded(!m.isExpanded());
                            if (!m.isExpanded()) {
                                if (openMultiDropdown != null && m.getSettings().contains(openMultiDropdown)) {
                                    openMultiDropdown = null;
                                }
                                if (openModeDropdown != null && m.getSettings().contains(openModeDropdown)) {
                                    openModeDropdown = null;
                                }
                            }
                            GuiSounds.expand(m.isExpanded());
                            return true;
                        } else if (click.button() == 0) {
                            // Toggle module enabled
                            m.toggle();
                            GuiSounds.toggle(m.isEnabled());
                            return true;
                        }
                    }

                    curY += modH;

                    // Click on module settings if expanded
                    if (m.isExpanded()) {
                        float settingsH = calculateSettingsHeight(m, modW, S);

                        if (mouseY >= curY && mouseY <= curY + settingsH) {
                            if (handleSettingsClick(m, modX, curY, modW, mouseX, mouseY, click.button(), S)) {
                                return true;
                            }
                        }
                        curY += settingsH + 3.0f * S;
                    } else {
                        curY += 2.0f * S;
                    }
                }
            }
        }

        if (openMultiDropdown != null && click.button() == 0) {
            openMultiDropdown = null;
        }
        if (openModeDropdown != null && click.button() == 0) {
            openModeDropdown = null;
        }

        return super.mouseClicked(click, doubled);
    }

    private boolean handleSettingsClick(Module m, float x, float y, float w,
                                        float mouseX, float mouseY, int button, float scale) {

        float curY = y + 1.5f * scale;

        for (ru.fluxvisuals.module.api.setting.Setting setting : m.getSettingsForAPI()) {
            
            if (setting instanceof ModeSetting mode) {
                float titleH = Fonts.sf_medium.getHeight(4.8f * scale) + 1.5f * scale;
                float boxY = curY + titleH;
                float boxH = 13.0f * scale;

                // Click on dropdown bar: toggle open/close
                if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x, boxY, w, boxH)) {
                    if (openModeDropdown == mode) {
                        openModeDropdown = null;
                    } else {
                        openModeDropdown = mode;
                        openMultiDropdown = null;
                    }
                    GuiSounds.button();
                    return true;
                }

                float total = titleH + boxH + 1.5f * scale;

                String animKey = "" + ":" + mode.name;
                Animation openAnim = dropdownAnims.get(animKey);
                float prog = (openAnim != null) ? openAnim.getOutput() : (openModeDropdown == mode ? 1.0f : 0.0f);

                // If open, check clicks on items
                if (prog > 0.4f) {
                    float menuY = boxY + boxH + 1.5f * scale;
                    float itemH = 11.5f * scale;
                    float menuH = mode.modes.size() * itemH + 2.0f * scale;

                    for (int i = 0; i < mode.modes.size(); i++) {
                        String val = mode.modes.get(i);
                        float rowY = menuY + 1.0f * scale + i * itemH;
                        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x, rowY, w, itemH)) {
                            mode.currentMode = val;
                            GuiSounds.chip(i, mode.modes.size());
                            openModeDropdown = null;
                            return true;
                        }
                    }
                }

                if (prog > 0.001f) {
                    float itemH = 11.5f * scale;
                    float menuH = mode.modes.size() * itemH + 2.0f * scale;
                    total += (menuH + 1.5f * scale) * prog;
                }

                curY += total;
            } else if (setting instanceof MultiBooleanSetting multi) {
                float titleH = Fonts.sf_medium.getHeight(4.8f * scale) + 1.5f * scale;
                float boxY = curY + titleH;
                float boxH = 13.0f * scale;

                // Click on dropdown bar: toggle open/close
                if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x, boxY, w, boxH)) {
                    if (openMultiDropdown == multi) {
                        openMultiDropdown = null;
                    } else {
                        openMultiDropdown = multi;
                        openModeDropdown = null;
                    }
                    GuiSounds.button();
                    return true;
                }

                float total = titleH + boxH + 1.5f * scale;

                String animKey = "" + ":" + multi.name;
                Animation openAnim = dropdownAnims.get(animKey);
                float prog = (openAnim != null) ? openAnim.getOutput() : (openMultiDropdown == multi ? 1.0f : 0.0f);

                // If open, check clicks on items
                if (prog > 0.4f) {
                    float menuY = boxY + boxH + 1.5f * scale;
                    float itemH = 11.5f * scale;
                    float menuH = multi.settings.size() * itemH + 2.0f * scale;

                    Collection<BooleanSetting> options = multi.settings;
                    int i = 0;
                    for (BooleanSetting b : options) {
                        float rowY = menuY + 1.0f * scale + i * itemH;
                        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x, rowY, w, itemH)) {
                            b.set(!b.get());
                            GuiSounds.chipMulti(b.get());
                            return true;
                        }
                        i++;
                    }
                }

                if (prog > 0.001f) {
                    float itemH = 11.5f * scale;
                    float menuH = multi.settings.size() * itemH + 2.0f * scale;
                    total += (menuH + 1.5f * scale) * prog;
                }

                curY += total;
            } else if (setting instanceof SliderSetting slider) {
                float labelH = Fonts.sf_bold.getHeight(4.8f * scale);
                float trackY = curY + labelH + 2.6f * scale;
                float trackH = 1.4f * scale;
                float knobD = 2.6f * scale;
                float knobR = knobD / 2.0f;
                float usableW = w - 2.0f * knobR;

                if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x - 2.0f * scale, trackY - 4.0f * scale, w + 4.0f * scale, trackH + 8.0f * scale)) {
                    draggingSlider = slider;
                    sliderTrackX = x + knobR;
                    sliderTrackW = usableW;
                    float percent = MathHelper.clamp((mouseX - sliderTrackX) / sliderTrackW, 0.0f, 1.0f);
                    float val = slider.minimum + (slider.maximum - slider.minimum) * percent;
                    if (slider.increment > 0) {
                        val = Math.round(val / slider.increment) * slider.increment;
                    }
                    slider.current = MathHelper.clamp(val, slider.minimum, slider.maximum);
                    GuiSounds.sliderGrab();
                    return true;
                }

                curY += labelH + 6.5f * scale;
            } else if (setting instanceof BooleanSetting bool) {
                float rowH = 9.6f * scale;
                if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x, curY, w, rowH)) {
                    bool.set(!bool.get());
                    GuiSounds.toggle(bool.get());
                    return true;
                }
                curY += rowH + 0.4f * scale;
            } else if (setting instanceof BindSettings bind) {
                float rowH = 9.6f * scale;
                if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x + w - 30.0f * scale, curY, 30.0f * scale, rowH)) {
                    activeBind = bind;
                    GuiSounds.bindStart();
                    return true;
                }
                curY += rowH + 0.4f * scale;
            } else if (setting instanceof StringSetting str) {
                float rowH = 10.0f * scale;
                if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x + w - 55.0f * scale, curY, 55.0f * scale, rowH)) {
                    activeStringSetting = str;
                    stringEditBuffer = str.get();
                    GuiSounds.editStart();
                    return true;
                }
                curY += rowH + 0.5f * scale;
            } else if (setting instanceof HueSetting color) {
                float rowH = 10.0f * scale;
                curY += rowH + 0.5f * scale;
            }
        }

        // Check Module Keybind row
        float rowH = 9.6f * scale;
        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x + w - 30.0f * scale, curY, 30.0f * scale, rowH)) {
            activeModuleBind = m;
            GuiSounds.bindStart();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingSlider != null) {
            GuiSounds.sliderRelease();
        }
        draggingSlider = null;
        draggingColor = null;
        draggingWindow = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        float mouseX = (float) (click.comp_4798() / scaleFix);
        float mouseY = (float) (click.comp_4799() / scaleFix);

        if (draggingSlider != null && sliderTrackW > 0) {
            float percent = MathHelper.clamp((mouseX - sliderTrackX) / sliderTrackW, 0.0f, 1.0f);
            float newValue = draggingSlider.minimum + (draggingSlider.maximum - draggingSlider.minimum) * percent;
            if (draggingSlider.increment > 0) {
                newValue = Math.round(newValue / draggingSlider.increment) * draggingSlider.increment;
            }
            draggingSlider.set(MathHelper.clamp(newValue, draggingSlider.minimum, draggingSlider.maximum));
            GuiSounds.sliderTick(percent);
            return true;
        }

        if (draggingWindow) {
            targetWindowOffsetX = mouseX - dragStartX;
            targetWindowOffsetY = mouseY - dragStartY;
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseXRaw, double mouseYRaw, double horizontalAmount, double verticalAmount) {
        float mouseX = (float) (mouseXRaw / scaleFix);
        float mouseY = (float) (mouseYRaw / scaleFix);

        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = (int) (mc.getWindow().getScaledWidth() / scaleFix);
        int screenHeight = (int) (mc.getWindow().getScaledHeight() / scaleFix);

        float colW = 96.0f * S;
        float colGap = 9.0f * S;
        float totalColsW = 5 * colW + 4 * colGap;
        float colH = Math.min(screenHeight - 50.0f * S, 285.0f * S);
        float startX = (screenWidth - totalColsW) / 2.0f + windowOffsetX;
        float colY = Math.max(10.0f * S, (screenHeight - colH - 30.0f * S) / 2.0f + windowOffsetY);

        for (int i = 0; i < 5; i++) {
            float cx = startX + i * (colW + colGap);
            if (mouseX >= cx && mouseX <= cx + colW && mouseY >= colY && mouseY <= colY + colH) {
                scrollTargets[i] = MathHelper.clamp(scrollTargets[i] - (float) verticalAmount * 24.0f * S, 0.0f, maxScrolls[i]);
                GuiSounds.scroll();
                return true;
            }
        }

        return super.mouseScrolled(mouseXRaw, mouseYRaw, horizontalAmount, verticalAmount);
    }

    private void resetScrolls() {
        for (int i = 0; i < 5; i++) {
            scrollTargets[i] = 0.0f;
            scrollCurrents[i] = 0.0f;
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.comp_4795();

        // Handle Active Module Bind
        if (activeModuleBind != null) {
            boolean reset = key == 256 || key == 261 || key == 259; // Esc, Del, Backspace
            activeModuleBind.setKey(reset ? -1 : key);
            activeModuleBind = null;
            if (reset) GuiSounds.bindReset();
            else GuiSounds.bindSet();
            return true;
        }

        // Handle Active Setting Bind
        if (activeBind != null) {
            boolean reset = key == 256 || key == 261 || key == 259;
            activeBind.set(reset ? -1 : key);
            activeBind = null;
            if (reset) GuiSounds.bindReset();
            else GuiSounds.bindSet();
            return true;
        }

        // Handle Active String Input
        if (activeStringSetting != null) {
            if (key == 256) { // Esc
                activeStringSetting = null;
                stringEditBuffer = "";
                GuiSounds.searchClear();
            } else if (key == 257 || key == 335) { // Enter
                activeStringSetting.set(stringEditBuffer);
                activeStringSetting = null;
                stringEditBuffer = "";
                GuiSounds.editCommit();
            } else if (key == 259 && !stringEditBuffer.isEmpty()) { // Backspace
                stringEditBuffer = stringEditBuffer.substring(0, stringEditBuffer.length() - 1);
                GuiSounds.erase();
            }
            return true;
        }

        // Handle Active Search Input
        if (searchActive) {
            if (key == 256) { // Esc
                if (!searchQuery.isEmpty()) {
                    searchQuery = "";
                    resetScrolls();
                } else {
                    searchActive = false;
                }
                GuiSounds.searchClear();
            } else if (key == 257 || key == 335) { // Enter
                searchActive = false;
                GuiSounds.editCommit();
            } else if (key == 259 && !searchQuery.isEmpty()) { // Backspace
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                resetScrolls();
                GuiSounds.erase();
            }
            return true;
        }

        // Ctrl + F search toggle
        boolean isControl = (input.comp_4797() & 2) != 0 || (input.comp_4797() & 8) != 0;
        if (key == 70 && isControl) {
            searchActive = !searchActive;
            if (searchActive) {
                searchTypeTime = System.currentTimeMillis();
                GuiSounds.editStart();
            } else {
                searchQuery = "";
                resetScrolls();
                GuiSounds.searchClear();
            }
            return true;
        }

        // ClickGui Key toggle
        // ClickGui clickGui = Client.get() != null && Client.get().moduleManager() != null ? Client.get().moduleManager().get(ClickGui.class) : null;
        if (false) { // ClickGui module not available
            if (System.currentTimeMillis() - openTime > 250) {
                shouldCloseOnEsc();
            }
            return true;
        }

        // Esc close
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            shouldCloseOnEsc();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (activeStringSetting != null) {
            if (input.isValidChar()) {
                stringEditBuffer += input.asString();
                GuiSounds.type();
            }
            return true;
        }

        if (searchActive) {
            if (input.isValidChar() && searchQuery.length() < SEARCH_LIMIT) {
                searchQuery += input.asString();
                resetScrolls();
                GuiSounds.type();
            }
            return true;
        }

        return super.charTyped(input);
    }

    private void updateMovementKeys() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getWindow() == null || mc.options == null) return;

        KeyBinding[] movementKeys = {
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey,
                mc.options.sprintKey,
                mc.options.sneakKey
        };

        boolean canMove = !isTyping();
        for (KeyBinding keyBinding : movementKeys) {
            if (keyBinding == null) continue;
            boolean isPhysicalPressed = canMove && InputUtil.isKeyPressed(mc.getWindow(), keyBinding.getDefaultKey().getCode());
            keyBinding.setPressed(isPhysicalPressed);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (activeBind != null || activeModuleBind != null) {
            return false;
        }
        if (!exit) {
            globalAnim.run(0, 0.20F, Easings.SINE_IN);
            exit = true;
            GuiSounds.close();
        }
        return false;
    }

    private void closeCheck() {
        if (exit && globalAnim.isFinished()) {
            close();
            exit = false;
        }
    }

    @Override
    public void close() {
        super.close();
        activeBind = null;
        activeModuleBind = null;
        activeStringSetting = null;
        openMultiDropdown = null;
        openModeDropdown = null;
        draggingSlider = null;
        draggingWindow = false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.options != null) {
            KeyBinding[] movementKeys = {
                    mc.options.forwardKey,
                    mc.options.backKey,
                    mc.options.leftKey,
                    mc.options.rightKey,
                    mc.options.jumpKey,
                    mc.options.sprintKey,
                    mc.options.sneakKey
            };
            for (KeyBinding keyBinding : movementKeys) {
                if (keyBinding != null) {
                    keyBinding.setPressed(false);
                }
            }
        }
    }
}
