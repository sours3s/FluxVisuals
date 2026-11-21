package ru.fluxvisuals.ui.fluxgui.module.misc;

import ru.fluxvisuals.ui.fluxgui.module.Module;
import ru.fluxvisuals.ui.fluxgui.setting.Setting;

/** Заглушка модуля макросов для GUI FluxVisuals. */
public final class Macros extends Module {
    public Macros() {
        super(null);
    }

    public int getMacroCount() {
        return 0;
    }

    public Setting<?> getCommandSetting(int index) {
        return null;
    }

    public Setting<?> getBindSetting(int index) {
        return null;
    }

    public void removeMacro(int index) {
    }
}
