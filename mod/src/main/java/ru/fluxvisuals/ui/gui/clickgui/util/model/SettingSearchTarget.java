package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.AbstractFrame;
import ru.fluxvisuals.ui.gui.clickgui.MenuTabElement;
import ru.fluxvisuals.ui.gui.clickgui.util.SearchNavigator;
import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;


public final class SettingSearchTarget implements SearchNavTarget {
    public final MenuTabElement tab;
    public final AbstractFrame frame;
    public final Setting setting;

    public SettingSearchTarget(MenuTabElement menuTabElement, AbstractFrame abstractFrame, Setting setting661) {
        this.tab = menuTabElement;
        this.frame = abstractFrame;
        this.setting = setting661;
    }

    @Override
    public void navigate(SearchNavigator searchNavigator) {
        searchNavigator.focusSetting(this.tab, this.frame, this.setting);
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "tab=" + this.tab + ", " + "frame=" + this.frame + ", " + "setting=" + this.setting + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.tab, this.frame, this.setting);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SettingSearchTarget)) return false;
        SettingSearchTarget o = (SettingSearchTarget) obj;
        return java.util.Objects.equals(this.tab, o.tab) && java.util.Objects.equals(this.frame, o.frame) && java.util.Objects.equals(this.setting, o.setting);
    }
public MenuTabElement tab() {
        return this.tab;
    }

    public AbstractFrame frame() {
        return this.frame;
    }

    public Setting setting() {
        return this.setting;
    }
}
