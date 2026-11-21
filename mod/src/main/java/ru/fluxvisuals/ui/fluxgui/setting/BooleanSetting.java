package ru.fluxvisuals.ui.fluxgui.setting;

import java.util.function.Supplier;

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, boolean value) {
        super(name, value);
    }

    public BooleanSetting(String name, boolean value, Supplier<Boolean> visibility) {
        super(name, value, visibility);
    }

    public void toggle() {
        this.value = !this.value;
    }

    public boolean isValue() {
        return this.getValue();
    }
}
