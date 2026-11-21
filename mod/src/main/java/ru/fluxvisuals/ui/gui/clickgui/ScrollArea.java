package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.MathUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScrollInput;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class ScrollArea extends Widget {
    public static final float scrollSpeed = 80.0f;
    public float pendingScroll;
    public float scrollOffset;
    public float areaWidth;
    public float areaHeight;
    public float contentHeight;
    public final AnimatedFloat scrollAnimation = new AnimatedFloat(120, Easings.EASE_OUT_CUBIC);
    public final WidgetBounds physicalBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) throws MatchException {
        if (z) {
            return false;
        }
        InputEvent inputEvent691 = inputEventContext.inputEvent();
        if (!(inputEvent691 instanceof ScrollInput)) {
            return false;
        }
        try {
            double dDeltaY = ((ScrollInput) inputEvent691).deltaY();
            if (!inputEventContext.inPhysicalArea(inputEventContext.currentMousePosition(), this.physicalBounds.x(), this.physicalBounds.y(), this.physicalBounds.width(), this.physicalBounds.height())) {
                return false;
            }
            float f = (float) ((-dDeltaY) * 80.0d * ((double) (Mc.INSTANCE.isControlDown() ? 3.0f : 1.0f)));
            if (this.pendingScroll != 0.0f && Math.signum(f) != Math.signum(this.pendingScroll)) {
                this.pendingScroll = 0.0f;
            }
            this.pendingScroll += f;
            return true;
        } catch (Throwable th) {
            throw new MatchException(th.toString(), th);
        }
    }

    public void beginArea(DrawCtx drawCtx, float f, float f2, float f3, float f4) {
        this.scrollOffset += this.pendingScroll;
        this.pendingScroll = 0.0f;
        LayoutScaleContext layoutScaleContext = drawCtx.layoutContext();
        this.areaWidth = f3;
        this.areaHeight = f4;
        DrawEngine drawEngine154 = drawCtx.drawEngine();
        MatrixStack matrixStack = drawCtx.matrixStack();
        clampScroll(f4);
        float physical = layoutScaleContext.toPhysical(f);
        float physical2 = layoutScaleContext.toPhysical(f2);
        float physical3 = layoutScaleContext.toPhysical(f3);
        float physical4 = layoutScaleContext.toPhysical(f4);
        listen(drawCtx, physical, physical2, physical3, physical4);
        drawEngine154.beginScissor(matrixStack.peek().getPositionMatrix(), physical, physical2, physical3, physical4);
        this.scrollAnimation.destination(this.scrollOffset);
        matrixStack.push();
        matrixStack.translate(0.0f, -layoutScaleContext.toPhysical(scrollY()), 0.0f);
    }

    public void scrollToBottomSmooth() {
        float fMax = Math.max(0.0f, this.contentHeight - this.areaHeight);
        if (Math.abs(this.scrollOffset - fMax) < 1.0f) {
            return;
        }
        this.scrollOffset = fMax;
        clampScroll(this.areaHeight);
    }

    public void scrollToBottomImmediate() {
        this.scrollOffset = Math.max(0.0f, this.contentHeight - this.areaHeight);
        clampScroll(this.areaHeight);
        this.scrollAnimation.set(this.scrollOffset);
        this.scrollAnimation.destination(this.scrollOffset);
        this.pendingScroll = 0.0f;
    }

    @Deprecated
    public void scrollToBottom() {
        scrollToBottomSmooth();
    }

    public void endArea(DrawCtx drawCtx, float f) {
        DrawEngine drawEngine154 = drawCtx.drawEngine();
        MatrixStack matrixStack = drawCtx.matrixStack();
        this.contentHeight = f;
        drawEngine154.endScissor();
        matrixStack.pop();
    }

    public void listen(DrawCtx drawCtx, float f, float f2, float f3, float f4) {
        Matrix4f positionMatrix = drawCtx.matrixStack().peek().getPositionMatrix();
        DrawEngine drawEngine154 = drawCtx.drawEngine();
        Vector4f vector4f = new Vector4f(f, f2, 0.0f, 1.0f);
        Vector4f vector4f2 = new Vector4f(f + f3, f2 + f4, 0.0f, 1.0f);
        vector4f.mul(positionMatrix);
        vector4f2.mul(positionMatrix);
        this.physicalBounds.withPosition(drawEngine154.transformX(positionMatrix, f), drawEngine154.transformY(positionMatrix, f2)).withSize(vector4f2.x - vector4f.x, vector4f2.y - vector4f.y);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.areaWidth, this.areaHeight);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.scrollAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public void scrollBy(float f) {
        this.scrollOffset += f;
        clampScroll(this.areaHeight);
    }

    public void scrollTo(float f) {
        this.scrollOffset = f;
        clampScroll(this.areaHeight);
        this.scrollAnimation.set(this.scrollOffset);
        this.scrollAnimation.destination(this.scrollOffset);
        this.pendingScroll = 0.0f;
    }

    public float scrollY() {
        return this.scrollAnimation.animatedValue();
    }

    public float visibleTop() {
        return scrollY();
    }

    public float visibleBottom() {
        return scrollY() + this.areaHeight;
    }

    public void clampScroll(float f) {
        if (this.contentHeight > f) {
            this.scrollOffset = MathUtil.clamp(this.scrollOffset, 0.0f, this.contentHeight - f);
        } else {
            this.scrollOffset = 0.0f;
        }
    }
}
