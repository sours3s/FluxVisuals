package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.Lang;
import ru.fluxvisuals.ui.gui.clickgui.ModuleTab;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ModuleCategory {
    ATTACK(Lang.CATEGORY_ATTACK, "/icons/menu/new/tabs/combat.png", ModuleTab.COMBAT),
    DEFENSE(Lang.CATEGORY_DEFENSE, "/icons/menu/new/shield.png", ModuleTab.COMBAT),
    AUTOMATION(Lang.CATEGORY_AUTOMATION, "/icons/menu/new/binary.png", ModuleTab.COMBAT, ModuleTab.PLAYER, ModuleTab.MISC, ModuleTab.MOVEMENT),
    EXPLOITS(Lang.CATEGORY_EXPLOITS, "/icons/menu/new/debug.png", ModuleTab.MOVEMENT, ModuleTab.MISC),
    BOOST(Lang.CATEGORY_BOOST, "/icons/menu/new/bolt.png", ModuleTab.MOVEMENT),
    ANTI_LIMITS(Lang.CATEGORY_ANTI_LIMITS, "/icons/menu/new/cross.png", ModuleTab.MOVEMENT, ModuleTab.PLAYER),
    CONVENIENCE(Lang.CATEGORY_CONVENIENCE, "/icons/menu/new/spark.png", ModuleTab.PLAYER, ModuleTab.MISC, ModuleTab.COMBAT),
    VISUALIZATION(Lang.CATEGORY_VISUALIZATION, "/icons/menu/new/tabs/themes.png", ModuleTab.RENDER),
    INTERFACE(Lang.CATEGORY_INTERFACE, "/icons/menu/new/frame.png", ModuleTab.RENDER),
    WORLD(Lang.CATEGORY_WORLD, "/icons/menu/new/planet.png", ModuleTab.RENDER),
    UTILITIES(Lang.CATEGORY_UTILITIES, "/icons/menu/new/leaf.png", ModuleTab.MISC);

    public final Translation displayName;
    public final String iconPath;
    public GlTexture iconTexture = null;
    public final Set<ModuleTab> moduleTabs;

    ModuleCategory(Translation translation, String str, ModuleTab... moduleTabs847) {
        this.displayName = translation;
        this.iconPath = str;
        this.moduleTabs = Set.of(moduleTabs847);
    }

    public static ModuleCategory forTab(ModuleTab moduleTab) {
        for (ModuleCategory moduleCategory : values()) {
            if (moduleCategory.moduleTabs.contains(moduleTab)) {
                return moduleCategory;
            }
        }
        return UTILITIES;
    }

    public String displayName() {
        return this.displayName.effective();
    }

    public GlTexture icon() {
        if (this.iconTexture == null) {
            this.iconTexture = new GlTexture(new ClasspathResource(this.iconPath));
        }
        return this.iconTexture;
    }

    public boolean supports(ModuleTab moduleTab) {
        return moduleTab != null && this.moduleTabs.contains(moduleTab);
    }

    public static List<ModuleCategory> categoriesForTab(ModuleTab moduleTab) {
        if (moduleTab == null) {
            return Collections.emptyList();
        }
        switch (moduleTab) {
            case COMBAT:
                return List.of(ATTACK, DEFENSE, AUTOMATION, CONVENIENCE);
            case MOVEMENT:
                return List.of(EXPLOITS, BOOST, ANTI_LIMITS, AUTOMATION);
            case PLAYER:
                return List.of(AUTOMATION, CONVENIENCE, ANTI_LIMITS);
            case RENDER:
                return List.of(VISUALIZATION, INTERFACE, WORLD);
            case MISC:
                return List.of(AUTOMATION, UTILITIES, CONVENIENCE, EXPLOITS);
            default:
                return Collections.emptyList();
        }
    }

    public static List<ModuleTab> supportedTabs(ModuleCategory moduleCategory) {
        Stream stream = Arrays.stream(ModuleTab.values());
        Set<ModuleTab> set = moduleCategory.moduleTabs;
        Objects.requireNonNull(set);
        return (List) stream.filter((v1) -> {
            return set.contains(v1);
        }).collect(Collectors.toList());
    }

    public Translation getDisplayName() {
        return this.displayName;
    }

    public String getIconPath() {
        return this.iconPath;
    }

    public Set<ModuleTab> getModuleTabs() {
        return this.moduleTabs;
    }
}
