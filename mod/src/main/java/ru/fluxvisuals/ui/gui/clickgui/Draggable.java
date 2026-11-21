package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DragRenderContext;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.MathUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScreenClickContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ScreenResolution;
import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;
import ru.fluxvisuals.ui.gui.clickgui.util.SnapManager;
import ru.fluxvisuals.ui.gui.clickgui.util.model.SnapResult;
import ru.fluxvisuals.ui.gui.clickgui.util.StencilBufferUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Stopwatch;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public abstract class Draggable {
    public final String name;
    public float x;
    public float y;
    public float aC;
    public float L;
    public BooleanSupplier visibility;
    public boolean dragging;
    public boolean clicked;
    public boolean cursorHovered;
    public SnapManager snapGrid;
    public List<Draggable> siblingWidgets;

    public ScreenResolution resolution;
    public final List<Setting> settings = new ArrayList();

    public ToggleAnimator clickAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public ToggleAnimator hoveredAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public ToggleAnimator tooltipAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public boolean excludeFromSnapGrid = false;

    public final Stopwatch lastResizeTimer = new Stopwatch(false);

    public Draggable(String str, BooleanSupplier booleanSupplier) {
        this.name = str;
        this.visibility = booleanSupplier;
    }

    public void addSettings(Setting... settings661) {
        this.settings.addAll(Arrays.asList(settings661));
    }

    public void draw(DragRenderContext dragRenderContext) {
        if (this.resolution == null || this.resolution.screenWidth() <= 0 || this.resolution.screenHeight() <= 0) {
            this.resolution = ScreenResolution.resolution();
        } else if (isVisible()) {
            render(dragRenderContext);
        }
    }

    public void handleResize(int i, int i2) {
        this.resolution = new ScreenResolution(i, i2);
        this.lastResizeTimer.reset();
    }

    public void applySavedPosition(float f, float f2) {
        this.x = f;
        this.y = f2;
    }

    public void updateDrag(DragRenderContext dragRenderContext) {
        ScreenResolution screenResolution;
        Mouse mouse = Mc.INSTANCE.getMouse();
        Screen currentScreen = Mc.INSTANCE.getCurrentScreen();
        MenuWindow menuWindow776 = FluxVisuals.INSTANCE.menuWindow();
        if (menuWindow776 != null && menuWindow776.visible()) {
            WidgetSettingsPopup widgetSettingsPopup = (WidgetSettingsPopup) FluxVisuals.INSTANCE.windowController().getWindow(WidgetSettingsPopup.class);
            if (widgetSettingsPopup != null) {
                widgetSettingsPopup.closeWindow();
                return;
            }
            return;
        }
        if (!(currentScreen instanceof ChatScreen)) {
            this.dragging = false;
            this.clicked = false;
            if (this.snapGrid != null) {
                this.snapGrid.clear();
            }
            WidgetSettingsPopup class810Var2 = (WidgetSettingsPopup) FluxVisuals.INSTANCE.windowController().getWindow(WidgetSettingsPopup.class);
            if (class810Var2 != null) {
                class810Var2.close();
                return;
            }
            return;
        }
        if (!this.dragging || (screenResolution = dragRenderContext.resolution()) == null || screenResolution.screenWidth() <= 0 || screenResolution.screenHeight() <= 0) {
            return;
        }
        float fScaleFactor = dragRenderContext.scaleFactor();
        float x = (float) (mouse.getX() / ((double) fScaleFactor));
        float y = (float) (mouse.getY() / ((double) fScaleFactor));
        float f = x - this.aC;
        float f2 = y - this.L;
        if (this.snapGrid == null || this.siblingWidgets == null || isExcludeFromSnapGrid()) {
            this.x = f;
            this.y = f2;
        } else {
            SnapResult snapResult = this.snapGrid.calculateSnap(this, this.siblingWidgets, f, f2, screenResolution.screenWidth(), screenResolution.screenHeight());
            this.x = snapResult.x;
            this.y = snapResult.y;
        }
        boolean z = this.x < 0.0f || this.y < 0.0f || this.x + width() > ((float) screenResolution.screenWidth()) || this.y + height() > ((float) screenResolution.screenHeight());
        if (screenResolution.isExceeding(width(), height()) && z) {
            this.x = MathUtil.clamp(this.x, 0.0f, screenResolution.screenWidth() - width());
            this.y = MathUtil.clamp(this.y, 0.0f, screenResolution.screenHeight() - height());
        }
    }

    public void animation(WeightedEngine weightedEngine) {
        this.hoveredAnimation.state(this.cursorHovered).animate(weightedEngine);
        this.tooltipAnimation.state(this.cursorHovered && (Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen)).animate(weightedEngine);
        this.clickAnimation.state(this.clicked).animate(weightedEngine);
        animate(weightedEngine);
    }

    public void updateWidget() {
        if (isVisible()) {
            update();
        }
    }

    public boolean onCursor(MouseMoveInput mouseMoveInput) {
        boolean zIsWithinBounds = mouseMoveInput.isWithinBounds(this.x, this.y, width(), height());
        boolean zCursor = cursor(mouseMoveInput, zIsWithinBounds);
        if (!isVisible()) {
            this.cursorHovered = false;
            return false;
        }
        if (mouseMoveInput.intercepted()) {
            this.cursorHovered = false;
        } else {
            this.cursorHovered = zIsWithinBounds;
            if (zIsWithinBounds) {
                return true;
            }
        }
        return zCursor;
    }

    public boolean onClick(ScreenClickContext screenClickContext) {
        if (!isVisible()) {
            return false;
        }
        boolean zIsWithinBounds = screenClickContext.isWithinBounds(this.x, this.y, width(), height());
        boolean zClick = click(screenClickContext, zIsWithinBounds);
        if (screenClickContext.isLeftButtonPressed()) {
            if (!screenClickContext.intercepted() && screenClickContext.press() && zIsWithinBounds) {
                if (zClick) {
                    return true;
                }
                this.clicked = true;
                float fMouseX = screenClickContext.mouseX();
                float fMouseY = screenClickContext.mouseY();
                this.aC = fMouseX - this.x;
                this.L = fMouseY - this.y;
                this.dragging = true;
                return true;
            }
            if (screenClickContext.release()) {
                this.clicked = false;
                this.dragging = false;
                if (this.snapGrid == null) {
                    return true;
                }
                this.snapGrid.clear();
                return true;
            }
        }
        if (!screenClickContext.isRightButtonPressed() || screenClickContext.intercepted() || !screenClickContext.press() || !zIsWithinBounds || zClick || this.dragging) {
            return false;
        }
        WidgetSettingsPopup widgetSettingsPopup = (WidgetSettingsPopup) FluxVisuals.INSTANCE.windowController().getWindow(WidgetSettingsPopup.class);
        if (this.settings.isEmpty()) {
            if (widgetSettingsPopup == null) {
                return true;
            }
            widgetSettingsPopup.close();
            return true;
        }
        if (widgetSettingsPopup != null) {
            if (widgetSettingsPopup.matchesWidget(this.name) && widgetSettingsPopup.isOpen()) {
                widgetSettingsPopup.closeWindow();
                return true;
            }
            widgetSettingsPopup.close();
            FluxVisuals.INSTANCE.windowController().removeWindow(widgetSettingsPopup);
        }
        WidgetSettingsPopup class810Var2 = new WidgetSettingsPopup(this.name, this.settings);
        FluxVisuals.INSTANCE.windowController().newWindow(class810Var2);
        class810Var2.openAt(this.x, this.y, width(), height(), screenClickContext.resolution(), screenClickContext.scaleFactor());
        return true;
    }

    public void drawTooltip(DragRenderContext dragRenderContext) {
        float fSmoothAnimation = this.tooltipAnimation.smoothAnimation();
        if (fSmoothAnimation <= 0.0f) {
            return;
        }
        DrawEngine drawEngine154 = dragRenderContext.drawEngine();
        MatrixStack matrixStack = dragRenderContext.matrixStack();
        ColorStack colorStack115 = drawEngine154.colorStack();
        ArrayList arrayList = new ArrayList();
        arrayList.add(Lang.WIDGET_DRAG_HINT.effective());
        if (!this.settings.isEmpty()) {
            arrayList.add(Lang.WIDGET_SETTINGS_HINT.effective());
        }
        float height = Fonts.INTER_SEMIBOLD.get().getHeight(12.0f);
        float size = (this.y - (height * arrayList.size())) - 4.0f;
        boolean z = size < 0.0f;
        if (z) {
            size = this.y + height() + 4.0f;
        }
        float f = (z ? -4 : 4) * (1.0f - fSmoothAnimation);
        for (int i = 0; i < arrayList.size(); i++) {
            drawEngine154.msdfFont(matrixStack.peek().getPositionMatrix(), Fonts.INTER_SEMIBOLD.get(), (String) arrayList.get(i), ((int) this.x) + 3, size + (i * height) + f, 12.0f, 0.0f, colorStack115.computeColor(fSmoothAnimation, StencilBufferUtil.STENCIL_MASK, StencilBufferUtil.STENCIL_MASK, StencilBufferUtil.STENCIL_MASK));
        }
    }

    public boolean hasTooltip() {
        return true;
    }

    public abstract void layout(DragRenderContext dragRenderContext);

    public abstract void render(DragRenderContext dragRenderContext);

    public abstract boolean click(ScreenClickContext screenClickContext, boolean z);

    public abstract boolean cursor(MouseMoveInput mouseMoveInput, boolean z);

    public abstract void animate(WeightedEngine weightedEngine);

    public abstract void update();

    public abstract float width();

    public abstract float height();

    public void resetCursor() {
        this.cursorHovered = false;
    }

    public boolean isVisible() {
        return this.visibility.getAsBoolean();
    }

    public String getName() {
        return this.name;
    }

    public List<Setting> getSettings() {
        return this.settings;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getDragX() {
        return this.aC;
    }

    public float getDragY() {
        return this.L;
    }

    public BooleanSupplier getVisibility() {
        return this.visibility;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public ToggleAnimator getClickAnimation() {
        return this.clickAnimation;
    }

    public ToggleAnimator getHoveredAnimation() {
        return this.hoveredAnimation;
    }

    public ToggleAnimator getTooltipAnimation() {
        return this.tooltipAnimation;
    }

    public boolean isClicked() {
        return this.clicked;
    }

    public boolean isCursorHovered() {
        return this.cursorHovered;
    }

    public SnapManager getSnapGrid() {
        return this.snapGrid;
    }

    public List<Draggable> getSiblingWidgets() {
        return this.siblingWidgets;
    }

    public boolean isExcludeFromSnapGrid() {
        return this.excludeFromSnapGrid;
    }

    public ScreenResolution getResolution() {
        return this.resolution;
    }

    public Stopwatch getLastResizeTimer() {
        return this.lastResizeTimer;
    }

    public void setX(float f) {
        this.x = f;
    }

    public void setY(float f) {
        this.y = f;
    }

    public void setDragX(float f) {
        this.aC = f;
    }

    public void setDragY(float f) {
        this.L = f;
    }

    public void setSnapGrid(SnapManager snapManager) {
        this.snapGrid = snapManager;
    }

    public void setSiblingWidgets(List<Draggable> list) {
        this.siblingWidgets = list;
    }

    public void setExcludeFromSnapGrid(boolean z) {
        this.excludeFromSnapGrid = z;
    }
}
