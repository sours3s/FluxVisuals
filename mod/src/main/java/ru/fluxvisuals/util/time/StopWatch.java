package ru.fluxvisuals.util.time;

public class StopWatch {
    private long lastTime = System.currentTimeMillis();

    public StopWatch() {}
    public long getTime() {
        return System.currentTimeMillis();
    }
    public long elapsedTime() {
        return System.currentTimeMillis() - lastTime;
    }
    public void reset() {
        lastTime = System.currentTimeMillis();
    }
}
