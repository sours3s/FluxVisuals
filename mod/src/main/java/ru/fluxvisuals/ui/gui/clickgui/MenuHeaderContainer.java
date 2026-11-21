package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.KeyInput;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.MathUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.OverlayCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.RenderCommandQueue;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.joml.Vector4f;

public class MenuHeaderContainer extends WidgetContainer {
    final ExpandedHeaderBar expandedBar = new ExpandedHeaderBar(this::exchange);

    final CollapsedHeaderBar collapsedBar = new CollapsedHeaderBar(this::exchange);
    final ToggleAnimator transitionAnimation = ToggleAnimator.times(3, 70, Easings.EASE_IN_OUT_CUBIC);
    final Set<Integer> pressedKeys = new HashSet();

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.colorStack();
        drawCtx.fillRoundedRect(x(), y(), width(), height(), new Vector4f(8.0f, 0.0f, 8.0f, 0.0f), colorStack115.computeColor(16777215, 0.01f));
        if (this.transitionAnimation.smoothAnimation() >= 1.0f) {
            DrawEngine drawEngine154 = drawCtx.drawEngine();
            drawCtx.roundedBlur(drawCtx.getColorAttachmentId(FluxVisuals.INSTANCE.windowController().headerBlur().getBlurFramebuffer()), x(), y(), width(), height(), new Vector4f(8.0f, 0.0f, 8.0f, 0.0f), drawEngine154.colorStack().white());
            drawCtx.fillRoundedRect(x(), y(), width(), height(), new Vector4f(8.0f, 0.0f, 8.0f, 0.0f), drawEngine154.colorStack().computeColor(drawCtx.theme().palette().surfaceBackground().tone(900).argb(), 0.8f));
        }
        colorStack115.push();
        colorStack115.alpha(computeContentAlpha());
        if (this.transitionAnimation.smoothAnimation() >= 1.0f) {
            drawCtx.fillRoundedRect(x(), (y() + MenuWindow.EXPANDED_HEADER_HEIGHT) - 1.0f, width(), 296.0f, new Vector4f(0.0f, 8.0f, 0.0f, 8.0f), drawCtx.drawEngine().colorStack().computeColor(1052947, 0.95f));
        }
        currentHeaderState().render(drawCtx);
        colorStack115.pop();
        drawCtx.fillRect(x(), (y() + height()) - 1.0f, width(), 1.0f, colorStack115.computeColor(drawCtx.theme().palette().surfaceBackground().tone(700).argb()));
        super.render(drawCtx);
    }

    public void exchange() {
        this.transitionAnimation.invert();
        this.pressedKeys.clear();
        if (currentHeaderState() == this.expandedBar) {
            this.expandedBar.resetHotkeysState();
        }
    }

    @Override
    public void onMenuDrag(boolean z) {
        currentHeaderState().onMenuDrag(z);
        super.onMenuDrag(z);
    }

    public boolean expandedState() {
        return currentHeaderState() == this.expandedBar;
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        WidgetContainer widgetContainer = currentHeaderState();
        boolean zHandleInput = (widgetContainer == this.expandedBar || (inputEventContext.inputEvent() instanceof KeyInput)) ? z | this.expandedBar.handleInput(inputEventContext, z) : z | widgetContainer.handleInput(inputEventContext, z);
        if (!zHandleInput) {
            InputEvent inputEvent691 = inputEventContext.inputEvent();
            if (inputEvent691 instanceof KeyInput) {
                KeyInput keyInput = (KeyInput) inputEvent691;
                int iKeyCode = keyInput.keyCode();
                boolean zPress = keyInput.keyAction().press();
                boolean zRelease = keyInput.keyAction().release();
                if (zPress) {
                    this.pressedKeys.add(Integer.valueOf(iKeyCode));
                } else if (zRelease) {
                    this.pressedKeys.remove(Integer.valueOf(iKeyCode));
                }
                boolean zMethod001 = isBindingPressed(this.expandedBar.exchangeEditableBinding());
                if (widgetContainer == this.expandedBar && zPress && (iKeyCode == 256 || zMethod001)) {
                    exchange();
                    return true;
                }
                if (widgetContainer != this.expandedBar && zPress && zMethod001) {
                    exchange();
                    return true;
                }
            }
        }
        return zHandleInput;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(MenuWindow.MENU_WIDTH, computeHeight());
        currentHeaderState().setPosition(x(), y());
        currentHeaderState().layout(layoutScaleContext);
        super.layout(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.transitionAnimation.animate(weightedEngine);
        currentHeaderState().animation(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public void handleClose() {
        this.pressedKeys.clear();
        currentHeaderState().handleClose();
        super.handleClose();
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        currentHeaderState().collectBloomElements(renderCommandQueue);
        super.collectBloomElements(renderCommandQueue);
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
        if (currentHeaderState() == this.collapsedBar) {
            overlayCommandQueue.recordElement(currentHeaderState());
        }
        super.collectBlurElements(overlayCommandQueue);
    }

    public WidgetContainer currentHeaderState() {
        return this.transitionAnimation.smoothAnimation() <= 1.5f ? this.collapsedBar : this.expandedBar;
    }

    public float computeContentAlpha() {
        float fSmoothAnimation = this.transitionAnimation.smoothAnimation();
        if (fSmoothAnimation < 1.0f || fSmoothAnimation > 2.0f) {
            return fSmoothAnimation < 1.0f ? 1.0f - fSmoothAnimation : fSmoothAnimation - 2.0f;
        }
        return 0.0f;
    }

    public float computeHeight() {
        float fSmoothAnimation = this.transitionAnimation.smoothAnimation();
        float f = MenuWindow.COLLAPSED_HEADER_HEIGHT;
        float f2 = MenuWindow.EXPANDED_HEADER_HEIGHT;
        if (fSmoothAnimation <= 1.0f) {
            return f;
        }
        return fSmoothAnimation >= 2.0f ? f2 : MathUtil.interpolate(f, f2, fSmoothAnimation - 1.0f);
    }

    public boolean isBindingPressed(List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        return this.pressedKeys.containsAll(list);
    }
}
