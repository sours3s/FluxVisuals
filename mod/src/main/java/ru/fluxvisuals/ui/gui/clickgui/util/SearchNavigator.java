package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.AbstractFrame;
import ru.fluxvisuals.ui.gui.clickgui.MenuTabElement;
import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;

public interface SearchNavigator {
    void focusFrame(AbstractFrame abstractFrame, MenuTabElement menuTabElement);

    void focusSetting(MenuTabElement menuTabElement, AbstractFrame abstractFrame, Setting setting);
}
