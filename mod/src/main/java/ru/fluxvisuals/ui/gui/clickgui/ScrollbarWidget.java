package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.MathUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.type.MouseButtonAction;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScrollInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.joml.Vector4f;

public class ScrollbarWidget extends AbstractWidget {
    public final Supplier<Float> scrollOffsetSupplier;
    public final Supplier<Float> contentSizeSupplier;
    public final Supplier<Float> viewportSizeSupplier;
    public final Consumer<Float> scrollConsumer;
    public final float padding;
    public final float thickness;
    public final float minThumbSize;
    public boolean dragging;
    public float dragOffset;
    public long lastScrollTime;
    public final ToggleAnimator visibilityAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator hoverAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public final WidgetBounds trackBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds thumbBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);

    public ScrollbarWidget(Supplier<Float> supplier, Supplier<Float> supplier2, Supplier<Float> supplier3, Consumer<Float> consumer, float f, float f2, float f3) {
        this.scrollOffsetSupplier = supplier;
        this.contentSizeSupplier = supplier2;
        this.viewportSizeSupplier = supplier3;
        this.scrollConsumer = consumer;
        this.padding = f;
        this.thickness = f2;
        this.minThumbSize = f3;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        float fMethod002 = floatOrZero(this.viewportSizeSupplier.get());
        setSize(this.thickness, fMethod002);
        float fX = x();
        float fY = y() + this.padding;
        this.trackBounds.withPosition(fX, fY).withSize(this.thickness, Math.max(0.0f, fMethod002 - (this.padding * 2.0f)));
        updateThumbBounds();
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.colorStack();
        int iComputeColor = colorStack115.computeColor(themePalette.surfaceBackground().tone(300).argb());
        int iComputeColor2 = colorStack115.computeColor(themePalette.surfaceBackground().tone(200).argb());
        drawCtx.fillRoundedRect(this.thumbBounds.x(), this.thumbBounds.y(), this.thumbBounds.width(), this.thumbBounds.height(), new Vector4f(2.0f, 2.0f, 2.0f, 2.0f), colorStack115.interpolate(colorStack115.interpolate(iComputeColor, iComputeColor2, this.hoverAnimator, this.hoverAnimator), colorStack115.computeColor(themePalette.surfaceBackground().tone(200).argb()), this.visibilityAnimator, this.visibilityAnimator));
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.hoverAnimator.animate(weightedEngine);
        this.visibilityAnimator.animate(weightedEngine);
        if (!this.dragging && this.lastScrollTime > 0 && System.currentTimeMillis() - this.lastScrollTime > 1000) {
            this.visibilityAnimator.state(false);
            this.lastScrollTime = 0L;
        }
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) throws MatchException {
        if (z) {
            return false;
        }
        InputEvent inputEvent691 = inputEventContext.inputEvent();
        if (inputEvent691 instanceof CursorMoveInput) {
            try {
                ((CursorMoveInput) inputEvent691).mousePosition();
                this.hoverAnimator.state(!this.dragging && inputEventContext.inArea(this.thumbBounds.x(), this.thumbBounds.y(), this.thumbBounds.width(), this.thumbBounds.height()));
                if (!this.dragging) {
                    return false;
                }
                dragTo(inputEventContext.logicalMousePosition().y());
                return true;
            } catch (Throwable th) {
                throw new MatchException(th.toString(), th);
            }
        }
        if (inputEvent691 instanceof ScrollInput) {
            this.visibilityAnimator.state(true);
            this.lastScrollTime = System.currentTimeMillis();
            return false;
        }
        if (!(inputEvent691 instanceof MouseButtonInput)) {
            return false;
        }
        MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent691;
        if (mouseButtonInput.button() != 0) {
            return false;
        }
        if (floatOrZero(this.contentSizeSupplier.get()) <= floatOrZero(this.viewportSizeSupplier.get()) + 0.5f) {
            this.dragging = false;
            this.visibilityAnimator.state(false);
            return false;
        }
        MouseButtonAction mouseButtonAction = mouseButtonInput.action();
        boolean zInArea = inputEventContext.inArea(this.thumbBounds.x(), this.thumbBounds.y(), this.thumbBounds.width(), this.thumbBounds.height());
        boolean zInArea2 = inputEventContext.inArea(this.trackBounds.x(), this.trackBounds.y(), this.trackBounds.width(), this.trackBounds.height());
        if (mouseButtonAction.press()) {
            if (zInArea) {
                this.dragging = true;
                this.visibilityAnimator.state(true);
                this.lastScrollTime = 0L;
                this.dragOffset = inputEventContext.logicalMousePosition().y() - this.thumbBounds.y();
                return true;
            }
            if (zInArea2) {
                applyScrollPosition(inputEventContext.logicalMousePosition().y() - (this.thumbBounds.height() / 2.0f));
                return true;
            }
        }
        if (!mouseButtonAction.release() || !this.dragging) {
            return false;
        }
        this.dragging = false;
        this.visibilityAnimator.state(false);
        return true;
    }

    public void updateThumbBounds() {
        float fMethod002 = floatOrZero(this.contentSizeSupplier.get());
        float fMethod003 = floatOrZero(this.viewportSizeSupplier.get());
        float fHeight = this.trackBounds.height();
        if (fHeight <= 0.0f) {
            this.thumbBounds.withPosition(this.trackBounds.x(), this.trackBounds.y()).withSize(this.trackBounds.width(), 0.0f);
            return;
        }
        if (fMethod002 <= fMethod003 + 0.5f) {
            this.thumbBounds.withPosition(this.trackBounds.x(), this.trackBounds.y()).withSize(this.trackBounds.width(), fHeight);
            return;
        }
        float fMin = Math.min(Math.max(this.minThumbSize, (fHeight * fMethod003) / fMethod002), fHeight);
        float fClamp = MathUtil.clamp(floatOrZero(this.scrollOffsetSupplier.get()) / Math.max(0.001f, fMethod002 - fMethod003), 0.0f, 1.0f);
        this.thumbBounds.withPosition(this.trackBounds.x(), this.trackBounds.y() + ((fHeight - fMin) * fClamp)).withSize(this.trackBounds.width(), fMin);
    }

    public void dragTo(double d) {
        applyScrollPosition(((float) d) - this.dragOffset);
    }

    public void applyScrollPosition(float f) {
        float fMethod002 = floatOrZero(this.contentSizeSupplier.get());
        float fMethod003 = floatOrZero(this.viewportSizeSupplier.get());
        float fHeight = this.trackBounds.height();
        if (fMethod002 <= fMethod003 + 0.5f || fHeight <= 0.0f) {
            return;
        }
        float fMax = Math.max(0.001f, fHeight - this.thumbBounds.height());
        this.scrollConsumer.accept(Float.valueOf(((MathUtil.clamp(f, this.trackBounds.y(), this.trackBounds.y() + fMax) - this.trackBounds.y()) / fMax) * Math.max(0.0f, fMethod002 - fMethod003)));
        updateThumbBounds();
    }

    public float floatOrZero(Float f) {
        if (f == null) {
            return 0.0f;
        }
        return f.floatValue();
    }
}
