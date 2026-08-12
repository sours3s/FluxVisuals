package ru.fluxvisuals.module.hud;

/** Оценка TPS сервера по скорости продвижения времени мира (без пакетов). */
public class TpsModule extends SimpleHudModule {
    private long lastTime;
    private long lastWorldTime;
    private int tps = 20;

    public TpsModule() {
        super("TPS", "Тиков сервера в секунду");
    }

    @Override
    protected String getText() {
        var mc = mc();
        if (mc.world == null) return null;
        long now = System.currentTimeMillis();
        long wt = mc.world.getTime();
        if (lastTime == 0) {
            lastTime = now;
            lastWorldTime = wt;
            return "TPS " + tps;
        }
        long dt = now - lastTime;
        if (dt >= 1000) {
            double delta = (wt - lastWorldTime) * 1000.0 / dt;
            tps = (int) Math.round(delta);
            lastTime = now;
            lastWorldTime = wt;
        }
        return "TPS " + tps;
    }
}
