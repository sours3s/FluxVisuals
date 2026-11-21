package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ScreenResolution;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Theme;

import net.minecraft.client.Mouse;
import net.minecraft.client.util.math.MatrixStack;

public final class DragRenderContext {
    public final ScreenResolution resolution;
    public final Mouse mousePosition;
    public final MatrixStack matrixStack;

    public final DrawEngine drawEngine;

    public final Theme theme;
    public final float aF;

    public DragRenderContext(ScreenResolution screenResolution, Mouse mouse, MatrixStack matrixStack, DrawEngine drawEngine154, Theme theme760, float f) {
        this.resolution = screenResolution;
        this.mousePosition = mouse;
        this.matrixStack = matrixStack;
        this.drawEngine = drawEngine154;
        this.theme = theme760;
        this.aF = f;
    }

    public float mouseX() {
        return (float) (this.mousePosition.getX() / ((double) this.aF));
    }

    public float mouseY() {
        return (float) (this.mousePosition.getY() / ((double) this.aF));
    }

    public void sizeAnimation(MatrixStack matrixStack, float f, float f2, float f3) {
        matrixStack.translate(f / 2.0f, f2 / 2.0f, 0.0f);
        matrixStack.scale(f3, f3, 0.0f);
        matrixStack.translate((-f) / 2.0f, (-f2) / 2.0f, 0.0f);
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "resolution=" + this.resolution + ", " + "mousePosition=" + this.mousePosition + ", " + "matrixStack=" + this.matrixStack + ", " + "drawEngine=" + this.drawEngine + ", " + "theme=" + this.theme + ", " + "aF=" + this.aF + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.resolution, this.mousePosition, this.matrixStack, this.drawEngine, this.theme, this.aF);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DragRenderContext)) return false;
        DragRenderContext o = (DragRenderContext) obj;
        return java.util.Objects.equals(this.resolution, o.resolution) && java.util.Objects.equals(this.mousePosition, o.mousePosition) && java.util.Objects.equals(this.matrixStack, o.matrixStack) && java.util.Objects.equals(this.drawEngine, o.drawEngine) && java.util.Objects.equals(this.theme, o.theme) && java.util.Objects.equals(this.aF, o.aF);
    }
public ScreenResolution resolution() {
        return this.resolution;
    }

    public Mouse mousePosition() {
        return this.mousePosition;
    }

    public MatrixStack matrixStack() {
        return this.matrixStack;
    }

    public DrawEngine drawEngine() {
        return this.drawEngine;
    }

    public Theme theme() {
        return this.theme;
    }

    public float scaleFactor() {
        return this.aF;
    }
}
