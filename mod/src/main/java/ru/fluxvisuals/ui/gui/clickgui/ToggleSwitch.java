package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.function.Supplier;

public class ToggleSwitch extends AbstractWidget {
    public final float switchWidth;
    public final float switchHeight;
    public final Supplier<Boolean> stateSupplier;
    public Runnable clickRunnable;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public final ToggleAnimator toggleSwitchAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    public boolean lastState = false;

    public ToggleSwitch(float f, float f2, Supplier<Boolean> supplier) {
        this.switchWidth = f;
        this.switchHeight = f2;
        this.stateSupplier = supplier;
        this.clickable.clickCallback(this::invert);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        boolean zBooleanValue = this.stateSupplier.get().booleanValue();
        if (zBooleanValue != this.lastState) {
            this.toggleSwitchAnimation.state(zBooleanValue);
            this.lastState = zBooleanValue;
        }
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        int i = colorStack115.toggleDarkenedInterpolateColor(colorStack115.computeColor(themePalette.surfaceBackground().tone(500).argb()), colorStack115.computeColor(themePalette.accent().argb()), 0.05f, this.clickable.hoverAnimation(), this.toggleSwitchAnimation);
        drawCtx.fillOutlinedRoundedRect(x(), y(), this.switchWidth, this.switchHeight, 9.0f, 2.5f, colorStack115.toggleDarkenedInterpolateColor(colorStack115.computeColor(themePalette.surfaceOutline().tone(300).argb()), colorStack115.computeColor(themePalette.accentBright().argb()), 0.05f, this.clickable.hoverAnimation(), this.toggleSwitchAnimation), i);
        float f = (this.switchHeight - (2.0f * 3.0f)) / 2.0f;
        drawCtx.circle(x() + 3.0f + f + (((this.switchWidth - (2.0f * 3.0f)) - (2.0f * f)) * this.toggleSwitchAnimation.smoothAnimation()), y() + (this.switchHeight / 2.0f), f, colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(200).argb()), colorStack115.white(), this.toggleSwitchAnimation));
    }

    public void invert() {
        if (this.clickRunnable != null) {
            this.clickRunnable.run();
        }
    }

    public void setClickableArea(float f, float f2, float f3, float f4) {
        this.clickable.setDimensions(f, f2, f3, f4);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.switchWidth, this.switchHeight);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.toggleSwitchAnimation.animate(weightedEngine);
        this.clickable.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = super.handleInput(inputEventContext, z);
        if (!zHandleInput) {
            zHandleInput = this.clickable.handleInput(inputEventContext, z);
        }
        return zHandleInput;
    }

    public ToggleAnimator toggleSwitchAnimation() {
        return this.toggleSwitchAnimation;
    }

    public ToggleSwitch runnable(Runnable runnable) {
        this.clickRunnable = runnable;
        return this;
    }
}
