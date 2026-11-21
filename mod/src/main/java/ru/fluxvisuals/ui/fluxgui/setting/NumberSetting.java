package ru.fluxvisuals.ui.fluxgui.setting;

import java.util.function.Supplier;

public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double increment;

    public NumberSetting(String name, double value, double min, double max, double increment) {
        super(name, value);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public NumberSetting(String name, double value, double min, double max, double increment, Supplier<Boolean> visibility) {
        super(name, value, visibility);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public double getIncrement() {
        return this.increment;
    }

    public void setValue(Double value) {
        double precision = 1.0 / this.increment;
        this.value = Math.round(Math.max(this.min, Math.min(this.max, value)) * precision) / precision;
    }

    public float getFloatValue() {
        return this.value.floatValue();
    }

    public int getIntValue() {
        return this.value.intValue();
    }
}
