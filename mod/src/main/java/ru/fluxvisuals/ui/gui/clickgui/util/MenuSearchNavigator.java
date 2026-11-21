package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.AbstractFrame;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.MenuTabElement;
import ru.fluxvisuals.ui.gui.clickgui.ModuleCard;
import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ModuleCategory;

public final class MenuSearchNavigator implements SearchNavigator {
    @Override
    public void focusFrame(AbstractFrame abstractFrame, MenuTabElement menuTabElement) {
        TabsController tabsController733 = FluxVisuals.INSTANCE.tabsController();
        if (tabsController733.current() != menuTabElement) {
            tabsController733.open(menuTabElement);
            menuTabElement.markFramesDirty();
        }
        ensureCategoryVisible(tabsController733, menuTabElement, abstractFrame);
        menuTabElement.focusFrame(abstractFrame);
    }

    @Override
    public void focusSetting(MenuTabElement menuTabElement, AbstractFrame abstractFrame, Setting setting) {
        TabsController tabsController733 = FluxVisuals.INSTANCE.tabsController();
        if (tabsController733.current() != menuTabElement) {
            tabsController733.open(menuTabElement);
            menuTabElement.markFramesDirty();
        }
        ensureCategoryVisible(tabsController733, menuTabElement, abstractFrame);
        menuTabElement.focusSetting(abstractFrame, setting);
    }

    public void ensureCategoryVisible(TabsController tabsController, MenuTabElement menuTabElement, AbstractFrame abstractFrame) {
        if (abstractFrame instanceof ModuleCard) {
            tabsController.ensureCategoryVisible(menuTabElement, ModuleCategory.forTab(((ModuleCard) abstractFrame).clickGuiModule.getModuleTab()));
        }
    }
}
