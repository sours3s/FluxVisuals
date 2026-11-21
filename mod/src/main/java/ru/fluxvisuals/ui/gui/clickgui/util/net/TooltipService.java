package ru.fluxvisuals.ui.gui.clickgui.util.net;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.TooltipWidget;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;
import ru.fluxvisuals.ui.gui.clickgui.WidgetContainer;

import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class TooltipService {
    public final WidgetContainer container;
    public final Map<Object, TooltipWidget> tooltips = new HashMap();

    public TooltipService(WidgetContainer widgetContainer) {
        this.container = widgetContainer;
    }

    public void show(Object obj, WidgetBounds widgetBounds, Translation translation, GlTexture glTexture) {
        this.tooltips.computeIfAbsent(obj, obj2 -> {
            TooltipWidget tooltipWidget = new TooltipWidget();
            this.container.addChild(tooltipWidget);
            return tooltipWidget;
        }).show(translation, glTexture, widgetBounds);
    }

    public void hide(Object obj) {
        TooltipWidget tooltipWidget = this.tooltips.get(obj);
        if (tooltipWidget != null) {
            tooltipWidget.hide();
        }
    }

    public void updateAnchor(Object obj, WidgetBounds widgetBounds, DrawCtx drawCtx) {
        TooltipWidget tooltipWidget = this.tooltips.get(obj);
        if (tooltipWidget == null || !tooltipWidget.isVisible()) {
            return;
        }
        tooltipWidget.updateAnchor(toScreenRect(drawCtx, widgetBounds.x(), widgetBounds.y(), widgetBounds.width(), widgetBounds.height()));
    }

    public static WidgetBounds toScreenRect(DrawCtx drawCtx, float f, float f2, float f3, float f4) {
        Matrix4f positionMatrix = drawCtx.matrixStack().peek().getPositionMatrix();
        DrawEngine drawEngine154 = drawCtx.drawEngine();
        Vector4f vector4fMul = new Vector4f(f, f2, 0.0f, 1.0f).mul(positionMatrix);
        Vector4f vector4fMul2 = new Vector4f(f + f3, f2 + f4, 0.0f, 1.0f).mul(positionMatrix);
        return new WidgetBounds(drawEngine154.transformX(positionMatrix, f), drawEngine154.transformY(positionMatrix, f2), vector4fMul2.x - vector4fMul.x, vector4fMul2.y - vector4fMul.y);
    }

    public boolean isOwnedBy(Object obj) {
        TooltipWidget tooltipWidget = this.tooltips.get(obj);
        return tooltipWidget != null && tooltipWidget.isVisible();
    }
}
