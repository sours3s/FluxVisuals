package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.MathUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ScreenResolution;

public class DraggableContainer extends WidgetContainer {
    public boolean dragging;
    public float dragOffsetX;
    public float dragOffsetY;
    public ScreenResolution screenResolution;
    public float logicalWidth;
    public float logicalHeight;

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        InputEvent inputEvent691 = inputEventContext.inputEvent();
        if (z && !this.dragging) {
            return false;
        }
        if (inputEvent691 instanceof CursorMoveInput) {
            boolean zInArea = inputEventContext.inArea(x(), y(), width(), height());
            if (!this.dragging) {
                return zInArea;
            }
            float fX = inputEventContext.logicalMousePosition().x() - this.dragOffsetX;
            float fY = inputEventContext.logicalMousePosition().y() - this.dragOffsetY;
            if (shouldClampPosition(this.screenResolution.width(), this.screenResolution.height())) {
                float fWidth = this.logicalWidth - this.screenResolution.width();
                float fHeight = this.logicalHeight - this.screenResolution.height();
                fX = MathUtil.clamp(fX, 0.0f, fWidth);
                fY = MathUtil.clamp(fY, 0.0f, fHeight);
            }
            this.screenResolution.setPosition(fX, fY);
            return true;
        }
        if (inputEvent691 instanceof MouseButtonInput) {
            MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent691;
            if (mouseButtonInput.button() == 0) {
                if (mouseButtonInput.action().press()) {
                    boolean zInArea2 = inputEventContext.inArea(x(), y(), width(), height());
                    if (!z && zInArea2) {
                        this.dragging = true;
                        this.dragOffsetX = inputEventContext.logicalMousePosition().x() - this.screenResolution.x();
                        this.dragOffsetY = inputEventContext.logicalMousePosition().y() - this.screenResolution.y();
                        return true;
                    }
                }
                if (mouseButtonInput.action().release() && this.dragging) {
                    this.dragging = false;
                    return true;
                }
            }
        }
        return super.handleInput(inputEventContext, z);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.screenResolution = layoutScaleContext.screenResolution();
        this.logicalWidth = layoutScaleContext.logicalWidth();
        this.logicalHeight = layoutScaleContext.logicalHeight();
        if (shouldClampPosition(this.screenResolution.width(), this.screenResolution.height())) {
            this.screenResolution.setPosition(MathUtil.clamp(this.screenResolution.x(), 0.0f, this.logicalWidth - this.screenResolution.width()), MathUtil.clamp(this.screenResolution.y(), 0.0f, this.logicalHeight - this.screenResolution.height()));
        }
        super.layout(layoutScaleContext);
    }

    public boolean shouldClampPosition(float f, float f2) {
        return this.screenResolution != null && this.logicalWidth > f && this.logicalHeight > f2;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public void setDragging(boolean z) {
        this.dragging = z;
    }
}
