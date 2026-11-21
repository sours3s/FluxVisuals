package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimationStack;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ButtonAction;
import ru.fluxvisuals.ui.gui.clickgui.util.math.DeltaTimeTracker;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DragRenderContext;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.WidgetMouseButtonEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScreenClickContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ScreenResolution;
import ru.fluxvisuals.ui.gui.clickgui.util.SnapManager;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;

public class WidgetStack {
    public ScreenResolution resolution;
    public final List<Draggable> widgets = new ArrayList();
    public final DeltaTimeTracker deltaTimeTracker = new DeltaTimeTracker();
    public final ToggleAnimationStack animationStack = new ToggleAnimationStack();
    public final SnapManager snapManager = new SnapManager();

    public Draggable activeWidget = null;

    public void draw() {
        Mc mc = Mc.INSTANCE;
        ScreenResolution screenResolution = ScreenResolution.resolution();
        float fMax = Math.max(FluxVisuals.INSTANCE.windowController().dpiScaleFactor(), 0.01f);
        this.resolution = new ScreenResolution(Math.max(1, Math.round(screenResolution.screenWidth() / fMax)), Math.max(1, Math.round(screenResolution.screenHeight() / fMax)));
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.scale(fMax, fMax, 1.0f);
        WeightedEngine weightedEngine = new WeightedEngine(this.deltaTimeTracker.elapsedUnit(), this.animationStack);
        Mouse mouse = mc.getMouse();
        for (Draggable draggable : this.widgets) {
            draggable.setSnapGrid(this.snapManager);
            draggable.setSiblingWidgets(this.widgets);
        }
        if (mc.getCurrentScreen() instanceof ChatScreen) {
            boolean z = false;
            for (int size = this.widgets.size() - 1; size >= 0; size--) {
                if (this.widgets.get(size).onCursor(new MouseMoveInput(z, mouse, this.resolution, fMax))) {
                    z = true;
                }
            }
        } else {
            this.widgets.forEach((v0) -> {
                v0.resetCursor();
            });
            this.snapManager.clear();
        }
        boolean zMethod005 = isAnyDragging();
        this.snapManager.animate(weightedEngine, zMethod005);
        this.animationStack.begin();
        this.widgets.forEach(class806Var2 -> {
            class806Var2.animation(weightedEngine);
        });
        this.animationStack.end();
        DrawEngine drawEngine154 = FluxVisuals.INSTANCE.drawEngine();
        DragRenderContext dragRenderContext = new DragRenderContext(this.resolution, mouse, matrixStack, drawEngine154, FluxVisuals.INSTANCE.theme(), fMax);
        drawEngine154.begin();
        this.widgets.forEach(class806Var3 -> {
            class806Var3.layout(dragRenderContext);
            class806Var3.draw(dragRenderContext);
        });
        this.widgets.forEach(class806Var4 -> {
            if (class806Var4.hasTooltip()) {
                class806Var4.drawTooltip(dragRenderContext);
            }
            class806Var4.updateDrag(dragRenderContext);
        });
        if (zMethod005) {
            this.snapManager.render(dragRenderContext);
        }
        drawEngine154.end();
    }

    public void click(WidgetMouseButtonEvent widgetMouseButtonEvent) {
        Mc mc = Mc.INSTANCE;
        if (mc.getCurrentScreen() instanceof ChatScreen) {
            Mouse mouse = mc.getMouse();
            boolean z = false;
            for (int size = this.widgets.size() - 1; size >= 0; size--) {
                Draggable draggable = this.widgets.get(size);
                if (draggable.onClick(new ScreenClickContext(z, mouse, widgetMouseButtonEvent.action(), this.resolution, widgetMouseButtonEvent.button(), Math.max(FluxVisuals.INSTANCE.windowController().dpiScaleFactor(), 0.01f)))) {
                    if (!z) {
                        markActive(draggable);
                    }
                    z = true;
                }
            }
            if (widgetMouseButtonEvent.action() == ButtonAction.RELEASE) {
                this.snapManager.clear();
            }
        }
    }

    public boolean isAnyDragging() {
        for (Draggable draggable : this.widgets) {
            if (draggable.isDragging() && draggable.isVisible()) {
                return true;
            }
        }
        return false;
    }

    public void handleResize(int i, int i2) {
        this.widgets.forEach(draggable -> {
            draggable.handleResize(i, i2);
        });
    }

    public void update() {
        this.widgets.forEach((v0) -> {
            v0.updateWidget();
        });
    }

    public void markActive(Draggable draggable) {
        if (draggable == this.activeWidget || draggable == null) {
            return;
        }
        this.activeWidget = draggable;
        if (this.widgets.size() > 1) {
            bringToFront(draggable);
        }
    }

    public void newWidget(Draggable draggable) {
        if (this.widgets.contains(draggable)) {
            return;
        }
        this.widgets.add(draggable);
        draggable.setSnapGrid(this.snapManager);
        draggable.setSiblingWidgets(this.widgets);
    }

    public void bringToFront(Draggable draggable) {
        ArrayList arrayList = new ArrayList(this.widgets);
        int size = this.widgets.size() - 1;
        int iIndexOf = this.widgets.indexOf(draggable);
        if (iIndexOf == size) {
            return;
        }
        int i = 0;
        for (int i2 = size; i2 >= 0; i2--) {
            if (i2 == iIndexOf) {
                i = 1;
            } else {
                this.widgets.set((i2 - 1) + i, (Draggable) arrayList.get(i2));
            }
        }
        this.widgets.set(size, draggable);
    }

    public List<Draggable> widgets() {
        return this.widgets;
    }
}
