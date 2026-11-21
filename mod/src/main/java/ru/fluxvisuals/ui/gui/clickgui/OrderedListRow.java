package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

public class OrderedListRow<E> {
    public final E value;
    public final ToggleSwitch toggleSwitch;
    public final ToggleAnimator toggleAnimation;
    public final WidgetBounds rowRect = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds grabRect = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);

    public E value() {
        return this.value;
    }

    public ToggleSwitch toggleSwitch() {
        return this.toggleSwitch;
    }

    public ToggleAnimator toggleAnimation() {
        return this.toggleAnimation;
    }

    public WidgetBounds rowRect() {
        return this.rowRect;
    }

    public WidgetBounds grabRect() {
        return this.grabRect;
    }

    public OrderedListRow(E e, ToggleSwitch toggleSwitch754, ToggleAnimator toggleAnimator) {
        this.value = e;
        this.toggleSwitch = toggleSwitch754;
        this.toggleAnimation = toggleAnimator;
    }
}
