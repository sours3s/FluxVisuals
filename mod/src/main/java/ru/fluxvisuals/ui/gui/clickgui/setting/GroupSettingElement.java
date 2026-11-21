package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.ClickableBehavior;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.events.api.EventCallback;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.render.HighlightAnimation;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.KeyInput;
import ru.fluxvisuals.ui.gui.clickgui.util.type.KeybindCaptureState;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.LanguageChangeEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.ModuleFrame;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.RenderCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ScreenResolution;
import ru.fluxvisuals.ui.gui.clickgui.SettingsGroupWindow;
import ru.fluxvisuals.ui.gui.clickgui.util.StringUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.util.math.MatrixStack;

public class GroupSettingElement extends ModuleFrame {
    static final int titleTextSize = 14;
    static final int descTextSize = 13;
    static final int bodyTextSize = 12;
    static final float d = 3.0f;
    static final float e = 10.0f;
    static final float f = 25.0f;
    static final float g = 8.0f;
    static final float i = 8.0f;
    static final float j = 5.0f;
    public final Translation name;
    public final Translation description;
    public Supplier<Boolean> toggleSupplier;
    public Supplier<Boolean> visibilitySupplier;
    public Runnable changeCallback;
    public String wrappedDescription;
    public String cachedDescriptionSource;
    public float descriptionHeight;
    public ScreenResolution resolution;

    public final SettingsGroupWindow groupWindow;
    public final MsdfFont semiBoldFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont mediumFont = Fonts.INTER_MEDIUM.get();
    public final GlTexture keyboardIcon = new GlTexture(new ClasspathResource("/icons/menu/new/keyboard.png"));
    public final GlTexture checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
    public final GlTexture groupIcon = new GlTexture(new ClasspathResource("/icons/menu/new/group.png"));
    public final EventCallback<LanguageChangeEvent> languageChangeCallback = event -> {
        resetDescriptionCache();
    };
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final ClickableBehavior toggleClickable = new ClickableBehavior();
    public final ClickableBehavior keybindClickable = new ClickableBehavior();
    public final ClickableBehavior groupClickable = new ClickableBehavior();
    public final ToggleAnimator toggleAnimator = new ToggleAnimator(Easings.EASE_IN_OUT_CUBIC);
    public final KeybindCaptureState keybindCapture = new KeybindCaptureState();
    public final WidgetBounds bindingBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds groupButtonBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public boolean lastToggleState = false;
    public float lastDescriptionWidth = -1.0f;
    public boolean descriptionDirty = true;
    public float k = 0.0f;

    public GroupSettingElement(Translation translation, Translation class254Var2, List<Setting> list, List<Integer> list2) {
        this.name = translation;
        this.description = class254Var2;
        this.keybindCapture.addMutableKeys(list2);
        this.toggleClickable.clickCallback(this::invert);
        ClickableBehavior clickableBehavior = this.keybindClickable;
        KeybindCaptureState keybindCaptureState = this.keybindCapture;
        Objects.requireNonNull(keybindCaptureState);
        clickableBehavior.clickCallback(keybindCaptureState::toggleCapture);
        this.groupWindow = new SettingsGroupWindow(translation, list);
        this.groupClickable.clickCallback(this::toggleGroupWindow);
        FluxVisuals.INSTANCE.menuWindow().priorityOverlayHandler().registerPopup(this.groupWindow);
    }

    public void onChange(Consumer<List<Integer>> consumer) {
        this.keybindCapture.onChange(consumer);
    }

    public void toggleGroupWindow() {
        float fDpiScaleFactor = FluxVisuals.INSTANCE.windowController().dpiScaleFactor();
        ScreenResolution screenResolution = this.resolution != null ? this.resolution : ScreenResolution.resolution();
        float fScreenWidth = screenResolution.screenWidth() / fDpiScaleFactor;
        float fScreenHeight = screenResolution.screenHeight() / fDpiScaleFactor;
        float fWidth = this.groupWindow.width();
        float f2 = this.groupWindow.totalHeight();
        float fX = this.groupButtonBounds.x() + this.groupButtonBounds.width() + 8.0f;
        float fX2 = (this.groupButtonBounds.x() - 8.0f) - fWidth;
        if (fX + fWidth > fScreenWidth - j && fX2 >= j) {
            fX = fX2;
        }
        float fMax = Math.max(j, Math.min(fX, (fScreenWidth - fWidth) - j));
        float fY = ((this.groupButtonBounds.y() - this.viewportTop) + (this.groupButtonBounds.height() / 2.0f)) - (f2 / 2.0f);
        if (fY + f2 > fScreenHeight - j) {
            fY = (fScreenHeight - f2) - j;
        }
        if (fY < j) {
            fY = 5.0f;
        }
        if (this.groupWindow.isOpen()) {
            this.groupWindow.closeWindow();
        } else {
            this.groupWindow.openWindow();
            this.groupWindow.setPosition(fMax, fY);
        }
    }

