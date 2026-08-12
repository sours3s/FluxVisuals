package ru.fluxvisuals.module.hud;

/** Текущая скорость игрока в км/ч. */
public class SpeedometerModule extends SimpleHudModule {
    public SpeedometerModule() {
        super("Speedometer", "Скорость в км/ч");
    }

    @Override
    protected String getText() {
        var p = mc().player;
        if (p == null) return null;
        var vel = p.getVelocity();
        double mps = Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20.0;
        return String.format("Speed %.1f km/h", mps * 3.6);
    }
}
