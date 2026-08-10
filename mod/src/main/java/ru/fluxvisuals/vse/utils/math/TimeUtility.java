package ru.fluxvisuals.vse.utils.math;

public class TimeUtility {
    private long lastMS = 0L;

    public boolean reached(long ms, boolean reset) {
        if (System.currentTimeMillis() - lastMS >= ms) {
            if (reset) {
                reset();
            }
            return true;
        }
        return false;
    }

    public void reset() {
        lastMS = System.currentTimeMillis();
    }

    public long getCurrentMS() {
        return System.currentTimeMillis();
    }
}
