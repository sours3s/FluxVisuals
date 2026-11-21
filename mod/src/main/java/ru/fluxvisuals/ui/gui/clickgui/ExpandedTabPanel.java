package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.Iterator;
import java.util.List;

public class ExpandedTabPanel extends WidgetContainer {
    float offsetX = 0.0f;
    float offsetY = 0.0f;
    final List<MenuTabElement> tabElements = FluxVisuals.INSTANCE.tabsController().tabElements();

    @Override
    public void render(DrawCtx drawCtx) {
        Iterator<MenuTabElement> it = this.tabElements.iterator();
        while (it.hasNext()) {
            it.next().drawExpanded(drawCtx);
        }
        super.render(drawCtx);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = super.handleInput(inputEventContext, z);
        if (!zHandleInput) {
            Iterator<MenuTabElement> it = this.tabElements.iterator();
            while (it.hasNext()) {
                zHandleInput |= it.next().handleInput(inputEventContext, zHandleInput);
            }
        }
        return zHandleInput;
    }

    @Override
    public void onMenuDrag(boolean z) {
        this.tabElements.forEach(menuTabElement -> {
            menuTabElement.onMenuDrag(z);
        });
        super.onMenuDrag(z);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(270.0f, 36.0f);
        setPosition(this.parent.x(), this.parent.y());
        float fMax = 0.0f;
        float fMax2 = 0.0f;
        for (MenuTabElement menuTabElement : this.tabElements) {
            menuTabElement.layout(layoutScaleContext);
            fMax = Math.max(fMax, menuTabElement.expandedWidth());
            fMax2 = Math.max(fMax2, menuTabElement.expandedHeight());
        }
        float fX = x() + 15.0f;
        float fY = y() + MenuWindow.COLLAPSED_HEADER_HEIGHT + 33.0f;
        float f = fX + fMax + 5.0f;
        int size = this.tabElements.size();
        for (int i = 0; i < size; i++) {
            this.tabElements.get(i).setPosition(i % 2 == 0 ? fX : f, fY + ((i / 2) * (fMax2 + 2.0f)));
        }
        super.layout(layoutScaleContext);
    }

    @Override
    public void handleClose() {
        this.tabElements.forEach((v0) -> {
            v0.handleClose();
        });
        super.handleClose();
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        Iterator<MenuTabElement> it = this.tabElements.iterator();
        while (it.hasNext()) {
            it.next().animation(weightedEngine);
        }
        super.animation(weightedEngine);
    }
}
