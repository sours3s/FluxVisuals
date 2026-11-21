package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.ClickableBehavior;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.type.MultiSelectOption;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.ToggleSwitch;

import java.util.function.Supplier;

public class MultiSelectOptionItem<T> {
    final MultiSelectOption<T> option;
    final ClickableBehavior clickableBehavior = new ClickableBehavior();
    public final ToggleAnimator currentOptionAnimation = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);

    final ToggleSwitch toggleSwitch;

    public MultiSelectOptionItem(MultiSelectOption<T> multiSelectOption, Supplier<Boolean> supplier) {
        this.option = multiSelectOption;
        this.toggleSwitch = new ToggleSwitch(22.0f, 15.0f, supplier);
    }

    public MultiSelectOption<T> option() {
        return this.option;
    }

    public ClickableBehavior clickableBehavior() {
        return this.clickableBehavior;
    }

    public ToggleAnimator currentOptionAnimation() {
        return this.currentOptionAnimation;
    }

    public ToggleSwitch toggleSwitch() {
        return this.toggleSwitch;
    }
}
