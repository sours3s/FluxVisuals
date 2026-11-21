package ru.fluxvisuals.ui.gui.clickgui.util.render;
import ru.fluxvisuals.ui.gui.clickgui.util.math.EasingFunction;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

public class AnimatedFloat {
    public static final int defaultSpeed = 200;
    public final EasingFunction easing;
    public final float progressSpeed;
    public float startValue;
    public float destination;
    public float progress;
    public float animatedValue;

    public AnimatedFloat(EasingFunction easingFunction) {
        this(defaultSpeed, easingFunction);
    }

    public AnimatedFloat(int i, EasingFunction easingFunction) {
        this.easing = easingFunction;
        this.progressSpeed = 1000.0f / i;
    }

    public AnimatedFloat destination(float f) {
        if (this.destination != f) {
            this.startValue = currentValue();
            this.destination = f;
            this.progress = 0.0f;
        }
        return this;
    }

    public void set(float f) {
        this.startValue = f;
        this.destination = f;
        this.progress = 1.0f;
        this.animatedValue = f;
    }

    public void animate(WeightedEngine weightedEngine) {
        this.progress = Math.min(1.0f, this.progress + (weightedEngine.weight() * this.progressSpeed));
        this.animatedValue = this.startValue + ((this.destination - this.startValue) * this.easing.ease(this.progress) * weightedEngine.engine().animationMultiplier());
    }

    public float currentValue() {
        return this.startValue + ((this.destination - this.startValue) * this.progress);
    }

    public boolean isAtDestination() {
        return this.progress >= 1.0f;
    }

    public boolean isZero() {
        return this.animatedValue <= 0.01f;
    }

    public boolean isOne() {
        return this.animatedValue == 1.0f;
    }

    public float destination() {
        return this.destination;
    }

    public float animatedValue() {
        return this.animatedValue;
    }
}
