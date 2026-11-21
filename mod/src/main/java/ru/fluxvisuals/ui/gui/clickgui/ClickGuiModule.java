package ru.fluxvisuals.ui.gui.clickgui;

import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;
import ru.fluxvisuals.ui.gui.clickgui.util.type.BindMode;

import java.util.List;

public interface ClickGuiModule {
    String getName();
    List<Integer> getKeyBind();
    void setKey(List<Integer> keys);
    void setBind(List<Integer> keys, BindMode type);
    BindMode getType();
    void setType(BindMode type);
    boolean isState();
    void setState(boolean enable);
    void setStateSilent(boolean enable);
    void switchState();
    List<Setting> getSettings();
    ModuleTab getModuleTab();
}