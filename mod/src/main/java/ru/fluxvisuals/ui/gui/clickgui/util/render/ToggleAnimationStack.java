package ru.fluxvisuals.ui.gui.clickgui.util.render;

public class ToggleAnimationStack {
    public final AnimationStack stack = new AnimationStack();

    public void begin() {
        this.stack.begin();
    }

    public float animationMultiplier() {
        return this.stack.animation();
    }

    public void push() {
        this.stack.push();
    }

    public void animation(ToggleAnimator toggleAnimator) {
        this.stack.animation(toggleAnimator.smoothAnimation());
    }

    public void pop() {
        this.stack.pop();
    }

    public void end() {
        this.stack.end();
    }
}
