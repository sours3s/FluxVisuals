package ru.fluxvisuals.ui.gui.clickgui.externall;

import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.module.ModuleManager;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.Render2D;

import java.util.ArrayList;
import java.util.List;

public class Client {
    private static final ModuleManager manager = ModuleManager.get();
    private static final Render2D render2D = new Render2D();
    private static final Client INSTANCE = new Client();

    public static Client get() {
        return INSTANCE;
    }

    public ModuleManager moduleManager() {
        return manager;
    }

    public Module getModule(String name) {
        return manager.get(name);
    }

    public <T extends Module> T get(Class<T> clazz) {
        return manager.get(clazz);
    }

    public List<Module> getModules() {
        return manager.getModules();
    }

    public List<Module> getByCategory(Category category) {
        return manager.getByCategory(category);
    }

    public Render2D render2D() {
        return render2D;
    }
}