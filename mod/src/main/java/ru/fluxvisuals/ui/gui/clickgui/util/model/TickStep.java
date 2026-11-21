package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.util.type.PerformAction;

import java.util.function.BooleanSupplier;

public class TickStep implements Comparable<TickStep> {
    public TickStep() {
    }

    public TickStep(int i, PerformAction performAction, BooleanSupplier booleanSupplier, int i2) {
    }

    @Override
    public int compareTo(TickStep o) {
        return 0;
    }

    public PerformAction action() {
        return null;
    }

    public BooleanSupplier condition() {
        return null;
    }

    public int ticks() {
        return 0;
    }
}
