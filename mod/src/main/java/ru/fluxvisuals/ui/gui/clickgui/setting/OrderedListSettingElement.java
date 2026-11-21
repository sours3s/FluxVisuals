package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.render.HighlightAnimation;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.ModuleFrame;
import ru.fluxvisuals.ui.gui.clickgui.util.type.MouseButtonAction;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.OrderedListRow;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.ToggleSwitch;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.lang.Enum;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class OrderedListSettingElement<E extends Enum<E>> extends ModuleFrame {
    public static final float C = 3.0f;
    public static final float D = 10.0f;
    public static final float E = 20.0f;
    public static final float F = 6.0f;
    public static final float G = 4.0f;
    public static final float H = 12.0f;
    public static final float I = 4.0f;
    public static final float J = 22.0f;
    public static final float K = 15.0f;
    public static final int fontSize = 12;
    public final Supplier<List<E>> orderSupplier;
    public final Supplier<Set<E>> selectedSupplier;
    public final Function<E, Translation> nameFunction;

    public final Consumer<E> toggleConsumer;
    public final BiConsumer<Integer, Integer> moveConsumer;
    public Supplier<Boolean> visibilitySupplier;
    public boolean dragging;

    public E draggedValue;
    public float dragOffset;
    public float L;
    public final MsdfFont font = Fonts.INTER_BOLD.get();
    public final GlTexture grabTexture = new GlTexture(new ClasspathResource("/icons/menu/new/grab.png"));
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final Map<E, OrderedListRow<E>> rowCache = new HashMap();
    public final List<OrderedListRow<E>> rows = new ArrayList();
    public final List<E> currentOrder = new ArrayList();
    public final WidgetBounds bounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public int draggedIndex = -1;

    public OrderedListSettingElement(Supplier<List<E>> supplier, Supplier<Set<E>> supplier2, Function<E, Translation> function, Consumer<E> consumer, BiConsumer<Integer, Integer> biConsumer) {
        this.orderSupplier = supplier;
        this.selectedSupplier = supplier2;
        this.nameFunction = function;
        this.toggleConsumer = consumer;
        this.moveConsumer = biConsumer;
    }

    @Override
    public void highlight() {
        this.highlightAnimation.trigger();
    }

    @Override
    public void clearHighlight() {
        this.highlightAnimation.clearHighlight();
    }

    @Override
    public void draw(DrawCtx drawCtx, float f, float f2) {
        rebuildRows();
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        float fY = y();
        float fMethod001 = computeHeight();
        this.bounds.withPosition(x(), fY).withSize(f, fMethod001);
        for (int i = 0; i < this.rows.size(); i++) {
            OrderedListRow<E> orderedListRow = this.rows.get(i);
            float f3 = fY + (i * 26.0f);
            float fClamp = f3;
            if (this.dragging && orderedListRow.value() == this.draggedValue) {
                fClamp = clamp(this.L, fY, (fY + fMethod001) - E);
            }
            orderedListRow.rowRect().withPosition(x(), f3).withSize(f, E);
            orderedListRow.grabRect().withPosition(x(), f3).withSize(24.0f, E);
            float fX = x();
            drawCtx.texture(this.grabTexture, fX, (fClamp + D) - F, H, H, colorStack115.computeColor(ThemePalette.grey.argb()));
            drawCtx.text(this.font, this.nameFunction.apply(orderedListRow.value()).effective(), fontSize, fX + H + 4.0f, (fClamp + D) - (this.font.getHeight(H) / 2.0f), colorStack115.interpolate(colorStack115.computeColor(themePalette.text().tone(600).argb()), colorStack115.computeColor(themePalette.text().tone(200).argb()), orderedListRow.toggleAnimation()));
            float fX2 = (x() + f) - J;
            float f4 = (fClamp + D) - 7.5f;
            orderedListRow.toggleSwitch().setPosition(fX2, f4);
            orderedListRow.toggleSwitch().setClickableArea(x(), f4, f, K);
            orderedListRow.toggleSwitch().render(drawCtx);
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = super.handleInput(inputEventContext, z);
        if (zHandleInput || !visible()) {
            return zHandleInput;
        }
        InputEvent inputEvent691 = inputEventContext.inputEvent();
        if (inputEvent691 instanceof MouseButtonInput) {
            MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent691;
            if (mouseButtonInput.button() != 0) {
                return false;
            }
            MouseButtonAction mouseButtonAction = mouseButtonInput.action();
            if (mouseButtonAction.press() && !z) {
                for (int i = 0; i < this.rows.size(); i++) {
                    OrderedListRow<E> orderedListRow = this.rows.get(i);
                    WidgetBounds widgetBounds = orderedListRow.grabRect();
                    if (inputEventContext.inArea(widgetBounds.x(), widgetBounds.y(), widgetBounds.width(), widgetBounds.height())) {
                        startDrag(inputEventContext.logicalMousePosition().y(), i, orderedListRow.value());
                        return true;
                    }
                }
            }
            if (mouseButtonAction.release() && this.dragging) {
                endDrag();
                return true;
            }
        }
        if ((inputEventContext.inputEvent() instanceof CursorMoveInput) && this.dragging) {
            updateDrag(inputEventContext.logicalMousePosition().y());
            return true;
        }
        if (this.dragging) {
            return false;
        }
        boolean z2 = z;
        Iterator<OrderedListRow<E>> it = this.rows.iterator();
        while (it.hasNext()) {
            if (it.next().toggleSwitch().handleInput(inputEventContext, z2)) {
                z2 = true;
            }
        }
        return z2;
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibilitySupplier != null) {
            visible(this.visibilitySupplier.get().booleanValue());
        }
        if (visible()) {
            rebuildRows();
            for (OrderedListRow<E> orderedListRow : this.rows) {
                orderedListRow.toggleSwitch().animation(weightedEngine);
                orderedListRow.toggleAnimation().state(this.selectedSupplier.get().contains(orderedListRow.value()));
                orderedListRow.toggleAnimation().animate(weightedEngine);
            }
            this.highlightAnimation.animate(weightedEngine);
            super.animation(weightedEngine);
        }
    }

    @Override
    public void handleClose() {
        endDrag();
        super.handleClose();
    }

    @Override
    public float height() {
        rebuildRows();
        return computeHeight();
    }

    public void rebuildRows() {
        List<E> list = this.orderSupplier.get();
        if (list == null) {
            return;
        }
        if (!this.currentOrder.equals(list)) {
            this.currentOrder.clear();
            this.currentOrder.addAll(list);
        }
        this.rowCache.keySet().retainAll(new HashSet(this.currentOrder));
        this.rows.clear();
        Iterator<E> it = this.currentOrder.iterator();
        while (it.hasNext()) {
            this.rows.add(this.rowCache.computeIfAbsent(it.next(), this::createRow));
        }
    }

    public OrderedListRow<E> createRow(E e) {
        ToggleSwitch toggleSwitch = new ToggleSwitch(J, K, () -> {
            return Boolean.valueOf(this.selectedSupplier.get().contains(e));
        });
        ToggleAnimator toggleAnimator = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
        toggleAnimator.force(this.selectedSupplier.get().contains(e));
        OrderedListRow<E> orderedListRow = new OrderedListRow<>(e, toggleSwitch, toggleAnimator);
        toggleSwitch.runnable(() -> {
            boolean z = !this.selectedSupplier.get().contains(e);
            this.toggleConsumer.accept(e);
            orderedListRow.toggleAnimation().state(z);
        });
        return orderedListRow;
    }

    public float computeHeight() {
        int size = this.rows.size();
        if (size == 0) {
            return 0.0f;
        }
        return (E * size) + (F * Math.max(0, size - 1));
    }

    public void startDrag(float f, int i, E e) {
        this.dragging = true;
        this.draggedIndex = i;
        this.draggedValue = e;
        this.dragOffset = f - (this.bounds.y() + (i * 26.0f));
        this.L = f - this.dragOffset;
    }

    public void updateDrag(float f) {
        float fY = this.bounds.y();
        float fY2 = (this.bounds.y() + this.bounds.height()) - E;
        if (fY2 < fY) {
            fY2 = fY;
        }
        this.L = clamp(f - this.dragOffset, fY, fY2);
        int iMethod006 = indexFromY(f);
        if (iMethod006 == this.draggedIndex || iMethod006 < 0 || iMethod006 >= this.rows.size()) {
            return;
        }
        moveRow(this.draggedIndex, iMethod006);
        this.moveConsumer.accept(Integer.valueOf(this.draggedIndex), Integer.valueOf(iMethod006));
        this.draggedIndex = iMethod006;
    }

    public int indexFromY(float f) {
        float fY = f - this.bounds.y();
        if (fY <= 0.0f) {
            return 0;
        }
        return Math.max(0, Math.min(this.rows.size() - 1, (int) Math.floor(fY / 26.0f)));
    }

    public void moveRow(int i, int i2) {
        if (i == i2 || i < 0 || i2 < 0 || i >= this.rows.size() || i2 >= this.rows.size()) {
            return;
        }
        this.rows.add(i2, this.rows.remove(i));
        this.currentOrder.add(i2, this.currentOrder.remove(i));
    }

    public void endDrag() {
        this.dragging = false;
        this.draggedIndex = -1;
        this.draggedValue = null;
    }

    public float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    public OrderedListSettingElement<E> visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }
}
