package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.RenderCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.Iterator;
import java.util.List;

public class CollapsedTabBar extends WidgetContainer {
    final AnimatedFloat indicatorPosition = new AnimatedFloat(100, Easings.EASE_IN_OUT_CUBIC);
    final GlTexture triangleIcon = new GlTexture(new ClasspathResource("/icons/menu/new/triangle.png"));
    final List<MenuTabElement> tabElements = FluxVisuals.INSTANCE.tabsController().tabElements();

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.colorStack();
        drawCtx.fillOutlinedRoundedRect(x(), y(), width(), height(), 20.0f, 2.5f, colorStack115.computeColor(drawCtx.theme().palette().surfaceOutline().tone(600).argb()), colorStack115.computeColor(drawCtx.theme().palette().surfaceOutline().tone(600).argb(), 0));
        Iterator<MenuTabElement> it = this.tabElements.iterator();
        while (it.hasNext()) {
            it.next().drawCollapsed(drawCtx);
        }
        drawCtx.fillRect(x() + 2.0f + this.indicatorPosition.animatedValue(), (y() + height()) - 1.0f, 10.0f, 1.0f, colorStack115.computeColor(drawCtx.theme().palette().accent().argb()));
        super.render(drawCtx);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        Iterator<MenuTabElement> it = this.tabElements.iterator();
        while (it.hasNext()) {
            if (it.next().handleInput(inputEventContext, z)) {
                return true;
            }
        }
        return super.handleInput(inputEventContext, z);
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
        setPosition((this.parent.x() + (this.parent.width() / 2.0f)) - (width() / 2.0f), (this.parent.y() + (parent().height() / 2.0f)) - (height() / 2.0f));
        float fX = x() + 20.0f;
        for (MenuTabElement menuTabElement : this.tabElements) {
            menuTabElement.layout(layoutScaleContext);
            menuTabElement.setPosition(fX, (y() + (height() / 2.0f)) - (menuTabElement.collapsedHeight() / 2.0f));
            fX += menuTabElement.collapsedWidth() + 22.0f;
        }
        this.indicatorPosition.destination(FluxVisuals.INSTANCE.tabsController().current().x() - x());
        super.layout(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        Iterator<MenuTabElement> it = this.tabElements.iterator();
        while (it.hasNext()) {
            it.next().animation(weightedEngine);
        }
        this.indicatorPosition.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public void handleClose() {
        this.tabElements.forEach((v0) -> {
            v0.handleClose();
        });
        super.handleClose();
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        this.tabElements.forEach(menuTabElement -> {
            menuTabElement.collectBloomElements(renderCommandQueue);
        });
        super.collectBloomElements(renderCommandQueue);
    }
}
