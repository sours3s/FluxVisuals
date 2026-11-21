package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

public class LabeledIconButton extends AbstractWidget {
    public final MsdfFont font;
    public final GlTexture icon;
    public final float cornerRadius;
    public final float paddingX;
    public final float paddingY;
    public final int iconSize;
    public final Translation label;
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final int textSize = 12;

    public LabeledIconButton(Translation translation, Runnable runnable, MsdfFont msdfFont, GlTexture glTexture, float f, float f2, float f3, int i) {
        this.label = translation;
        this.font = msdfFont;
        this.icon = glTexture;
        this.cornerRadius = f;
        this.paddingX = f2;
        this.paddingY = f3;
        this.iconSize = i;
        this.clickBehavior.clickCallback(() -> {
            if (runnable != null) {
                runnable.run();
            }
        });
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        float fWidth = width();
        float fHeight = height();
        ToggleAnimator toggleAnimator = this.clickBehavior.hoverAnimation();
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(themePalette.surfaceBackground().tone(700).argb(), 0.0f), colorStack115.computeColor(themePalette.surfaceBackground().tone(700).argb(), 140), toggleAnimator);
        int iInterpolate2 = colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(500).argb()), colorStack115.computeColor(themePalette.text().tone(400).argb()), toggleAnimator);
        drawCtx.fillOutlinedRoundedRect(x(), y(), fWidth, fHeight, this.cornerRadius, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), iInterpolate);
        drawCtx.texture(this.icon, x() + this.paddingX, (y() + (fHeight / 2.0f)) - (this.iconSize / 2), this.iconSize, this.iconSize, colorStack115.computeColor(iInterpolate2));
        drawCtx.text(this.font, this.label.effective(), 12, x() + this.paddingX + this.iconSize + 5.0f, (y() + (fHeight / 2.0f)) - (this.font.getHeight(12.0f) / 2.0f), iInterpolate2);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return this.clickBehavior.handleInput(inputEventContext, z);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.iconSize + 5.0f + layoutScaleContext.textWidthPhysical(this.font, this.label.effective(), 12) + (this.paddingX * 2.0f), Math.max(this.iconSize, this.font.metrics().lineHeight()) + (this.paddingY * 2.0f));
        this.clickBehavior.setDimensions(x(), y(), width(), height());
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.clickBehavior.animate(weightedEngine);
    }
}
