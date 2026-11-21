package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

public interface TabLayout {
    default void initialize(MenuTabElement menuTabElement) {
    }

    void render(DrawCtx drawCtx);

    default void renderOverlays(DrawCtx drawCtx) {
    }

    void layout(LayoutScaleContext layoutScaleContext);

    void animation(WeightedEngine weightedEngine);

    boolean handleInput(InputEventContext inputEventContext, boolean z);

    float getContentHeight();

    void positionFrames();

    float width();

    float height();
}
