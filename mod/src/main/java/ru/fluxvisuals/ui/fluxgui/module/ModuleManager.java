package ru.fluxvisuals.ui.fluxgui.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Представление модулей FluxVisuals для ClickGUI. */
public final class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();
    private final Map<Category, List<Module>> byCategory = new HashMap<>();

    private ModuleManager() {
    }

    /** Пересобирает список из активного менеджера модулей клиента. */
    public void sync() {
        modules.clear();
        byCategory.clear();
        ru.fluxvisuals.client.FluxVisualsClient client = ru.fluxvisuals.client.FluxVisualsClient.get;
        if (client == null || client.manager == null) {
            return;
        }
        for (ru.fluxvisuals.module.api.Module raw : client.manager.module) {
            Module module = new Module(raw);
            modules.add(module);
            byCategory.computeIfAbsent(module.getCategory(), c -> new ArrayList<>()).add(module);
        }
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        return byCategory.getOrDefault(category, List.of());
    }

    public <T> T getModule(Class<T> type) {
        if (type == ru.fluxvisuals.ui.fluxgui.module.render.Animations.class) {
            return type.cast(ru.fluxvisuals.ui.fluxgui.module.render.Animations.INSTANCE);
        }
        if (type == ru.fluxvisuals.ui.fluxgui.module.render.Cooldowns.class) {
            return type.cast(ru.fluxvisuals.ui.fluxgui.module.render.Cooldowns.INSTANCE);
        }
        return null;
    }

    public Module getModule(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }
}
