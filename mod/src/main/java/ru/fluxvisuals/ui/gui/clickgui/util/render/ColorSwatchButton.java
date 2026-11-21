package ru.fluxvisuals.ui.gui.clickgui.util.render;
import ru.fluxvisuals.ui.gui.clickgui.ClickableBehavior;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;

public class ColorSwatchButton {
    final int color;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public final ToggleAnimator selectAnim = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);

    public int color() {
        return this.color;
    }

    public ClickableBehavior clickable() {
        return this.clickable;
    }

    public ToggleAnimator selectAnim() {
        return this.selectAnim;
    }

    public ColorSwatchButton(int i) {
        this.color = i;
    }
}
