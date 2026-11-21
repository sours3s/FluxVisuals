package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorValue;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.StencilBufferUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.function.Supplier;

public class TextureToggleWidget extends AbstractWidget {
    public final GlTexture inactiveTexture;
    public final GlTexture activeTexture;
    public final float textureWidth;
    public final float textureHeight;
    public final Supplier<Boolean> stateSupplier;
    public ColorValue inactiveColorValue;

    public ColorValue activeColorValue;
    public Runnable clickRunnable;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public boolean lastState = false;
    public float hitboxPaddingValue = 0.0f;
    public final ToggleAnimator toggleAnimator = new ToggleAnimator(Easings.EASE_IN_OUT_CUBIC);

    public TextureToggleWidget(GlTexture glTexture, GlTexture class073Var2, Supplier<Boolean> supplier, float f, float f2) {
        this.inactiveTexture = glTexture;
        this.activeTexture = class073Var2;
        this.textureWidth = f;
        this.textureHeight = f2;
        this.stateSupplier = supplier;
        this.clickable.clickCallback(() -> {
            if (this.clickRunnable != null) {
                this.clickRunnable.run();
            }
        });
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.textureWidth, this.textureHeight);
        float f = this.hitboxPaddingValue;
        float f2 = this.hitboxPaddingValue;
        this.clickable.setDimensions(x() - f, y() - f2, width() + (f * 2.0f), height() + (f2 * 2.0f));
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        boolean zBooleanValue = this.stateSupplier.get().booleanValue();
        if (zBooleanValue != this.lastState) {
            this.toggleAnimator.state(zBooleanValue);
            this.lastState = zBooleanValue;
        }
        if (this.inactiveColorValue == null) {
            this.inactiveColorValue = drawCtx.theme().palette().text().tone(500);
        }
        if (this.activeColorValue == null) {
            this.activeColorValue = drawCtx.theme().palette().text().tone(100);
        }
        int iComputeColor = colorStack115.computeColor(this.inactiveColorValue.argb());
        int iComputeColor2 = colorStack115.computeColor(this.activeColorValue.argb());
        int iBrightenedInterpolatedColor = colorStack115.brightenedInterpolatedColor(iComputeColor, 0.1f, this.clickable.hoverAnimation());
        int iDarkenedInterpolatedColor = colorStack115.darkenedInterpolatedColor(iComputeColor2, 0.1f, this.clickable.hoverAnimation());
        float fSmoothAnimation = this.toggleAnimator.smoothAnimation();
        int iMethod001 = applyAlpha(iBrightenedInterpolatedColor, 1.0f - fSmoothAnimation);
        int iMethod002 = applyAlpha(iDarkenedInterpolatedColor, fSmoothAnimation);
        drawCtx.texture(this.inactiveTexture, x(), y(), this.textureWidth, this.textureHeight, iMethod001);
        drawCtx.texture(this.activeTexture, x(), y(), this.textureWidth, this.textureHeight, iMethod002);
    }

    public static int applyAlpha(int i, float f) {
        return (i & 16777215) | (((int) (((i >>> 24) & StencilBufferUtil.STENCIL_MASK) * Math.max(0.0f, Math.min(1.0f, f)))) << 24);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return this.clickable.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.toggleAnimator.animate(weightedEngine);
        this.clickable.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public TextureToggleWidget inactiveColor(ColorValue colorValue) {
        this.inactiveColorValue = colorValue;
        return this;
    }

    public TextureToggleWidget activeColor(ColorValue colorValue) {
        this.activeColorValue = colorValue;
        return this;
    }

    public TextureToggleWidget hitboxPadding(float f) {
        this.hitboxPaddingValue = f;
        return this;
    }

    public TextureToggleWidget runnable(Runnable runnable) {
        this.clickRunnable = runnable;
        return this;
    }
}
