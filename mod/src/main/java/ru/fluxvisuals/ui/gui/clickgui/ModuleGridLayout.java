package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ModuleGridLayout extends AbstractTabLayout {
    public static final float as = 50.0f;
    public static final float at = 16.0f;
    public static final float au = 16.0f;
    public static final float av = 15.0f;
    public static final float aw = 14.0f;
    ScrollbarWidget scrollbar;

    @Override
    public void initialize(MenuTabElement menuTabElement) {
        super.initialize(menuTabElement);
        ScrollArea scrollArea = menuTabElement.scrollingAreaComponent();
        Objects.requireNonNull(scrollArea);
        Supplier supplier = scrollArea::scrollY;
        Supplier supplier2 = this::getContentHeight;
        Supplier supplier3 = () -> {
            return Float.valueOf(MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT);
        };
        ScrollArea class789VarScrollingAreaComponent2 = menuTabElement.scrollingAreaComponent();
        Objects.requireNonNull(class789VarScrollingAreaComponent2);
        this.scrollbar = new ScrollbarWidget(supplier, supplier2, supplier3, (v1) -> {
            scrollArea.scrollTo(v1);
        }, 16.0f, 3.0f, 24.0f);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        colorStack115.push();
        colorStack115.alphaAnimation(this.tab.currentTabAnimation());
        this.scrollbar.render(drawCtx);
        this.tab.scrollingAreaComponent().beginArea(drawCtx, originX(), originY(), width(), height());
        renderFrames(drawCtx);
        this.tab.scrollingAreaComponent().endArea(drawCtx, getContentHeight());
        renderOverlays(drawCtx);
        colorStack115.pop();
    }

    public void renderFrames(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame abstractFrame = this.tab.frames().get(size);
            if (abstractFrame.visible() && isFrameVisible(abstractFrame, as, originY())) {
                float dimmingForFrame = this.tab.dimmingManager().getDimmingForFrame(abstractFrame);
                if (dimmingForFrame > 0.01f) {
                    colorStack115.push();
                    colorStack115.alpha(1.0f - (dimmingForFrame * 0.5f));
                }
                abstractFrame.render(drawCtx);
                if (dimmingForFrame > 0.01f) {
                    colorStack115.pop();
                }
            }
        }
    }

    @Override
    public void renderOverlays(DrawCtx drawCtx) {
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        this.tab.scrollingAreaComponent().scrollY();
        colorStack115.push();
        drawCtx.matrixStack().push();
        drawCtx.matrixStack().translate(0.0f, -drawCtx.layoutContext().toPhysical(this.tab.scrollingAreaComponent().scrollY()), 0.0f);
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame abstractFrame = this.tab.frames().get(size);
            if (abstractFrame.visible() && isFrameVisible(abstractFrame, as, originY())) {
                float dimmingForFrame = this.tab.dimmingManager().getDimmingForFrame(abstractFrame);
                if (dimmingForFrame > 0.01f) {
                    colorStack115.push();
                    colorStack115.alpha(1.0f - (dimmingForFrame * 0.5f));
                }
                abstractFrame.renderOverlays(drawCtx);
                if (dimmingForFrame > 0.01f) {
                    colorStack115.pop();
                }
            }
        }
        drawCtx.matrixStack().pop();
        colorStack115.pop();
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.tab.frames().forEach(abstractFrame -> {
            abstractFrame.layout(layoutScaleContext);
        });
        layoutScrollbar(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.scrollbar.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = z | this.scrollbar.handleInput(inputEventContext, z);
        boolean z2 = ((((float) inputEventContext.logicalMousePosition().y()) > this.tab.framesOriginY() ? 1 : (((float) inputEventContext.logicalMousePosition().y()) == this.tab.framesOriginY() ? 0 : -1)) < 0) && ((inputEventContext.inputEvent() instanceof MouseButtonInput) || (inputEventContext.inputEvent() instanceof CursorMoveInput));
        InputEventContext inputEventContext688 = inputEventContext.withMouseOffset(0.0f, this.tab.scrollingAreaComponent().scrollY());
        for (AbstractFrame abstractFrame : this.tab.frames()) {
            if (abstractFrame.visible() && isFrameVisible(abstractFrame, as, originY())) {
                if (abstractFrame.handleInput(inputEventContext688, z2 || zHandleInput) && !z2) {
                    zHandleInput = true;
                }
            }
        }
        return zHandleInput | this.tab.scrollingAreaComponent().handleInput(inputEventContext, zHandleInput);
    }

    @Override
    public float getContentHeight() {
        float f = 0.0f;
        for (AbstractFrame abstractFrame : this.tab.frames()) {
            if (abstractFrame.visible()) {
                float fY = (abstractFrame.y() - this.tab.framesOriginY()) + abstractFrame.height() + abstractFrame.contentHeight();
                if (fY > f) {
                    f = fY;
                }
            }
        }
        return f + 10.0f;
    }

    @Override
    public void positionFrames() {
        List<AbstractFrame> listFrames = this.tab.frames();
        listFrames.sort(Comparator.comparing(abstractFrame -> {
            if (abstractFrame instanceof ModuleCard) {
                return Boolean.valueOf(!((ModuleCard) abstractFrame).favorite());
            }
            return false;
        }).thenComparing(class757Var2 -> {
            return class757Var2 instanceof ModuleCard ? ((ModuleCard) class757Var2).name() : "";
        }, String.CASE_INSENSITIVE_ORDER));
        float fFloatValue = ((Float) listFrames.stream().filter((v0) -> {
            return v0.visible();
        }).map((v0) -> {
            return v0.width();
        }).max((v0, v1) -> {
            return Float.compare(v0, v1);
        }).orElse(Float.valueOf(0.0f))).floatValue();
        int iMax = fFloatValue > 0.0f ? Math.max(1, (int) Math.floor((((width() - 32.0f) - this.scrollbar.width()) + av) / (fFloatValue + av))) : 1;
        float[] fArr = new float[iMax];
        float[] fArr2 = new float[iMax];
        for (int i = 0; i < iMax; i++) {
            fArr2[i] = i * (fFloatValue + av);
        }
        for (AbstractFrame class757Var3 : listFrames) {
            if (class757Var3.visible()) {
                int iIndexOfMin = indexOfMin(fArr);
                float f = fArr2[iIndexOfMin];
                float f2 = fArr[iIndexOfMin];
                if (class757Var3 instanceof ModuleCard) {
                    ((ModuleCard) class757Var3).targetPosition(originX() + 16.0f + f, originY() + 16.0f + f2, !this.tab.forceImmediateFramePositioning());
                } else {
                    class757Var3.setPosition(originX() + 16.0f + f, originY() + 16.0f + f2);
                }
                fArr[iIndexOfMin] = fArr[iIndexOfMin] + class757Var3.height() + class757Var3.contentHeight() + aw;
            }
        }
    }

    @Override
    public float width() {
        return MenuWindow.MENU_WIDTH;
    }

    public void layoutScrollbar(LayoutScaleContext layoutScaleContext) {
        float f = MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT;
        this.scrollbar.setPosition(((originX() + width()) - 16.0f) - this.scrollbar.width(), originY());
        this.scrollbar.setSize(this.scrollbar.width(), f);
        this.scrollbar.layout(layoutScaleContext);
    }
}
