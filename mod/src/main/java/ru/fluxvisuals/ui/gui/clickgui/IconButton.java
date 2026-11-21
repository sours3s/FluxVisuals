package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DragRenderContext;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScreenClickContext;
import ru.fluxvisuals.ui.gui.clickgui.util.StencilBufferUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import net.minecraft.client.Mouse;

public class IconButton {
    public final ToggleAnimator hoverAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator pressAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public final Runnable onClick;
    public float width;
    public float height;
    public int color;
    public boolean pressed;
    public GlTexture icon;
    public float x;
    public float y;

    public IconButton(Runnable runnable, GlTexture glTexture, float f, float f2) {
        this.onClick = runnable;
        this.icon = glTexture;
        this.width = f;
        this.height = f2;
    }

    public void render(DragRenderContext dragRenderContext) {
        DrawEngine drawEngine154 = dragRenderContext.drawEngine();
        ColorStack colorStack115 = drawEngine154.colorStack();
        drawEngine154.texture(dragRenderContext.matrixStack().peek().getPositionMatrix(), this.x, this.y, this.width, this.height, drawEngine154.bindTexture(this.icon.textureWithSTB()), colorStack115.interpolate(colorStack115.computeColor(this.color), colorStack115.computeColor(StencilBufferUtil.STENCIL_MASK, 200, 200, 200), this.hoverAnimator));
    }

    public void animate(WeightedEngine weightedEngine) {
        Mouse mouse = Mc.INSTANCE.getMouse();
        this.hoverAnimator.state(contains((float) mouse.getX(), (float) mouse.getY())).animate(weightedEngine);
        this.pressAnimator.state(this.pressed).animate(weightedEngine);
    }

    public void switchTexture(GlTexture glTexture) {
        this.icon = glTexture;
    }

    public boolean click(ScreenClickContext screenClickContext, boolean z) {
        float fMouseX = screenClickContext.mouseX();
        float fMouseY = screenClickContext.mouseY();
        if (screenClickContext.button() != 0) {
            return false;
        }
        if (screenClickContext.press() && contains(fMouseX, fMouseY) && !screenClickContext.intercepted()) {
            this.pressed = true;
            return true;
        }
        if (!screenClickContext.release()) {
            return false;
        }
        this.pressed = false;
        if (!contains(fMouseX, fMouseY) || screenClickContext.intercepted()) {
            return false;
        }
        this.onClick.run();
        return true;
    }

    public boolean contains(float f, float f2) {
        return f > this.x && f2 > this.y && f < this.x + this.width && f2 < this.y + this.height;
    }

    public IconButton position(float f, float f2) {
        this.x = f;
        this.y = f2;
        return this;
    }

    public IconButton size(float f) {
        this.width = f;
        this.height = f;
        return this;
    }

    public IconButton color(int i) {
        this.color = i;
        return this;
    }

    public IconButton width(float f) {
        this.width = f;
        return this;
    }

    public IconButton height(float f) {
        this.height = f;
        return this;
    }

    public IconButton clicked(boolean z) {
        this.pressed = z;
        return this;
    }

    public IconButton icon(GlTexture glTexture) {
        this.icon = glTexture;
        return this;
    }

    public IconButton x(float f) {
        this.x = f;
        return this;
    }

    public IconButton y(float f) {
        this.y = f;
        return this;
    }

    public float width() {
        return this.width;
    }

    public float height() {
        return this.height;
    }
}
