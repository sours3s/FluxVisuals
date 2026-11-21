package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.ArrayList;
import java.util.List;

public class FrameElementColumn extends WidgetContainer {
    public final List<ModuleFrame> frames = new ArrayList();
    public float width;
    public final float gap;

    public FrameElementColumn(float f, float f2) {
        this.width = f;
        this.gap = f2;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        float fY = y();
        for (ModuleFrame moduleFrame : this.frames) {
            if (moduleFrame.visible()) {
                moduleFrame.setPosition(x(), fY);
                fY += moduleFrame.height() + this.gap;
            }
        }
        super.layout(layoutScaleContext);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        AbstractFrame class757VarMethod001 = findParentFrame();
        for (int size = this.frames.size() - 1; size >= 0; size--) {
            ModuleFrame moduleFrame = this.frames.get(size);
            if (moduleFrame.visible()) {
                float dimmingForElement = 0.0f;
                if (class757VarMethod001 != null && class757VarMethod001.elementDimmingManager().isDimming()) {
                    dimmingForElement = class757VarMethod001.elementDimmingManager().getDimmingForElement(moduleFrame);
                }
                if (dimmingForElement > 0.01f) {
                    colorStack115.push();
                    colorStack115.alpha(1.0f - (dimmingForElement * 0.5f));
                }
                moduleFrame.draw(drawCtx, width(), height());
                if (dimmingForElement > 0.01f) {
                    colorStack115.pop();
                }
            }
        }
    }

    public void renderOverlays(DrawCtx drawCtx) {
        for (int size = this.frames.size() - 1; size >= 0; size--) {
            ModuleFrame moduleFrame = this.frames.get(size);
            if (moduleFrame.visible()) {
                moduleFrame.drawOverlay(drawCtx);
            }
        }
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (!visible()) {
            return z;
        }
        boolean zHandleInput = z;
        boolean z2 = inputEventContext.inputEvent() instanceof CursorMoveInput;
        for (ModuleFrame moduleFrame : this.frames) {
            if (moduleFrame.visible()) {
                if (!z2 && zHandleInput) {
                    break;
                }
                zHandleInput |= moduleFrame.handleInput(inputEventContext, zHandleInput);
            }
        }
        return zHandleInput;
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        super.animation(weightedEngine);
    }

    public void handleViewportVisibility(float f, float f2, float f3) {
        for (ModuleFrame moduleFrame : this.frames) {
            if (moduleFrame.visible()) {
                moduleFrame.handleViewportVisibility(f, f2, f3);
            }
        }
    }

    @Override
    public final float width() {
        return this.width;
    }

    @Override
    public float height() {
        return computeHeight();
    }

    public void addFrameElement(ModuleFrame moduleFrame) {
        if (moduleFrame == null) {
            return;
        }
        this.frames.add(moduleFrame);
        addChild(moduleFrame);
    }

    public float computeHeight() {
        float fHeight = 0.0f;
        int i = 0;
        for (ModuleFrame moduleFrame : this.frames) {
            if (moduleFrame.visible()) {
                fHeight += moduleFrame.height();
                i++;
            }
        }
        if (i > 1) {
            fHeight += this.gap * (i - 1);
        }
        return fHeight;
    }

    public AbstractFrame findParentFrame() {
        WidgetContainer widgetContainer683 = parent();
        while (true) {
            WidgetContainer widgetContainer = widgetContainer683;
            if (widgetContainer == null) {
                return null;
            }
            if (widgetContainer instanceof AbstractFrame) {
                return (AbstractFrame) widgetContainer;
            }
            widgetContainer683 = widgetContainer.parent();
        }
    }

    public void clearElements() {
        this.frames.clear();
    }

    @Override
    public void removeChild(Widget widget) {
        super.removeChild(widget);
        if (widget instanceof ModuleFrame) {
            this.frames.remove((ModuleFrame) widget);
        }
    }

    public void setWidth(float f) {
        this.width = f;
    }
}
