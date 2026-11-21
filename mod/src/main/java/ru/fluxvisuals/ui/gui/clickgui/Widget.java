package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.events.api.LayoutCallback;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.OverlayCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.model.PixelPoint;
import ru.fluxvisuals.ui.gui.clickgui.util.RenderCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

public abstract class Widget implements LayoutCallback {
    public WidgetContainer parent;
    public WidgetBounds bounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public boolean visible = true;

    public void setPosition(float f, float f2) {
        this.bounds = this.bounds.withPosition(Math.round(f), Math.round(f2));
    }

    public void setSize(float f, float f2) {
        this.bounds = this.bounds.withSize(f, f2);
    }

    public float x() {
        return this.bounds.x();
    }

    public float y() {
        return this.bounds.y();
    }

    public float width() {
        return this.bounds.width();
    }

    public float height() {
        return this.bounds.height();
    }

    public void handleClose() {
    }

    public void onMenuDrag(boolean z) {
    }

    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return false;
    }

    public void animation(WeightedEngine weightedEngine) {
    }

    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
    }

    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
    }

    public boolean hitTest(PixelPoint pixelPoint, LayoutScaleContext layoutScaleContext) {
        return this.bounds.containsPhysical(pixelPoint.x(), pixelPoint.y(), layoutScaleContext.scaleFactor());
    }

    public WidgetContainer parent() {
        return this.parent;
    }

    public Widget parent(WidgetContainer widgetContainer) {
        this.parent = widgetContainer;
        return this;
    }

    public boolean visible() {
        return this.visible;
    }

    public Widget visible(boolean z) {
        this.visible = z;
        return this;
    }
}
