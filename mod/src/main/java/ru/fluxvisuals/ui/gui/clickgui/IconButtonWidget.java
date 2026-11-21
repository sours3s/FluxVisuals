package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.net.TooltipService;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

public class IconButtonWidget extends AbstractWidget {
    public final GlTexture icon;
    public final float iconWidth;
    public final float iconHeight;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public Translation tooltipText = null;
    public GlTexture tooltipIcon = null;
    public boolean tooltipShown = false;
    public int color = -1;

    public IconButtonWidget(GlTexture glTexture, Runnable runnable, float f, float f2) {
        this.clickable.clickCallback(runnable);
        this.icon = glTexture;
        this.iconWidth = f;
        this.iconHeight = f2;
    }

    public IconButtonWidget tooltip(Translation translation, GlTexture glTexture) {
        this.tooltipText = translation;
        this.tooltipIcon = glTexture;
        return this;
    }

    @Override
    public void render(DrawCtx drawCtx) {
        DrawEngine drawEngine154 = drawCtx.drawEngine();
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawEngine154.colorStack();
        if (this.color == -1) {
            this.color = themePalette.text().tone(500).argb();
        }
        int iComputeColor = colorStack115.computeColor(this.color);
        drawCtx.texture(this.icon, x(), y(), this.iconWidth, this.iconHeight, colorStack115.interpolate(iComputeColor, colorStack115.brighten(iComputeColor, 0.8f), this.clickable.hoverAnimation()));
        TooltipService tooltipService736 = FluxVisuals.INSTANCE.menuWindow().tooltipService();
        if (tooltipService736 != null && tooltipService736.isOwnedBy(this) && this.tooltipShown) {
            tooltipService736.updateAnchor(this, bounds(), drawCtx);
        }
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = this.clickable.handleInput(inputEventContext, z);
        boolean zState = this.clickable.hoverAnimation().state();
        TooltipService tooltipService736 = FluxVisuals.INSTANCE.menuWindow().tooltipService();
        if ((inputEventContext.inputEvent() instanceof CursorMoveInput) && tooltipService736 != null && this.tooltipText != null) {
            if (zState && !this.tooltipShown) {
                this.tooltipShown = true;
                tooltipService736.show(this, bounds(), this.tooltipText, this.tooltipIcon);
            } else if (!zState && this.tooltipShown) {
                this.tooltipShown = false;
                tooltipService736.hide(this);
            }
        }
        return zHandleInput;
    }

    public WidgetBounds bounds() {
        return new WidgetBounds(x(), y(), width(), height());
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.iconWidth, this.iconHeight);
        this.clickable.setDimensions(x(), y(), width(), height());
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.clickable.animate(weightedEngine);
    }

    public void setColor(int i) {
        this.color = i;
    }
}
