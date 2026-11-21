package ru.fluxvisuals.ui.gui.clickgui.config;
import ru.fluxvisuals.ui.gui.clickgui.ClickGuiModule;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModuleRepository {
    public final Map<Class<? extends ClickGuiModule>, ClickGuiModule> moduleMap;

    public ModuleRepository(List<Supplier<ClickGuiModule>> list) {
        this.moduleMap = (Map) list.stream().map((v0) -> {
            return v0.get();
        }).collect(Collectors.toMap((v0) -> {
            return v0.getClass();
        }, Function.identity(), (module, class605Var2) -> {
            return module;
        }, HashMap::new));
    }

    public Collection<ClickGuiModule> getModules() {
        return Collections.unmodifiableCollection(this.moduleMap.values());
    }

    public <T extends ClickGuiModule> T get(Class<T> cls) {
        ClickGuiModule module = this.moduleMap.get(cls);
        if (module == null) {
            throw new NoSuchElementException("Module not found: " + cls.getName());
        }
        return cls.cast(module);
    }

    public <T extends ClickGuiModule> Optional<T> find(Class<T> cls) {
        Optional optionalOfNullable = Optional.ofNullable(this.moduleMap.get(cls));
        Objects.requireNonNull(cls);
        return optionalOfNullable.map((v1) -> {
            return cls.cast(v1);
        });
    }

    public Map<Class<? extends ClickGuiModule>, ClickGuiModule> getModuleMap() {
        return this.moduleMap;
    }
}
