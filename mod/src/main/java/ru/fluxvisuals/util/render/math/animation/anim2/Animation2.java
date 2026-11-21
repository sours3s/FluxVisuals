package ru.fluxvisuals.util.render.math.animation.anim2;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.util.render.math.animation.Direction;

@Environment(EnvType.CLIENT)
public class Animation2 {
    private long startTime;
    private long duration;
    private double fromValue;
    private double toValue;
    private double value;
    private Easing easing = Easing.LINEAR;
    private Direction direction = Direction.FORWARDS;
    private boolean finished = true;

    public Animation2() {
        this(0, 0, Easing.LINEAR);
    }

    public Animation2(double startValue, double targetValue, Easing easing) {
        this.value = startValue;
        this.fromValue = startValue;
        this.toValue = targetValue;
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.duration = 150;
    }

    public void update() {
        if (finished) return;
        long elapsed = System.currentTimeMillis() - startTime;
        double progress = Math.min((double) elapsed / duration, 1.0);
        if (progress >= 1.0) {
            value = toValue;
            finished = true;
        } else {
            value = interpolate(fromValue, toValue, easing.getFunction().apply(progress));
        }
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setTarget(double target) {
        this.toValue = target;
        this.finished = false;
    }

    public void run(double target, float duration, Easing easing) {
        this.fromValue = this.value;
        this.toValue = target;
        this.duration = (long) (duration * 1000);
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.finished = false;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public float get() {
        return (float) value;
    }

    public float getValue() {
        return (float) value;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isSettled() {
        return finished;
    }

    public double getDuration() {
        return duration;
    }

    public double getToValue() {
        return toValue;
    }

    private double interpolate(double start, double end, double pct) {
        return start + (end - start) * pct;
    }
}
