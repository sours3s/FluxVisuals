package ru.fluxvisuals.ui.fluxgui.setting;

import java.util.function.Supplier;

public class ActionSetting extends Setting<Void> {
    private final Runnable action;

    public ActionSetting(String name, Runnable action) {
        super(name, null);
        this.action = action;
    }

    public ActionSetting(String name, Runnable action, Supplier<Boolean> visibility) {
        super(name, null, visibility);
        this.action = action;
    }

    public void trigger() {
        if (this.action != null) {
            this.action.run();
        }
    }
}
