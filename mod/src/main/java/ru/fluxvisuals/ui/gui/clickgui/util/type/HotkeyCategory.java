package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DisplayNamed;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.ModuleTab;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

public enum HotkeyCategory implements DisplayNamed {
    COMBAT(Lang.COMBAT, ModuleTab.COMBAT),
    MOVEMENT(Lang.MOVEMENT, ModuleTab.MOVEMENT),
    PLAYER(Lang.PLAYER, ModuleTab.PLAYER),
    RENDER(Lang.RENDER, ModuleTab.RENDER),
    MISC(Lang.MISC, ModuleTab.MISC);

    public final Translation displayName;
    public final ModuleTab tab;

    public static HotkeyCategory fromTab(ModuleTab moduleTab) {
        for (HotkeyCategory hotkeyCategory : values()) {
            if (hotkeyCategory.tab == moduleTab) {
                return hotkeyCategory;
            }
        }
        return null;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public ModuleTab getTab() {
        return this.tab;
    }

    HotkeyCategory(Translation translation, ModuleTab moduleTab) {
        this.displayName = translation;
        this.tab = moduleTab;
    }
}
