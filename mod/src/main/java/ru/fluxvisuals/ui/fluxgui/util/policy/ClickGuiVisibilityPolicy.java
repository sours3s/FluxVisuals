package ru.fluxvisuals.ui.fluxgui.util.policy;

import ru.fluxvisuals.ui.fluxgui.module.Module;

/** Политика отображения модулей в GUI FluxVisuals. */
public final class ClickGuiVisibilityPolicy {
    private ClickGuiVisibilityPolicy() {
    }

    public static boolean shouldHideModuleFromClickGui(Module module) {
        return false;
    }
}
