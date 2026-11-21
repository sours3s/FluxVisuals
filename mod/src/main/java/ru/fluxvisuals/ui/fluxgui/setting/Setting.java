package ru.fluxvisuals.ui.fluxgui.setting;

import ru.fluxvisuals.ui.fluxgui.util.language.LanguageManager;
import java.util.function.Supplier;

public abstract class Setting<T> {
    private final String name;
    private String displayName;
    private Supplier<Boolean> visibility;
    protected T value;

    public Setting(String name, T value) {
        this(name, value, () -> true);
    }

    public Setting(String name, T value, Supplier<Boolean> visibility) {
        this.name = name;
        this.displayName = name;
        this.value = value;
        this.visibility = visibility;
    }

    public void setDependency(Supplier<Boolean> dependency) {
        this.visibility = dependency;
    }

    public String getName() {
        return LanguageManager.repairIrcMojibake(this.name);
    }

    public String getConfigKey() {
        return this.getName();
    }

    public String getDisplayName() {
        return LanguageManager.repairIrcMojibake(this.displayName);
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean isVisible() {
        return this.visibility.get();
    }
}
