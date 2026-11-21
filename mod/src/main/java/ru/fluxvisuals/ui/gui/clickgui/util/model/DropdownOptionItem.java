package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.ClickableBehavior;
import ru.fluxvisuals.ui.gui.clickgui.util.type.DropdownOption;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;

public class DropdownOptionItem<T> {
    final DropdownOption<T> option;
    final ClickableBehavior clickableBehavior = new ClickableBehavior();
    public final ToggleAnimator currentOptionAnimation = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);

    public DropdownOption<T> option() {
        return this.option;
    }

    public ClickableBehavior clickableBehavior() {
        return this.clickableBehavior;
    }

    public ToggleAnimator currentOptionAnimation() {
        return this.currentOptionAnimation;
    }

    public DropdownOptionItem(DropdownOption<T> dropdownOption) {
        this.option = dropdownOption;
    }
}
