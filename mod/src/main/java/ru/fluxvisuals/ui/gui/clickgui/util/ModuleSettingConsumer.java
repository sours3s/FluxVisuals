package ru.fluxvisuals.ui.gui.clickgui.util;

import ru.fluxvisuals.ui.gui.clickgui.ClickGuiModule;
import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;

/**
 * Stub: the donor clickgui's ModuleSettingConsumer (and its ClickGuiModuleSettingConsumer
 * alias) were never ported to the ClickGuiModule-based clickgui. KeybindHandler.forEachModuleSetting
 * consumes (module, setting) pairs, so this recreates that contract locally.
 */
@FunctionalInterface
public interface ModuleSettingConsumer {
    void process(ClickGuiModule module, Setting setting);
}
