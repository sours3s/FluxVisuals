package ru.fluxvisuals.ui.clickgui;

public final class AnimatedFloat {
    private float value;
    private float target;

    public AnimatedFloat(float initial) {
        this.value = initial;
        this.target = initial;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public void setImmediate(float value) {
        this.value = value;
        this.target = value;
    }

    public float get() {
        return value;
    }

    public float target() {
        return target;
    }

    public float update(float dt, float speed) {
        value += (target - value) * (1.0f - (float) Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.0005f) {
            value = target;
        }
        return value;
    }
}
