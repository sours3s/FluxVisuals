package ru.fluxvisuals.ui.gui.clickgui.externall.math;

public class StopGPT {

    private long lastMS = System.currentTimeMillis();

    public void reset() {
        lastMS = System.currentTimeMillis();
    }

    public boolean hasTimePassed(long ms) {
        return System.currentTimeMillis() - lastMS >= ms;
    }

    public long getPassedTime() {
        return System.currentTimeMillis() - lastMS;
    }
}

