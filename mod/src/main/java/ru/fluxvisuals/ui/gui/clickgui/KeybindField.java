package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.KeyInput;
import ru.fluxvisuals.ui.gui.clickgui.util.type.KeybindCaptureState;
import ru.fluxvisuals.ui.gui.clickgui.util.render.KeybindColors;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class KeybindField extends AbstractWidget {
    public final MsdfFont font;
    public final float paddingX;
    public final float paddingY;
    public final float cornerRadius;
    public final int fontSize;
    public boolean initialized;
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final AnimatedFloat widthAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
    public final GlTexture keyboardIcon = new GlTexture(new ClasspathResource("/icons/menu/new/keyboard.png"));
    public final KeybindCaptureState captureState = new KeybindCaptureState();
    public Supplier<Float> rawXSupplier;

    public KeybindColors colorsOverride;

    public KeybindField(MsdfFont msdfFont, Runnable runnable, float f, float f2, float f3, int i) {
        this.font = msdfFont;
        this.paddingX = f;
        this.paddingY = f2;
        this.cornerRadius = f3;
        this.fontSize = i;
        if (runnable != null) {
            this.clickBehavior.rightClickCallback(runnable);
        }
        ClickableBehavior clickableBehavior = this.clickBehavior;
        KeybindCaptureState keybindCaptureState = this.captureState;
        Objects.requireNonNull(keybindCaptureState);
        clickableBehavior.clickCallback(keybindCaptureState::toggleCapture);
    }

    public void onChange(Consumer<List<Integer>> consumer) {
        this.captureState.onChange(consumer);
    }

    public KeybindField addMutableKeys(List<Integer> list) {
        this.captureState.addMutableKeys(list);
        return this;
    }

    public void syncKeys(List<Integer> list) {
        this.captureState.syncKeys(list);
    }

    public void addImmutableKeys(List<Integer> list) {
        this.captureState.addImmutableKeys(list);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        float fComputeDesiredHeight = computeDesiredHeight();
        float fComputeDesiredWidth = computeDesiredWidth(layoutScaleContext);
        if (!this.initialized) {
            this.widthAnimation.set(fComputeDesiredWidth);
            this.initialized = true;
        }
        this.widthAnimation.destination(fComputeDesiredWidth);
        setSize(this.widthAnimation.animatedValue(), fComputeDesiredHeight);
        this.clickBehavior.setDimensions(x(), y(), width(), height());
        this.keyboardIcon.setDimensions(12, 12);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        int iTextEmpty;
        float fWidth;
        float fX;
        if (this.captureState.captureKey()) {
            drawCtx.window().interceptKeyboard(true);
        }
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        String bindingText = this.captureState.formatBindingText();
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.font, bindingText, this.fontSize);
        float height = this.font.getHeight(this.fontSize);
        KeybindColors keybindColors = this.colorsOverride != null ? this.colorsOverride : KeybindColors.fromPalette(themePalette, colorStack115);
        ToggleAnimator toggleAnimator = this.clickBehavior.hoverAnimation();
        int iBrightenedInterpolatedColor = colorStack115.brightenedInterpolatedColor(keybindColors.outline(), 0.05f, toggleAnimator);
        int iBrightenedInterpolatedColor2 = colorStack115.brightenedInterpolatedColor(keybindColors.background(), 0.05f, toggleAnimator);
        drawCtx.fillOutlinedRoundedRect(x(), y(), width(), height(), this.cornerRadius, 2.5f, iBrightenedInterpolatedColor, iBrightenedInterpolatedColor2);
        float fY = (y() + (height() / 2.0f)) - (height / 2.0f);
        float fX2 = x() + this.widthAnimation.animatedValue();
        if (this.captureState.hasAnyKeys()) {
            float fComputeDesiredWidth = computeDesiredWidth(drawCtx.layoutContext());
            iTextEmpty = keybindColors.text();
            fWidth = ((this.rawXSupplier != null ? this.rawXSupplier.get().floatValue() : x()) + (fComputeDesiredWidth / 2.0f)) - (fTextWidthPhysical / 2.0f);
            fX = (x() + (width() / 2.0f)) - (fTextWidthPhysical / 2.0f);
        } else {
            iTextEmpty = keybindColors.textEmpty();
            float fX3 = x() + this.paddingX;
            drawCtx.texture(this.keyboardIcon, fX3, (y() + (height() / 2.0f)) - (this.keyboardIcon.height() / 2), this.keyboardIcon.width(), this.keyboardIcon.height(), iTextEmpty);
            fWidth = fX3 + this.keyboardIcon.width() + 6.0f;
            fX = fWidth;
        }
        if (fX + fTextWidthPhysical <= fX2 - 5.0f) {
            drawCtx.text(this.font, bindingText, this.fontSize, fWidth, fY, iTextEmpty);
            return;
        }
        drawCtx.drawEngine().beginStencil();
        drawCtx.fillOutlinedRoundedRect(x(), y(), width(), height(), this.cornerRadius, 2.5f, iBrightenedInterpolatedColor, iBrightenedInterpolatedColor2);
        drawCtx.drawEngine().prepareStencil(1);
        drawCtx.text(this.font, bindingText, this.fontSize, fWidth, fY, iTextEmpty);
        drawCtx.drawEngine().endStencil();
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.widthAnimation.animate(weightedEngine);
        this.clickBehavior.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = super.handleInput(inputEventContext, z);
        if (!z) {
            if (this.captureState.captureKey()) {
                InputEvent inputEvent691 = inputEventContext.inputEvent();
                if (inputEvent691 instanceof MouseButtonInput) {
                    MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent691;
                    if (mouseButtonInput.action().press() && mouseButtonInput.button() == 0 && !inputEventContext.inArea(x(), y(), width(), height())) {
                        this.captureState.toggleCapture();
                        zHandleInput = true;
                    }
                }
            }
            boolean zHandleInput2 = zHandleInput | this.clickBehavior.handleInput(inputEventContext, zHandleInput);
            if (!zHandleInput2) {
                InputEvent class691VarInputEvent2 = inputEventContext.inputEvent();
                if (class691VarInputEvent2 instanceof KeyInput) {
                    zHandleInput2 = this.captureState.handleKeyInput((KeyInput) class691VarInputEvent2);
                }
                if (class691VarInputEvent2 instanceof MouseButtonInput) {
                    zHandleInput2 = this.captureState.handleMouseInput((MouseButtonInput) class691VarInputEvent2);
                }
            }
            zHandleInput = zHandleInput2 | (this.captureState.captureKey() && !zHandleInput2);
        }
        return zHandleInput;
    }

    public float computeDesiredWidth(LayoutScaleContext layoutScaleContext) {
        float fTextWidthPhysical = layoutScaleContext.textWidthPhysical(this.font, this.captureState.formatBindingText(), Math.max(1, Math.round(this.fontSize * layoutScaleContext.scaleFactor()))) / layoutScaleContext.scaleFactor();
        if (this.captureState.hasAnyKeys()) {
            return fTextWidthPhysical + (this.paddingX * 2.0f);
        }
        return (this.paddingX * 2.0f) + this.keyboardIcon.width() + 6.0f + fTextWidthPhysical;
    }

    public float computeDesiredHeight() {
        float height = this.font.getHeight(this.fontSize);
        return (this.captureState.hasAnyKeys() ? height : Math.max(this.keyboardIcon.height(), height)) + (this.paddingY * 2.0f);
    }

    public KeybindField rawX(Supplier<Float> supplier) {
        this.rawXSupplier = supplier;
        return this;
    }

    public KeybindField colors(KeybindColors keybindColors) {
        this.colorsOverride = keybindColors;
        return this;
    }
}
