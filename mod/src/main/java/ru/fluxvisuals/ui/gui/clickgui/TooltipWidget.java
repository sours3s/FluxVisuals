package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.RenderCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

public class TooltipWidget extends AbstractWidget {
    static final float cornerRadius = 7.0f;
    static final float outlineThickness = 2.5f;
    static final float contentPadding = 6.0f;
    static final float spacing = 4.0f;
    static final float iconSize = 12.0f;
    static final float iconSpacing = 6.0f;
    public final MsdfFont font = Fonts.INTER_BOLD.get();
    public final ToggleAnimator visibilityAnimator = new ToggleAnimator(100, Easings.LINEAR);
    public final WidgetBounds anchorBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds tooltipBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public Translation text = Translation.clearText("Tooltip!");
    public GlTexture icon = new GlTexture(new ClasspathResource("/icons/menu/new/smile.png"));
    public boolean shown = false;

    public void show(Translation translation, GlTexture glTexture, WidgetBounds widgetBounds) {
        this.text = translation;
        this.icon = glTexture;
        this.anchorBounds.withPosition(widgetBounds.x(), widgetBounds.y()).withSize(widgetBounds.width(), widgetBounds.height());
        this.shown = true;
        this.visibilityAnimator.state(true);
    }

    public void hide() {
        this.shown = false;
        this.visibilityAnimator.state(false);
    }

    public boolean isVisible() {
        return this.shown || !this.visibilityAnimator.isZero();
    }

    @Override
    public void render(DrawCtx drawCtx) {
        if (this.visibilityAnimator.isZero()) {
            return;
        }
        ColorStack colorStack115 = drawCtx.colorStack();
        ThemePalette themePalette = drawCtx.theme().palette();
        float fSmoothAnimation = this.visibilityAnimator.smoothAnimation();
        colorStack115.push();
        colorStack115.alpha(fSmoothAnimation);
        drawCtx.bloom(this.tooltipBounds.x(), this.tooltipBounds.y(), this.tooltipBounds.width(), this.tooltipBounds.height(), 20.0f, drawCtx.getColorAttachmentId(FluxVisuals.INSTANCE.windowController().bloom().getBloomFramebuffer()));
        drawCtx.fillOutlinedRoundedRect(this.tooltipBounds.x(), this.tooltipBounds.y(), this.tooltipBounds.width(), this.tooltipBounds.height(), cornerRadius, outlineThickness, colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()));
        float fX = this.tooltipBounds.x() + 6.0f;
        float fY = this.tooltipBounds.y() + (this.tooltipBounds.height() / 2.0f);
        if (this.icon != null) {
            drawCtx.texture(this.icon, fX, fY - 6.0f, iconSize, iconSize, colorStack115.computeColor(9934746));
            fX += 18.0f;
        }
        drawCtx.text(this.font, this.text.effective(), 11, fX, fY - (this.font.getHeight(11.0f) / 2.0f), colorStack115.computeColor(themePalette.text().tone(500).argb()));
        colorStack115.pop();
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        if (isVisible()) {
            float fTextWidthPhysical = layoutScaleContext.textWidthPhysical(this.font, this.text.effective(), 11);
            float height = this.font.getHeight(11.0f);
            boolean z = this.icon != null;
            float f = iconSize + fTextWidthPhysical + (z ? 18.0f : 0.0f);
            this.tooltipBounds.withSize(f, 8.0f + Math.max(height, z ? iconSize : 0.0f));
            this.tooltipBounds.withPosition((this.anchorBounds.x() + (this.anchorBounds.width() / 2.0f)) - (f / 2.0f), (this.anchorBounds.y() - this.tooltipBounds.height()) - 10.0f);
        }
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.visibilityAnimator.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public void updateAnchor(WidgetBounds widgetBounds) {
        this.anchorBounds.withPosition(widgetBounds.x(), widgetBounds.y()).withSize(widgetBounds.width(), widgetBounds.height());
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        renderCommandQueue.record(drawCtx -> {
            if (this.visibilityAnimator.isZero()) {
                return;
            }
            ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
            ThemePalette themePalette = drawCtx.theme().palette();
            colorStack115.push();
            colorStack115.alpha(this.visibilityAnimator.smoothAnimation());
            drawCtx.fillOutlinedRoundedRect(this.tooltipBounds.x(), this.tooltipBounds.y(), this.tooltipBounds.width(), this.tooltipBounds.height(), cornerRadius, outlineThickness, colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()));
            colorStack115.pop();
        });
        super.collectBloomElements(renderCommandQueue);
    }
}
