package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.ClickableBehavior;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;

public class MultiSelectOptionButton<T> {
    public final T option;
    public final ClickableBehavior clickableBehavior = new ClickableBehavior();
    public final ToggleAnimator toggleAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);

    public MultiSelectOptionButton(T t) {
        this.option = t;
    }

    public T option() {
        return this.option;
    }

    public ClickableBehavior clickableBehavior() {
        return this.clickableBehavior;
    }

    public ToggleAnimator toggleAnimation() {
        return this.toggleAnimation;
    }
}
