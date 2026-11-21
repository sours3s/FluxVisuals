package ru.fluxvisuals.ui.gui.clickgui.util.render;
import ru.fluxvisuals.ui.gui.clickgui.util.math.EasingFunction;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.util.MathUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

public class ToggleAnimator {
    public static final int defaultDuration = 220;
    public final EasingFunction easeIn;
    public final EasingFunction easeOut;
    public final float speed;

    public boolean state;
    public float animation;
    public float smoothAnimation;
    public float maxValueScale;

    public ToggleAnimator(EasingFunction easingFunction) {
        this(defaultDuration, easingFunction, easingFunction);
    }

    public ToggleAnimator(int i, EasingFunction easingFunction) {
        this(i, easingFunction, easingFunction);
    }

    public ToggleAnimator(int i, EasingFunction easingFunction, EasingFunction class356Var2) {
        this.maxValueScale = 1.0f;
        this.easeIn = easingFunction;
        this.easeOut = class356Var2;
        this.speed = 1000.0f / i;
    }

    public void animate(WeightedEngine weightedEngine) {
        float fWeight = weightedEngine.weight() * this.speed;
        float fAnimationMultiplier = weightedEngine.engine().animationMultiplier();
        this.animation = MathUtil.clamp(this.animation + (this.state ? fWeight : -fWeight), 0.0f, 1.0f);
        float fEase = (this.state ? this.easeIn : this.easeOut).ease(this.state ? this.animation : 1.0f - this.animation);
        this.smoothAnimation = (this.state ? fEase : 1.0f - fEase) * fAnimationMultiplier * this.maxValueScale;
    }

    public void fill() {
        this.animation = 1.0f;
    }

    public void force(boolean z) {
        this.state = z;
        this.animation = z ? 1.0f : 0.0f;
        this.smoothAnimation = (z ? 1.0f : 0.0f) * this.maxValueScale;
    }

    public void invert() {
        this.state = !this.state;
    }

    public float interpolate(float f, float f2) {
        return f + ((f2 - f) * this.smoothAnimation);
    }

    public boolean finished() {
        return isZero() || isOne();
    }

    public boolean isZero() {
        return this.animation == 0.0f;
    }

    public boolean isOne() {
        return this.animation == 1.0f;
    }

    public static ToggleAnimator times(int i) {
        return times(i, Easings.EASE_IN_OUT_CUBIC);
    }

    public static ToggleAnimator times(int i, int i2) {
        return times(i, i2, Easings.EASE_IN_OUT_CUBIC);
    }

    public static ToggleAnimator times(int i, EasingFunction easingFunction) {
        return new ToggleAnimator((defaultDuration / i) * i, easingFunction).maxValue(i);
    }

    public static ToggleAnimator times(int i, int i2, EasingFunction easingFunction) {
        return new ToggleAnimator(i2 * i, easingFunction).maxValue(i);
    }

    public static ToggleAnimator times(int i, int i2, EasingFunction easingFunction, EasingFunction class356Var2) {
        return new ToggleAnimator(i2 * i, easingFunction, class356Var2).maxValue(i);
    }

    public ToggleAnimator state(boolean z) {
        this.state = z;
        return this;
    }

    public boolean state() {
        return this.state;
    }

    public ToggleAnimator animation(float f) {
        this.animation = f;
        return this;
    }

    public float animation() {
        return this.animation;
    }

    public float smoothAnimation() {
        return this.smoothAnimation;
    }

    public ToggleAnimator maxValue(float f) {
        this.maxValueScale = f;
        return this;
    }
}
