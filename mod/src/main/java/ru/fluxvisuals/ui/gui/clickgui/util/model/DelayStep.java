package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.util.type.PerformAction;

import java.util.function.BooleanSupplier;

public class DelayStep implements Comparable<DelayStep> {
    public DelayStep() {
    }

    public DelayStep(int i, PerformAction performAction, BooleanSupplier booleanSupplier, int i2) {
    }

    @Override
    public int compareTo(DelayStep o) {
        return 0;
    }

    public PerformAction action() {
        return null;
    }

    public BooleanSupplier condition() {
        return null;
    }

    public int delay() {
        return 0;
    }
}
