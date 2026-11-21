package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
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
import ru.fluxvisuals.ui.gui.clickgui.events.impl.LanguageChangeEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.ModuleFrame;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
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

public class KeybindSettingElement extends ModuleFrame {
    static final int nameFontSize = 14;
    static final int descriptionFontSize = 13;
    static final int bindingFontSize = 12;
    static final float l = 3.0f;
    static final float m = 10.0f;
    static final float n = 25.0f;
    static final float o = 8.0f;
    final Translation name;
    final Translation description;
    Supplier<Boolean> visibilitySupplier;
    String wrappedDescription;
    String cachedDescriptionSource;
    float descriptionHeight;
    public final Supplier<List<Integer>> keysSupplier;
    final MsdfFont semiBoldFont = Fonts.INTER_SEMIBOLD.get();
    final MsdfFont mediumFont = Fonts.INTER_MEDIUM.get();
    final GlTexture keyboardIcon = new GlTexture(new ClasspathResource("/icons/menu/new/keyboard.png"));
    final EventCallback<LanguageChangeEvent> languageChangeCallback = event -> {
        invalidateDescriptionCache();
    };
    final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    final ClickableBehavior clickableBehavior = new ClickableBehavior();
    final WidgetBounds bindingBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    final KeybindCaptureState captureState = new KeybindCaptureState();
    final ToggleAnimator activeToggleAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    final AnimatedFloat widthAnimator = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
    boolean widthInitialized = false;
    float scaleFactor = 1.0f;
    float cachedLayoutWidth = -1.0f;
    boolean descriptionDirty = true;

    public KeybindSettingElement(Translation translation, Translation class254Var2, Supplier<List<Integer>> supplier) {
        this.name = translation;
        this.description = class254Var2;
        this.keysSupplier = supplier;
        ClickableBehavior clickableBehavior766 = this.clickableBehavior;
        KeybindCaptureState keybindCaptureState = this.captureState;
        Objects.requireNonNull(keybindCaptureState);
        clickableBehavior766.clickCallback(keybindCaptureState::toggleCapture);
    }

