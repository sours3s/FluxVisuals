package ru.fluxvisuals.ui.fluxgui.setting;

import java.util.function.Supplier;

public class StringSetting extends Setting<String> {
    public StringSetting(String name, String value) {
        super(name, value);
    }

    public StringSetting(String name, String value, Supplier<Boolean> visibility) {
        super(name, value, visibility);
    }
}
