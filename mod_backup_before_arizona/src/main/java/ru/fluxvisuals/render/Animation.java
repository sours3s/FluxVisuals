package ru.fluxvisuals.render;

/** Плавное приближение значения к цели (экспоненциальное сглаживание). */
public class Animation {
    private double value;
    private double target;
    private final double speed;

    public Animation(double start, double speed) {
        this.value = start;
        this.target = start;
        this.speed = speed;
    }

    public void setTarget(double target) { this.target = target; }

    public double getTarget() { return target; }

    public double getValue() { return value; }

    /** Вызывается раз за кадр/тик. */
    public void tick() {
        value += (target - value) * Math.min(1.0, speed);
        if (Math.abs(target - value) < 0.001) value = target;
    }

    public boolean isDone() { return Math.abs(target - value) < 0.001; }

    public void setValue(double value) { this.value = value; }

    /** easeOutQuint, полезно для slide-in анимаций. */
    public static double easeOutCubic(double t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - Math.pow(1 - t, 3);
    }

    /** Плавный переворот t (0→1→0). */
    public static double pingPong(double t) {
        return 1 - Math.abs(1 - 2 * Math.max(0, Math.min(1, t)));
    }
}
