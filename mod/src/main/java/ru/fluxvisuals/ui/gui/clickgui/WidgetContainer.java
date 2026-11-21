package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.events.api.LayoutCallback;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.OverlayCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.RenderCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class WidgetContainer extends AbstractWidget {
    public final List<Widget> childWidgets = new ArrayList();

    public List<Widget> children() {
        return Collections.unmodifiableList(this.childWidgets);
    }

    public void addChild(Widget widget) {
        if (widget == null) {
            return;
        }
        if (widget.parent() != null) {
            widget.parent().removeChild(widget);
        }
        widget.parent(this);
        this.childWidgets.add(widget);
    }

    @Override
    public void onMenuDrag(boolean z) {
        Iterator<Widget> it = this.childWidgets.iterator();
        while (it.hasNext()) {
            it.next().onMenuDrag(z);
        }
    }

    public void addChild(int i, Widget widget) {
        if (widget == null) {
            return;
        }
        if (widget.parent() != null) {
            widget.parent().removeChild(widget);
        }
        widget.parent(this);
        this.childWidgets.add(i, widget);
    }

    public void removeChild(Widget widget) {
        if (this.childWidgets.remove(widget)) {
            widget.parent(null);
        }
    }

    public void clearChildren() {
        Iterator<Widget> it = this.childWidgets.iterator();
        while (it.hasNext()) {
            it.next().parent(null);
        }
        this.childWidgets.clear();
    }

    public void center(float f, float f2) {
        setPosition((f - width()) / 2.0f, (f2 - height()) / 2.0f);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        Iterator<Widget> it = this.childWidgets.iterator();
        while (it.hasNext()) {
            it.next().animation(weightedEngine);
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        Iterator<Widget> it = this.childWidgets.iterator();
        while (it.hasNext()) {
            it.next().layout(layoutScaleContext);
        }
    }

    @Override
    public void render(DrawCtx drawCtx) {
        for (LayoutCallback layoutCallback : this.childWidgets) {
            if (layoutCallback instanceof WidgetParent) {
                ((WidgetParent) layoutCallback).render(drawCtx);
            }
        }
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (!visible()) {
            return z;
        }
        boolean zHandleInput = z;
        for (int size = this.childWidgets.size() - 1; size >= 0; size--) {
            Widget widget = this.childWidgets.get(size);
            if (widget.visible()) {
                zHandleInput |= widget.handleInput(inputEventContext, zHandleInput);
            }
        }
        return zHandleInput | super.handleInput(inputEventContext, zHandleInput);
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
        Iterator<Widget> it = this.childWidgets.iterator();
        while (it.hasNext()) {
            it.next().collectBlurElements(overlayCommandQueue);
        }
    }

    @Override
    public void handleClose() {
        Iterator<Widget> it = this.childWidgets.iterator();
        while (it.hasNext()) {
            it.next().handleClose();
        }
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        Iterator<Widget> it = this.childWidgets.iterator();
        while (it.hasNext()) {
            it.next().collectBloomElements(renderCommandQueue);
        }
    }
}
