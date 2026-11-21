package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.KeyInput;
import ru.fluxvisuals.ui.gui.clickgui.util.type.KeyInputAction;
import ru.fluxvisuals.ui.gui.clickgui.util.KeyboardUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HeaderBindingEditor extends AbstractWidget {
    public final Supplier<List<Integer>> bindingSupplier;
    public final Consumer<List<Integer>> bindingConsumer;
    public boolean capturing;
    public boolean widthInitialized;
    public float aj;
    public float ak;
    public float al;
    public float am;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public final MsdfFont labelFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont bindingFont = Fonts.INTER_BOLD.get();
    public final float ag = 6.0f;
    public final int labelTextSize = 12;
    public final GlTexture keyboardIcon = new GlTexture(new ClasspathResource("/icons/menu/new/keyboard.png")).setDimensions(12, 12);
    public final float ah = 6.0f;
    public final float ai = 10.0f;
    public final ToggleAnimator captureAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public final AnimatedFloat widthAnimation = new AnimatedFloat(220, Easings.LINEAR);
    public final Set<Integer> pressedKeys = new HashSet();
    public final Set<Integer> capturedKeys = new LinkedHashSet();
    public int maxKeys = 2;
    public final int bindingTextSize = 12;

    public HeaderBindingEditor(Supplier<List<Integer>> supplier, Consumer<List<Integer>> consumer) {
        this.bindingSupplier = supplier;
        this.bindingConsumer = consumer;
        this.clickable.clickCallback(this::toggleCapture);
    }

    public HeaderBindingEditor maxBindingKeys(int i) {
        this.maxKeys = Math.max(1, i);
        return this;
    }

    public float preferredWidth(LayoutScaleContext layoutScaleContext) {
        return layoutScaleContext.textWidthPhysical(this.labelFont, Lang.HEADER_BINDING_LABEL.effective(), 12) + 10.0f + this.widthAnimation.animatedValue();
    }

    public float preferredHeight() {
        return Math.max(this.labelFont.metrics().lineHeight(), Math.max(this.bindingFont.getHeight(12.0f), 12.0f) + 12.0f);
    }

    public void toggleCapture() {
        this.capturing = !this.capturing;
        this.captureAnimator.state(this.capturing);
        this.capturedKeys.clear();
        this.pressedKeys.clear();
    }

    public void stopCapture() {
        this.capturing = false;
        this.captureAnimator.state(false);
        this.capturedKeys.clear();
        this.pressedKeys.clear();
    }

    @Override
    public void render(DrawCtx drawCtx) {
        if (this.capturing) {
            drawCtx.window().interceptKeyboard(true);
        }
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.labelFont, Lang.HEADER_BINDING_LABEL.effective(), 12);
        drawCtx.text(this.labelFont, Lang.HEADER_BINDING_LABEL.effective(), 12, x(), this.ak + ((this.am - this.labelFont.getHeight(12.0f)) / 2.0f), colorStack115.computeColor(themePalette.text().tone(600).argb()));
        drawBindingBox(drawCtx, fTextWidthPhysical + 10.0f);
    }

    public void drawBindingBox(DrawCtx drawCtx, float f) {
        DrawEngine drawEngine154 = drawCtx.drawEngine();
        ColorStack colorStack115 = drawEngine154.colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        String strMethod001 = formatBinding();
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.bindingFont, strMethod001, 12);
        ToggleAnimator toggleAnimator = this.clickable.hoverAnimation();
        int iComputeColor = colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb());
        int iInterpolate = colorStack115.interpolate(iComputeColor, colorStack115.brighten(iComputeColor, 0.2f), toggleAnimator);
        int iInterpolate2 = colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(700).argb(), 0.05f), colorStack115.computeColor(themePalette.surfaceBackground().tone(700).argb(), 90), this.captureAnimator, this.captureAnimator);
        float f2 = this.aj;
        float f3 = this.ak;
        float f4 = this.al;
        float f5 = this.am;
        float fSmoothAnimation = 1.0f - (0.4f * this.captureAnimator.smoothAnimation());
        colorStack115.push();
        colorStack115.alpha(fSmoothAnimation);
        drawCtx.fillOutlinedRoundedRect(f2, f3, f4, f5, 6.0f, 2.5f, iInterpolate, iInterpolate2);
        float f6 = f2 + ((f4 - (18.0f + fTextWidthPhysical)) / 2.0f);
        float f7 = f6 + 12.0f + 6.0f;
        float height = f3 + ((f5 - this.bindingFont.getHeight(12.0f)) / 2.0f);
        drawEngine154.beginStencil();
        drawCtx.fillOutlinedRoundedRect(f2, f3, f4, f5, 6.0f, 2.5f, iInterpolate, iInterpolate2);
        drawEngine154.prepareStencil(1);
        drawCtx.texture(this.keyboardIcon, f6, f3 + ((f5 - 12.0f) / 2.0f), 12.0f, 12.0f, colorStack115.computeColor(themePalette.text().tone(300).argb()));
        drawCtx.text(this.bindingFont, strMethod001, 12, f7, height, colorStack115.computeColor(themePalette.text().tone(300).argb()));
        drawEngine154.endStencil();
        colorStack115.pop();
    }

    public String formatBinding() {
        List<Integer> listMethod008 = getBinding();
        if (this.capturing) {
            return "...";
        }
        return listMethod008.isEmpty() ? "NONE" : (String) listMethod008.stream().map((v0) -> {
            return KeyboardUtil.keyToString(v0);
        }).collect(Collectors.joining(" + "));
    }

    public List<Integer> getBinding() {
        List<Integer> list = this.bindingSupplier.get();
        return list == null ? List.of() : list;
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        InputEvent inputEvent691 = inputEventContext.inputEvent();
        if (inputEvent691 instanceof MouseButtonInput) {
            MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent691;
            if (mouseButtonInput.button() == 0 && this.capturing) {
                boolean zInArea = inputEventContext.inArea(this.aj, this.ak, this.al, this.am);
                if (mouseButtonInput.action().press() && !zInArea) {
                    stopCapture();
                    return false;
                }
            }
        }
        boolean zHandleInput = this.clickable.handleInput(inputEventContext, z);
        InputEvent class691VarInputEvent2 = inputEventContext.inputEvent();
        if (class691VarInputEvent2 instanceof KeyInput) {
            KeyInput keyInput = (KeyInput) class691VarInputEvent2;
            if (!z) {
                zHandleInput = handleKeyInput(keyInput) || zHandleInput;
            }
        }
        return zHandleInput;
    }

    public boolean handleKeyInput(KeyInput keyInput) {
        int iKeyCode = keyInput.keyCode();
        KeyInputAction keyInputAction = keyInput.keyAction();
        if (keyInputAction.press()) {
            this.pressedKeys.add(Integer.valueOf(iKeyCode));
            if (!this.capturing) {
                return false;
            }
            if (iKeyCode == 256) {
                stopCapture();
                return true;
            }
            captureKey(iKeyCode);
            return true;
        }
        if (!keyInputAction.release()) {
            return false;
        }
        if (!this.capturing) {
            this.pressedKeys.remove(Integer.valueOf(iKeyCode));
            return false;
        }
        if (iKeyCode != 256) {
            captureKey(iKeyCode);
        }
        this.pressedKeys.remove(Integer.valueOf(iKeyCode));
        if (!this.pressedKeys.isEmpty() || this.capturedKeys.isEmpty()) {
            return true;
        }
        this.bindingConsumer.accept(deduplicateKeys(this.capturedKeys.stream().toList()));
        stopCapture();
        return true;
    }

    public void captureKey(int i) {
        if (this.capturedKeys.contains(Integer.valueOf(i))) {
            return;
        }
        if (this.capturedKeys.size() < this.maxKeys) {
            this.capturedKeys.add(Integer.valueOf(i));
        }
        if (this.capturedKeys.size() == this.maxKeys) {
            this.bindingConsumer.accept(deduplicateKeys(this.capturedKeys.stream().toList()));
            stopCapture();
        }
    }

    public List<Integer> deduplicateKeys(List<Integer> list) {
        ArrayList arrayList = new ArrayList(this.maxKeys);
        for (Integer num : list) {
            if (!arrayList.contains(num)) {
                arrayList.add(num);
                if (arrayList.size() == this.maxKeys) {
                    break;
                }
            }
        }
        return arrayList;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        float fTextWidthPhysical = layoutScaleContext.textWidthPhysical(this.labelFont, Lang.HEADER_BINDING_LABEL.effective(), 12);
        float fLineHeight = this.labelFont.metrics().lineHeight();
        float fTextWidthPhysical2 = layoutScaleContext.textWidthPhysical(this.bindingFont, formatBinding(), 12);
        float f = 18.0f + fTextWidthPhysical2;
        float fMax = Math.max(this.bindingFont.getHeight(12.0f), 12.0f);
        float f2 = f + (12.0f * 2.0f);
        float f3 = fMax + 12.0f;
        if (!this.widthInitialized) {
            this.widthAnimation.set(f2);
            this.widthInitialized = true;
        }
        this.widthAnimation.destination(f2);
        float fAnimatedValue = this.widthAnimation.animatedValue();
        setSize(fTextWidthPhysical + 10.0f + fAnimatedValue, Math.max(fLineHeight, f3));
        this.al = fAnimatedValue;
        this.am = f3;
        this.aj = x() + fTextWidthPhysical + 10.0f;
        this.ak = y();
        this.clickable.setDimensions(this.aj, this.ak, this.al, this.am);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.clickable.animate(weightedEngine);
        this.captureAnimator.animate(weightedEngine);
        this.widthAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }
}