    @Override
    public void highlight() {
        this.highlightAnimation.trigger();
    }

    @Override
    public void clearHighlight() {
        this.highlightAnimation.clearHighlight();
    }

    @Override
    public void draw(DrawCtx drawCtx, float f2, float f3) {
        if (this.keybindCapture.captureKey()) {
            drawCtx.window().interceptKeyboard(true);
        }
        this.resolution = drawCtx.resolution();
        updateDescriptionLayout(f2);
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        float fMethod007 = drawDescription(drawCtx, drawTitle(drawCtx, y())) + e;
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceOutline().tone(300).argb()), this.keybindClickable.hoverAnimation());
        int iInterpolate2 = colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(500).argb()), this.keybindClickable.hoverAnimation());
        this.bindingBounds.withPosition(x(), fMethod007).withSize(111.0f, f);
        drawCtx.fillOutlinedRoundedRect(this.bindingBounds.x(), this.bindingBounds.y(), this.bindingBounds.width(), this.bindingBounds.height(), 6.0f, 2.5f, iInterpolate, iInterpolate2);
        String bindingText = this.keybindCapture.formatBindingText();
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.semiBoldFont, bindingText, bodyTextSize);
        float height = this.semiBoldFont.getHeight(12.0f);
        boolean zHasAnyKeys = this.keybindCapture.hasAnyKeys();
        float f4 = !zHasAnyKeys ? 6.0f : 0.0f;
        float fWidth = !zHasAnyKeys ? this.keyboardIcon.width() : 0.0f;
        float fWidth2 = ((this.bindingBounds.width() - 16.0f) - fWidth) - f4;
        String strMethod006 = bindingText;
        if (fTextWidthPhysical > fWidth2) {
            strMethod006 = truncateText(drawCtx, bindingText, fWidth2);
        }
        float fX = this.bindingBounds.x() + ((this.bindingBounds.width() - ((fWidth + f4) + drawCtx.textWidthPhysical(this.semiBoldFont, strMethod006, bodyTextSize))) / 2.0f);
        float fY = this.bindingBounds.y() + (this.bindingBounds.height() / 2.0f);
        int iComputeColor = zHasAnyKeys ? colorStack115.computeColor(themePalette.text().tone(300).argb()) : colorStack115.computeColor(themePalette.text().tone(600).argb());
        if (!zHasAnyKeys) {
            drawCtx.textureVerticalC(this.keyboardIcon, fX, fY, this.keyboardIcon.width(), this.keyboardIcon.height(), iComputeColor);
        }
        drawCtx.text(this.semiBoldFont, strMethod006, bodyTextSize, fX + fWidth + f4, fY - (height / 2.0f), iComputeColor);
        this.groupButtonBounds.withPosition(x() + this.bindingBounds.width() + 6.0f, fMethod007).withSize(111.0f, f);
        drawGroupButton(drawCtx);
        drawToggle(drawCtx, fMethod007, f2);
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        this.groupWindow.collectBloomElements(renderCommandQueue);
        super.collectBloomElements(renderCommandQueue);
    }

    @Override
    public void drawOverlay(DrawCtx drawCtx) {
        if (this.groupWindow.isOpen()) {
            this.groupWindow.render(new DrawCtx(drawCtx.window(), drawCtx.resolution(), new MatrixStack(), drawCtx.mousePosition(), drawCtx.drawEngine(), drawCtx.drawEngine().colorStack(), drawCtx.lastKnownMousePosition(), drawCtx.theme(), drawCtx.layoutContext()));
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.groupClickable.setDimensions(this.groupButtonBounds.x(), this.groupButtonBounds.y(), this.groupButtonBounds.width(), this.groupButtonBounds.height());
        this.keybindClickable.setDimensions(this.bindingBounds.x(), this.bindingBounds.y(), this.bindingBounds.width(), this.bindingBounds.height());
        this.groupIcon.setDimensions(bodyTextSize, bodyTextSize);
        this.groupWindow.layout(layoutScaleContext);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = this.keybindClickable.handleInput(inputEventContext, z);
        if (this.keybindCapture.captureKey()) {
            InputEvent inputEvent691 = inputEventContext.inputEvent();
            if (inputEvent691 instanceof MouseButtonInput) {
                MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent691;
                if (mouseButtonInput.action().press() && mouseButtonInput.button() == 0 && !inputEventContext.inArea(this.bindingBounds.x(), this.bindingBounds.y(), this.bindingBounds.width(), this.bindingBounds.height())) {
                    this.keybindCapture.toggleCapture();
                    zHandleInput = true;
                }
            }
        }
        boolean zHandleInput2 = zHandleInput | this.groupClickable.handleInput(inputEventContext, zHandleInput);
        InputEvent class691VarInputEvent2 = inputEventContext.inputEvent();
        if (class691VarInputEvent2 instanceof KeyInput) {
            zHandleInput2 |= this.keybindCapture.handleKeyInput((KeyInput) class691VarInputEvent2) && !zHandleInput2;
        }
        InputEvent class691VarInputEvent3 = inputEventContext.inputEvent();
        if (class691VarInputEvent3 instanceof MouseButtonInput) {
            zHandleInput2 |= this.keybindCapture.handleMouseInput((MouseButtonInput) class691VarInputEvent3) && !zHandleInput2;
        }
        boolean zHandleInput3 = zHandleInput2 | this.toggleClickable.handleInput(inputEventContext, zHandleInput2);
        return zHandleInput3 | super.handleInput(inputEventContext, zHandleInput3);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        boolean zBooleanValue;
        if (this.toggleSupplier != null && (zBooleanValue = this.toggleSupplier.get().booleanValue()) != this.lastToggleState) {
            this.toggleAnimator.state(zBooleanValue);
            this.lastToggleState = zBooleanValue;
        }
        if (this.visibilitySupplier != null) {
            visible(this.visibilitySupplier.get().booleanValue());
        }
        this.groupWindow.animation(weightedEngine);
        this.groupClickable.animate(weightedEngine);
        this.keybindClickable.animate(weightedEngine);
        this.highlightAnimation.animate(weightedEngine);
        this.toggleClickable.animate(weightedEngine);
        this.toggleAnimator.animate(weightedEngine);
        this.checkmarkIcon.setDimensions(titleTextSize, titleTextSize);
        super.animation(weightedEngine);
    }

    @Override
    public void handleClose() {
        this.groupWindow.closeWindow();
        super.handleClose();
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.resolution != null) {
            updateDescriptionLayout(this.resolution.width());
        }
        float height = this.semiBoldFont.getHeight(14.0f);
        if (this.description != null && this.wrappedDescription != null) {
            height += d + this.descriptionHeight;
        }
        return height + e + f;
    }

    public void invert() {
        if (this.changeCallback != null) {
            this.changeCallback.run();
        }
    }

    public String truncateText(DrawCtx drawCtx, String str, float f2) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        float fTextWidthPhysical = f2 - drawCtx.textWidthPhysical(this.semiBoldFont, "...", bodyTextSize);
        if (fTextWidthPhysical <= 0.0f) {
            return "...";
        }
        StringBuilder sb = new StringBuilder();
        for (int i2 = 0; i2 < str.length(); i2++) {
            if (drawCtx.textWidthPhysical(this.semiBoldFont, sb.toString() + str.charAt(i2), bodyTextSize) > fTextWidthPhysical) {
                return String.valueOf(sb) + "...";
            }
            sb.append(str.charAt(i2));
        }
        return str;
    }

    public float drawTitle(DrawCtx drawCtx, float f2) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        drawCtx.text(this.semiBoldFont, this.name.effective(), titleTextSize, x(), f2, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(200).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), this.highlightAnimation.value()));
        return f2 + this.semiBoldFont.getHeight(14.0f);
    }

    public float drawDescription(DrawCtx drawCtx, float f2) {
        if (this.wrappedDescription == null) {
            return f2;
        }
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(300).argb()), this.highlightAnimation.value());
        float f3 = f2 + d;
        drawCtx.text(this.mediumFont, this.wrappedDescription, descTextSize, x(), f3, iInterpolate);
        return f3 + this.descriptionHeight;
    }

    public void drawGroupButton(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        drawCtx.fillOutlinedRoundedRect(this.groupButtonBounds.x(), this.groupButtonBounds.y(), this.groupButtonBounds.width(), this.groupButtonBounds.height(), 6.0f, 2.5f, colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceOutline().tone(300).argb()), this.groupClickable.hoverAnimation()), colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(500).argb()), this.groupClickable.hoverAnimation()));
        String strEffective = Lang.SHOW_GROUP.effective();
        float fX = this.groupButtonBounds.x() + ((this.groupButtonBounds.width() - ((this.groupIcon.width() + 6.0f) + drawCtx.textWidthPhysical(this.semiBoldFont, strEffective, bodyTextSize))) / 2.0f);
        float fY = this.groupButtonBounds.y() + (this.groupButtonBounds.height() / 2.0f);
        int iComputeColor = colorStack115.computeColor(themePalette.text().tone(500).argb());
        drawCtx.textureVerticalC(this.groupIcon, fX, fY, this.groupIcon.width(), this.groupIcon.height(), iComputeColor);
        drawCtx.text(this.semiBoldFont, strEffective, bodyTextSize, fX + this.groupIcon.width() + 6.0f, fY - (this.semiBoldFont.getHeight(12.0f) / 2.0f), iComputeColor);
    }

    public void drawToggle(DrawCtx drawCtx, float f2, float f3) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        int iComputeColor = colorStack115.computeColor(themePalette.surfaceOutline().tone(500).argb());
        int iComputeColor2 = colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb());
        int iComputeColor3 = colorStack115.computeColor(themePalette.accentBright().argb());
        int iInterpolate = colorStack115.interpolate(colorStack115.interpolate(iComputeColor, iComputeColor2, this.toggleClickable.hoverAnimation()), colorStack115.interpolate(iComputeColor3, colorStack115.autoBrightenDarken(iComputeColor3, 0.1f), this.toggleClickable.hoverAnimation()), this.toggleAnimator);
        int iComputeColor4 = colorStack115.computeColor(themePalette.surfaceBackground().tone(700).argb());
        int iComputeColor5 = colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb());
        int iComputeColor6 = colorStack115.computeColor(themePalette.accent().argb());
        int iInterpolate2 = colorStack115.interpolate(colorStack115.interpolate(iComputeColor4, iComputeColor5, this.toggleClickable.hoverAnimation()), colorStack115.interpolate(iComputeColor6, colorStack115.autoBrightenDarken(iComputeColor6, 0.1f), this.toggleClickable.hoverAnimation()), this.toggleAnimator);
        float fX = (x() + f3) - 18.0f;
        float f4 = (f2 + 12.5f) - (18.0f / 2.0f);
        drawCtx.fillOutlinedRoundedRect(fX, f4, 18.0f, 18.0f, j, 2.5f, iInterpolate, iInterpolate2);
        drawCtx.texture(this.checkmarkIcon, (fX + (18.0f / 2.0f)) - Math.round(this.checkmarkIcon.width() / 2.0f), (f4 + (18.0f / 2.0f)) - Math.round(this.checkmarkIcon.height() / 2.0f), this.checkmarkIcon.width(), this.checkmarkIcon.height(), colorStack115.computeColor(16777215, colorStack115.alphaMultiplier() * this.toggleAnimator.smoothAnimation()));
        this.toggleClickable.setDimensions(x(), y(), f3, height());
    }

    public void resetDescriptionCache() {
        this.descriptionDirty = true;
        this.lastDescriptionWidth = -1.0f;
        this.cachedDescriptionSource = null;
    }

    public void updateDescriptionLayout(float f2) {
        if (this.description == null) {
            clearDescriptionLayout(f2);
            return;
        }
        String strEffective = this.description.effective();
        if (isDescriptionUpToDate(f2, strEffective)) {
            return;
        }
        computeDescriptionLayout(f2, strEffective == null ? "" : strEffective);
    }

    public void clearDescriptionLayout(float f2) {
        this.wrappedDescription = null;
        this.descriptionHeight = 0.0f;
        this.descriptionDirty = false;
        this.lastDescriptionWidth = f2;
        this.cachedDescriptionSource = null;
    }

    public boolean isDescriptionUpToDate(float f2, String str) {
        return !this.descriptionDirty && f2 == this.lastDescriptionWidth && str != null && str.equals(this.cachedDescriptionSource);
    }

    public void computeDescriptionLayout(float f2, String str) {
        this.wrappedDescription = StringUtil.formatTextToFitWidth(str, f2, this.mediumFont, descTextSize);
        this.descriptionHeight = this.mediumFont.getHeightWithLineBreaks(this.wrappedDescription, descTextSize);
        this.lastDescriptionWidth = f2;
        this.cachedDescriptionSource = str;
        this.descriptionDirty = false;
    }

    public Translation name() {
        return this.name;
    }

    public GroupSettingElement toggledSupplier(Supplier<Boolean> supplier) {
        this.toggleSupplier = supplier;
        return this;
    }

    public GroupSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }

    public GroupSettingElement changeRunnable(Runnable runnable) {
        this.changeCallback = runnable;
        return this;
    }
}
