package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;

public class SeparatorElement extends ModuleFrame {
    @Override
    public void highlight() {
    }

    @Override
    public void clearHighlight() {
    }

    @Override
    public void draw(DrawCtx drawCtx, float f, float f2) {
        drawCtx.fillRect(x(), y(), f, 1.0f, drawCtx.drawEngine().colorStack().computeColor(drawCtx.theme().palette().surfaceOutline().tone(600).argb()));
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
    }

    @Override
    public float height() {
        return 1.0f;
    }
}
