package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

public class IconTileButton extends AbstractWidget {
    public final ClickableBehavior clickable = new ClickableBehavior();
    public final GlTexture icon;
    public final float cornerRadius;
    public final float paddingX;
    public final float paddingY;
    public float computedWidth;
    public float computedHeight;

    public IconTileButton(Runnable runnable, GlTexture glTexture, float f, float f2, float f3) {
        this.clickable.clickCallback(runnable);
        this.icon = glTexture;
        this.cornerRadius = f;
        this.paddingX = f2;
        this.paddingY = f3;
    }

    @Override
    public void render(DrawCtx drawCtx) {
        DrawEngine drawEngine154 = drawCtx.drawEngine();
        ColorStack colorStack115 = drawEngine154.colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        float fHeight = this.icon.height() + (this.paddingY * 2.0f);
        float fWidth = this.icon.width() + (this.paddingX * 2.0f);
        int iInterpolate = colorStack115.interpolate(colorStack115.computeColor(0, 30, 31, 40), colorStack115.computeColor(140, 30, 31, 40), this.clickable.hoverAnimation());
        int iComputeColor = colorStack115.computeColor(themePalette.text().tone(500).argb());
        drawCtx.fillOutlinedRoundedRect(x(), y(), fWidth, fHeight, this.cornerRadius, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), iInterpolate);
        drawCtx.textureVerticalCHorizontalC(this.icon, x() + (width() / 2.0f), y() + (height() / 2.0f), drawEngine154.colorStack().computeColor(iComputeColor));
        this.computedWidth = fWidth;
        this.computedHeight = fHeight;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.computedWidth, this.computedHeight);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.clickable.animate(weightedEngine);
    }
}
