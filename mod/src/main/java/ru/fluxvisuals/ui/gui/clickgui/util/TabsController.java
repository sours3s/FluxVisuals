package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.MenuTabElement;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ModuleCategory;
import ru.fluxvisuals.ui.gui.clickgui.ModuleGridLayout;
import ru.fluxvisuals.ui.gui.clickgui.ModuleTab;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TabsController {
    public final MenuTabElement combat = new MenuTabElement(ModuleTab.COMBAT, Lang.COMBAT, "combat", new ModuleGridLayout());
    public final MenuTabElement render = new MenuTabElement(ModuleTab.RENDER, Lang.RENDER, "render", new ModuleGridLayout());
    public final MenuTabElement player = new MenuTabElement(ModuleTab.PLAYER, Lang.PLAYER, "player", new ModuleGridLayout());
    public final MenuTabElement movement = new MenuTabElement(ModuleTab.MOVEMENT, Lang.MOVEMENT, "movement", new ModuleGridLayout());
    public final MenuTabElement misc = new MenuTabElement(ModuleTab.MISC, Lang.MISC, "misc", new ModuleGridLayout());
    public final MenuTabElement theme = new MenuTabElement(null, Lang.THEMES, "themes");
    public final MenuTabElement config = new MenuTabElement(null, Lang.CONFIGS, "configs");
    public final List<MenuTabElement> tabElements = List.of(this.combat, this.movement, this.player, this.render, this.misc, this.config, this.theme);
    public final Map<ModuleTab, MenuTabElement> tabByModuleTab = Map.of(ModuleTab.COMBAT, this.combat, ModuleTab.MOVEMENT, this.movement, ModuleTab.PLAYER, this.player, ModuleTab.RENDER, this.render, ModuleTab.MISC, this.misc);

    public final EnumMap<ModuleTab, Set<ModuleCategory>> selectedCategories = new EnumMap<>(ModuleTab.class);
    public MenuTabElement current = this.combat;
    public MenuTabElement previousTabElement;

    public TabsController() {
        for (ModuleTab moduleTab : ModuleTab.values()) {
            this.selectedCategories.put(moduleTab, new HashSet());
        }
        this.theme.setActive(false);
    }

    public void open(MenuTabElement menuTabElement) {
        if (menuTabElement == null || !menuTabElement.active()) {
            return;
        }
        this.previousTabElement = this.current;
        this.current = menuTabElement;
        menuTabElement.open();
    }

    public void revert() {
        open(this.previousTabElement);
    }

    public MenuTabElement tabElement(ModuleTab moduleTab) {
        if (moduleTab == null) {
            return null;
        }
        return this.tabByModuleTab.get(moduleTab);
    }

    public void ensureCategoryVisible(MenuTabElement menuTabElement, ModuleCategory moduleCategory) {
        Set<ModuleCategory> set;
        if (menuTabElement == null || moduleCategory == null) {
            return;
        }
        ModuleTab moduleTab847 = menuTabElement.moduleTab();
        if (moduleCategory.supports(moduleTab847) && (set = this.selectedCategories.get(moduleTab847)) != null && !set.isEmpty() && set.add(moduleCategory)) {
            applyCategoryFilter(moduleTab847);
        }
    }

    public Set<ModuleCategory> selectedCategories(ModuleTab moduleTab) {
        return moduleTab == null ? Set.of() : Collections.unmodifiableSet((Set) this.selectedCategories.getOrDefault(moduleTab, Set.of()));
    }

    public void toggleCategory(ModuleTab moduleTab, ModuleCategory moduleCategory) {
        if (moduleTab == null || moduleCategory == null || !moduleCategory.supports(moduleTab)) {
            return;
        }
        Set<ModuleCategory> set = this.selectedCategories.get(moduleTab);
        if (set.contains(moduleCategory)) {
            set.remove(moduleCategory);
        } else {
            set.add(moduleCategory);
        }
        applyCategoryFilter(moduleTab);
    }

    public void applyCategoryFilter(ModuleTab moduleTab) {
        MenuTabElement menuTabElement = tabElement(moduleTab);
        if (menuTabElement == null) {
            return;
        }
        menuTabElement.applyCategoryFilter(selectedCategories(moduleTab));
    }

    public void setActive(MenuTabElement menuTabElement, boolean z) {
        if (menuTabElement == null) {
            return;
        }
        menuTabElement.setActive(z);
    }

    public MenuTabElement combat() {
        return this.combat;
    }

    public MenuTabElement render() {
        return this.render;
    }

    public MenuTabElement player() {
        return this.player;
    }

    public MenuTabElement movement() {
        return this.movement;
    }

    public MenuTabElement misc() {
        return this.misc;
    }

    public MenuTabElement theme() {
        return this.theme;
    }

    public MenuTabElement config() {
        return this.config;
    }

    public Map<ModuleTab, MenuTabElement> tabByModuleTab() {
        return this.tabByModuleTab;
    }

    public EnumMap<ModuleTab, Set<ModuleCategory>> selectedCategories() {
        return this.selectedCategories;
    }

    public MenuTabElement current() {
        return this.current;
    }

    public MenuTabElement previousTabElement() {
        return this.previousTabElement;
    }

    public List<MenuTabElement> tabElements() {
        return this.tabElements;
    }
}
