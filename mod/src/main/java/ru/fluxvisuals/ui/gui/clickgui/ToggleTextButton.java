package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

public class ToggleTextButton extends AbstractWidget {
    public final MsdfFont font;
    public final GlTexture icon;
    public final float cornerRadius;
    public final float paddingX;
    public final float paddingY;
    public final int iconSize;
    public boolean active;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public Translation labelText = Translation.clearText("");
    public int textSize = 12;
    public final ToggleAnimator activeAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);

    public ToggleTextButton(Runnable runnable, MsdfFont msdfFont, GlTexture glTexture, float f, float f2, float f3, int i) {
        this.font = msdfFont;
        this.icon = glTexture;
        this.cornerRadius = f;
        this.paddingX = f2;
        this.paddingY = f3;
        this.iconSize = i;
        this.clickable.clickCallback(() -> {
            toggle();
            if (runnable != null) {
                runnable.run();
            }
        });
    }

    public ToggleTextButton label(Translation translation, int i) {
        this.labelText = translation;
        this.textSize = i;
        return this;
    }

    public void setActive(boolean z) {
        if (this.active == z) {
            return;
        }
        this.active = z;
    }

    public void toggle() {
        setActive(!this.active);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        float fWidth = width();
        float fHeight = height();
        ToggleAnimator toggleAnimator323 = this.clickable.hoverAnimation();
        ToggleAnimator toggleAnimator = this.activeAnimator;
        int iInterpolate = colorStack115.interpolate(colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(700).argb(), 0.0f), colorStack115.computeColor(themePalette.surfaceBackground().tone(700).argb(), 140), toggleAnimator323), colorStack115.computeColor(themePalette.surfaceBackground().tone(700).argb()), toggleAnimator, toggleAnimator);
        int iInterpolate2 = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(400).argb()), toggleAnimator, toggleAnimator);
        drawCtx.fillOutlinedRoundedRect(x(), y(), fWidth, fHeight, this.cornerRadius, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), iInterpolate);
        drawCtx.texture(this.icon, x() + this.paddingX, (y() + (fHeight / 2.0f)) - (this.iconSize / 2), this.iconSize, this.iconSize, colorStack115.computeColor(iInterpolate2));
        drawCtx.text(this.font, this.labelText.effective(), this.textSize, x() + this.paddingX + this.iconSize + 5.0f, (y() + (fHeight / 2.0f)) - (this.font.getHeight(this.textSize) / 2.0f), iInterpolate2);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return this.clickable.handleInput(inputEventContext, z);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.iconSize + 5.0f + layoutScaleContext.textWidthPhysical(this.font, this.labelText.effective(), this.textSize) + (this.paddingX * 2.0f), this.iconSize + this.font.metrics().lineHeight() + (this.paddingY * 2.0f));
        this.clickable.setDimensions(x(), y(), width(), height());
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.clickable.animate(weightedEngine);
        this.activeAnimator.state(this.active).animate(weightedEngine);
    }

    public boolean isActive() {
        return this.active;
    }
}
