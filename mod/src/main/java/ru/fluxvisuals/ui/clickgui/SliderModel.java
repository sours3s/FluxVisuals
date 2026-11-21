package ru.fluxvisuals.ui.clickgui;

public final class SliderModel {
    private final float min;
    private final float max;
    private float value;
    private float targetProgress;
    private float progress;

    public SliderModel(float min, float max, float initial) {
        this.min = min;
        this.max = max;
        this.value = initial;
        this.progress = (max > min) ? Math.max(0f, Math.min(1f, (initial - min) / (max - min))) : 0f;
        this.targetProgress = this.progress;
    }

    public float value() {
        return value;
    }

    public void setFromMouse(float mouseX, float sliderX, float sliderWidth) {
        if (sliderWidth <= 0) return;
        float p = Math.max(0f, Math.min(1f, (mouseX - sliderX) / sliderWidth));
        this.targetProgress = p;
        this.value = min + p * (max - min);
    }

    public float update(float dt) {
        progress += (targetProgress - progress) * (1.0f - (float) Math.exp(-22.0f * dt));
        if (Math.abs(targetProgress - progress) < 0.001f) {
            progress = targetProgress;
        }
        return progress;
    }
}