    public void onChange(Consumer<List<Integer>> consumer) {
        this.captureState.onChange(consumer);
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
    public void draw(DrawCtx drawCtx, float f, float f2) {
        if (this.captureState.captureKey()) {
            drawCtx.window().interceptKeyboard(true);
        }
        updateDescriptionLayout(f);
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        float fScaleFactor = drawCtx.layoutContext().scaleFactor();
        float fMethod006 = measureTextWidth(this.name.effective(), nameFontSize, fScaleFactor);
        float f3 = f / 2.0f;
        boolean z = fMethod006 >= (f - f3) + m || this.wrappedDescription != null;
        float fY = y();
        if (z) {
            fY = drawDescriptionText(drawCtx, drawNameLabel(drawCtx, fY));
        }
        String bindingText = this.captureState.formatBindingText();
        float fMethod007 = measureTextWidth(bindingText, bindingFontSize, fScaleFactor);
        float height = this.semiBoldFont.getHeight(12.0f);
        boolean zHasAnyKeys = this.captureState.hasAnyKeys();
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceOutline().tone(300).argb()), this.clickableBehavior.hoverAnimation());
        int iInterpolate2 = colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(500).argb()), this.clickableBehavior.hoverAnimation());
        float f4 = !zHasAnyKeys ? 6.0f : 0.0f;
        float fWidth = !zHasAnyKeys ? this.keyboardIcon.width() : 0.0f;
        float fWidth2 = z ? (((this.bindingBounds.width() - 20.0f) - fWidth) - f4) - 60.0f : (f - f3) - 30.0f;
        String strMethod005 = bindingText;
        if (fMethod007 > fWidth2) {
            strMethod005 = truncateWithEllipsis(bindingText, fWidth2, fScaleFactor);
        }
        float fMethod008 = measureTextWidth(strMethod005, bindingFontSize, fScaleFactor);
        float f5 = fWidth + f4 + fMethod008;
        float fY2 = this.bindingBounds.y() + (this.bindingBounds.height() / 2.0f);
        int iInterpolate3 = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(600).argb()), colorStack115.computeColor(themePalette.text().tone(300).argb()), this.activeToggleAnimator.smoothAnimation());
        int iInterpolate4 = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(600).argb()), colorStack115.white(), this.activeToggleAnimator.smoothAnimation());
        if (z) {
            this.bindingBounds.withPosition(x(), fY + m).withSize(f, n);
            float fX = this.bindingBounds.x() + ((this.bindingBounds.width() - f5) / 2.0f);
            drawCtx.fillOutlinedRoundedRect(this.bindingBounds.x(), this.bindingBounds.y(), this.bindingBounds.width(), this.bindingBounds.height(), 6.0f, 2.5f, iInterpolate, iInterpolate2);
            drawCtx.textureVerticalC(this.keyboardIcon, fX - (((fX - this.bindingBounds.x()) - m) * this.activeToggleAnimator.smoothAnimation()), fY2, this.keyboardIcon.width(), this.keyboardIcon.height(), iInterpolate4);
            drawCtx.text(this.semiBoldFont, strMethod005, bindingFontSize, fX + fWidth + f4, fY2 - (height / 2.0f), iInterpolate3);
        } else {
            float fAnimatedValue = this.widthAnimator.animatedValue();
            this.bindingBounds.withPosition((x() + f) - fAnimatedValue, y()).withSize(fAnimatedValue, n);
            float fX2 = this.bindingBounds.x() + (((f5 + 16.0f) - f5) / 2.0f);
            drawCtx.fillOutlinedRoundedRect(this.bindingBounds.x(), this.bindingBounds.y(), this.bindingBounds.width(), this.bindingBounds.height(), 6.0f, 2.5f, iInterpolate, iInterpolate2);
            float f6 = fX2 + fWidth + f4;
            if (f6 + fMethod008 > (this.bindingBounds.x() + fAnimatedValue) - 5.0f) {
                drawCtx.drawEngine().beginStencil();
                drawCtx.fillOutlinedRoundedRect(this.bindingBounds.x(), this.bindingBounds.y(), this.bindingBounds.width(), this.bindingBounds.height(), 6.0f, 2.5f, iInterpolate, iInterpolate2);
                drawCtx.drawEngine().prepareStencil(1);
                drawCtx.textureVerticalC(this.keyboardIcon, fX2, fY2, this.keyboardIcon.width(), this.keyboardIcon.height(), iInterpolate3);
                drawCtx.text(this.semiBoldFont, strMethod005, bindingFontSize, f6, fY2 - (height / 2.0f), iInterpolate3);
                drawCtx.drawEngine().endStencil();
            } else {
                if (!zHasAnyKeys) {
                    drawCtx.textureVerticalC(this.keyboardIcon, fX2, fY2, this.keyboardIcon.width(), this.keyboardIcon.height(), iInterpolate3);
                }
                drawCtx.text(this.semiBoldFont, strMethod005, bindingFontSize, f6, fY2 - (height / 2.0f), iInterpolate3);
            }
            drawCtx.text(this.semiBoldFont, this.name.effective(), nameFontSize, x(), fY2 - (this.semiBoldFont.getHeight(14.0f) / 2.0f), colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(200).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), this.highlightAnimation.value()));
        }
        this.clickableBehavior.setDimensions(x(), y(), f, height());
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        String bindingText = this.captureState.formatBindingText();
        this.scaleFactor = layoutScaleContext.scaleFactor();
        float fMethod006 = measureTextWidth(bindingText, bindingFontSize, this.scaleFactor);
        float fWidth = (this.parent.width() - (this.parent.width() / 2.0f)) - 30.0f;
        String strMethod005 = bindingText;
        if (fMethod006 > fWidth) {
            strMethod005 = truncateWithEllipsis(bindingText, fWidth, this.scaleFactor);
        }
        float fMethod007 = measureTextWidth(strMethod005, bindingFontSize, this.scaleFactor);
        boolean zHasAnyKeys = this.captureState.hasAnyKeys();
        float fWidth2 = (!zHasAnyKeys ? this.keyboardIcon.width() : 0.0f) + (!zHasAnyKeys ? 6.0f : 0.0f) + fMethod007 + 16.0f;
        if (!this.widthInitialized) {
            this.widthAnimator.set(fWidth2);
            this.widthInitialized = true;
        }
        this.widthAnimator.destination(fWidth2);
        this.keyboardIcon.setDimensions(bindingFontSize, bindingFontSize);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = super.handleInput(inputEventContext, z);
        boolean zHandleInput2 = zHandleInput | this.clickableBehavior.handleInput(inputEventContext, zHandleInput);
        if (!z) {
            if (this.captureState.captureKey()) {
                InputEvent inputEvent691 = inputEventContext.inputEvent();
                if (inputEvent691 instanceof MouseButtonInput) {
                    MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent691;
                    if (mouseButtonInput.action().press() && mouseButtonInput.button() == 0 && !inputEventContext.inArea(this.clickableBehavior.ownerX(), this.clickableBehavior.ownerY(), this.clickableBehavior.ownerW(), this.clickableBehavior.ownerH())) {
                        this.captureState.toggleCapture();
                        zHandleInput2 = true;
                    }
                }
            }
            if (!zHandleInput2) {
                InputEvent class691VarInputEvent2 = inputEventContext.inputEvent();
                if (class691VarInputEvent2 instanceof KeyInput) {
                    zHandleInput2 = this.captureState.handleKeyInput((KeyInput) class691VarInputEvent2);
                }
                if (class691VarInputEvent2 instanceof MouseButtonInput) {
                    zHandleInput2 = this.captureState.handleMouseInput((MouseButtonInput) class691VarInputEvent2);
                }
            }
            zHandleInput2 |= this.captureState.captureKey() && !zHandleInput2;
        }
        return zHandleInput2;
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibilitySupplier != null) {
            visible(this.visibilitySupplier.get().booleanValue());
        }
        this.captureState.syncKeys(this.keysSupplier.get());
        this.activeToggleAnimator.state(this.captureState.hasAnyKeys());
        this.activeToggleAnimator.animate(weightedEngine);
        this.widthAnimator.animate(weightedEngine);
        this.clickableBehavior.animate(weightedEngine);
        this.highlightAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public void handleClose() {
        super.handleClose();
    }

    @Override
    public float height() {
        float fMax;
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateDescriptionLayout(this.parent.width());
        }
        float fMethod006 = measureTextWidth(this.name.effective(), nameFontSize, this.scaleFactor);
        float fWidth = (this.parent.width() - (this.parent.width() / 2.0f)) + m;
        if (this.wrappedDescription != null || fMethod006 >= fWidth) {
            float height = 0.0f + this.semiBoldFont.getHeight(14.0f);
            if (this.description != null && this.wrappedDescription != null) {
                height += l + this.descriptionHeight;
            }
            fMax = height + 35.0f;
        } else {
            fMax = Math.max(this.semiBoldFont.getHeight(14.0f), n);
        }
        return fMax;
    }

    public String truncateWithEllipsis(String str, float f, float f2) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        float fMethod006 = f - measureTextWidth("...", bindingFontSize, f2);
        if (fMethod006 <= 0.0f) {
            return "...";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (measureTextWidth(sb.toString() + str.charAt(i), bindingFontSize, f2) > fMethod006) {
                return String.valueOf(sb) + "...";
            }
            sb.append(str.charAt(i));
        }
        return str;
    }

    public float measureTextWidth(String str, int i, float f) {
        return this.semiBoldFont.getWidth(str, Math.max(1, Math.round(i * f))) / f;
    }

    public float drawNameLabel(DrawCtx drawCtx, float f) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        drawCtx.text(this.semiBoldFont, this.name.effective(), nameFontSize, x(), f, colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(200).argb()), colorStack115.computeColor(themePalette.text().tone(50).argb()), this.highlightAnimation.value()));
        return f + this.semiBoldFont.getHeight(14.0f);
    }

    public float drawDescriptionText(DrawCtx drawCtx, float f) {
        if (this.wrappedDescription == null) {
            return f;
        }
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(300).argb()), this.highlightAnimation.value());
        float f2 = f + l;
        drawCtx.text(this.mediumFont, this.wrappedDescription, descriptionFontSize, x(), f2, iInterpolate);
        return f2 + this.descriptionHeight;
    }

    public void invalidateDescriptionCache() {
        this.descriptionDirty = true;
        this.cachedLayoutWidth = -1.0f;
        this.cachedDescriptionSource = null;
    }

    public void updateDescriptionLayout(float f) {
        if (this.description == null) {
            clearDescription(f);
            return;
        }
        String strEffective = this.description.effective();
        if (isDescriptionCacheValid(f, strEffective)) {
            return;
        }
        formatDescription(f, strEffective == null ? "" : strEffective);
    }

    public void clearDescription(float f) {
        this.wrappedDescription = null;
        this.descriptionHeight = 0.0f;
        this.descriptionDirty = false;
        this.cachedLayoutWidth = f;
        this.cachedDescriptionSource = null;
    }

    public boolean isDescriptionCacheValid(float f, String str) {
        return !this.descriptionDirty && f == this.cachedLayoutWidth && str != null && str.equals(this.cachedDescriptionSource);
    }

    public void formatDescription(float f, String str) {
        this.wrappedDescription = StringUtil.formatTextToFitWidth(str, f, this.mediumFont, descriptionFontSize);
        this.descriptionHeight = this.mediumFont.getHeightWithLineBreaks(this.wrappedDescription, descriptionFontSize);
        this.cachedLayoutWidth = f;
        this.cachedDescriptionSource = str;
        this.descriptionDirty = false;
    }

    public Translation name() {
        return this.name;
    }

    public KeybindSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }
}
