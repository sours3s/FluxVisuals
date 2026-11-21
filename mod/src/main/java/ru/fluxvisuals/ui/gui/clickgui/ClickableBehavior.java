package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.type.MouseButtonAction;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScissorBounds;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

public class ClickableBehavior {
    public static final int hoverDuration = 200;
    public final ToggleAnimator hoverAnimation = new ToggleAnimator(hoverDuration, Easings.EASE_IN_OUT_CUBIC);
    public Runnable clickAction;
    public Runnable rightClickAction;
    public float ownerX;
    public float ownerY;
    public float ownerW;
    public float ownerH;

    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        float f = this.ownerX;
        float f2 = this.ownerY;
        float f3 = this.ownerW;
        float f4 = this.ownerH;
        if (inputEventContext.inputEvent() instanceof CursorMoveInput) {
            boolean z2 = inputEventContext.inArea(f, f2, f3, f4) && !z;
            this.hoverAnimation.state(z2);
            return z2;
        }
        InputEvent inputEvent691 = inputEventContext.inputEvent();
        if (!(inputEvent691 instanceof MouseButtonInput)) {
            return false;
        }
        MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent691;
        MouseButtonAction mouseButtonAction = mouseButtonInput.action();
        boolean zInArea = inputEventContext.inArea(f, f2, f3, f4);
        if (mouseButtonInput.button() == 0 && this.clickAction != null && mouseButtonAction.press() && zInArea && !z) {
            this.clickAction.run();
            return true;
        }
        if (mouseButtonInput.button() != 1 || this.rightClickAction == null || !mouseButtonAction.press() || !zInArea || z) {
            return false;
        }
        this.rightClickAction.run();
        return true;
    }

    public void determineFromLastDrawEngineOperation(DrawCtx drawCtx) {
        DrawEngine drawEngine154 = drawCtx.drawEngine();
        ScissorBounds scissorBounds = drawEngine154.lastBounds();
        float fScaleFactor = drawCtx.layoutContext().scaleFactor();
        float fTransformX = drawEngine154.transformX(drawCtx.matrixStack(), scissorBounds.x);
        float fTransformY = drawEngine154.transformY(drawCtx.matrixStack(), scissorBounds.y);
        if (fScaleFactor <= 0.0f || fScaleFactor == 1.0f) {
            this.ownerX = fTransformX;
            this.ownerY = fTransformY;
            this.ownerW = scissorBounds.w;
            this.ownerH = scissorBounds.h;
            return;
        }
        this.ownerX = fTransformX / fScaleFactor;
        this.ownerY = fTransformY / fScaleFactor;
        this.ownerW = scissorBounds.w / fScaleFactor;
        this.ownerH = scissorBounds.h / fScaleFactor;
    }

    public void setDimensions(float f, float f2, float f3, float f4) {
        this.ownerX = f;
        this.ownerY = f2;
        this.ownerW = f3;
        this.ownerH = f4;
    }

    public void animate(WeightedEngine weightedEngine) {
        this.hoverAnimation.animate(weightedEngine);
    }

    public ToggleAnimator hoverAnimation() {
        return this.hoverAnimation;
    }

    public ClickableBehavior clickCallback(Runnable runnable) {
        this.clickAction = runnable;
        return this;
    }

    public ClickableBehavior rightClickCallback(Runnable runnable) {
        this.rightClickAction = runnable;
        return this;
    }

    public float ownerX() {
        return this.ownerX;
    }

    public float ownerY() {
        return this.ownerY;
    }

    public float ownerW() {
        return this.ownerW;
    }

    public float ownerH() {
        return this.ownerH;
    }
}
