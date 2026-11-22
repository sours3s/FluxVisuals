package ru.fluxvisuals.ui.fluxgui.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import ru.fluxvisuals.ui.fluxgui.setting.ActionSetting;
import ru.fluxvisuals.ui.fluxgui.setting.BindSetting;
import ru.fluxvisuals.ui.fluxgui.setting.BooleanSetting;
import ru.fluxvisuals.ui.fluxgui.setting.ColorSetting;
import ru.fluxvisuals.ui.fluxgui.setting.LabelSetting;
import ru.fluxvisuals.ui.fluxgui.setting.ModeSetting;
import ru.fluxvisuals.ui.fluxgui.setting.NumberSetting;
import ru.fluxvisuals.ui.fluxgui.setting.SeparatorSetting;
import ru.fluxvisuals.ui.fluxgui.setting.Setting;
import ru.fluxvisuals.ui.fluxgui.setting.StringSetting;

/** Адаптер между модулями FluxVisuals и рендером нового ClickGUI. */
public class Module {
    private final ru.fluxvisuals.module.api.Module delegate;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean binding;

    public Module(ru.fluxvisuals.module.api.Module delegate) {
        this.delegate = delegate;
        this.name = delegate == null ? "Macros" : (delegate.name != null ? delegate.name : delegate.getClass().getSimpleName());
        this.description = delegate != null && delegate.description != null ? delegate.description : "";
        this.category = delegate == null ? Category.UTILITY : mapCategory(delegate.category);
        if (delegate == null) {
            return;
        }
        for (ru.fluxvisuals.module.api.setting.Setting raw : delegate.getSettings()) {
            Setting<?> converted = convert(raw);
            if (converted != null) {
                this.settings.add(converted);
            }
        }
    }

    private static Category mapCategory(ru.fluxvisuals.module.api.Category category) {
        if (category == null) {
            return Category.VISUALS;
        }
        return switch (category) {
            case Visuals -> Category.VISUALS;
            case Utils -> Category.UTILITY;
            case Configs -> Category.CONFIGS;
            case Friends -> Category.FRIENDS;
            case Cosmetics -> Category.COSMETICS;
        };
    }

    private Setting<?> convert(ru.fluxvisuals.module.api.setting.Setting raw) {
        Supplier<Boolean> visible = () -> !raw.hidden.get();
        if (raw instanceof ru.fluxvisuals.module.api.setting.impl.BooleanSetting s) {
            return new BooleanSetting(s.name, s.get(), visible) {
                @Override
                public Boolean getValue() {
                    return s.get();
                }

                @Override
                public void setValue(Boolean value) {
                    s.set(value);
                }

                @Override
                public void toggle() {
                    s.set(!s.get());
                }
            };
        }
        if (raw instanceof ru.fluxvisuals.module.api.setting.impl.SliderSetting s) {
            return new NumberSetting(s.name, s.current, s.minimum, s.maximum,
                s.increment > 0 ? s.increment : 0.01, visible) {
                @Override
                public Double getValue() {
                    return (double) s.current;
                }

                @Override
                public void setValue(Double value) {
                    s.set(value.floatValue());
                }
            };
        }
        if (raw instanceof ru.fluxvisuals.module.api.setting.impl.HueSetting s) {
            return new NumberSetting(s.name, s.current, s.minimum, s.maximum,
                s.increment > 0 ? s.increment : 1.0, visible) {
                @Override
                public Double getValue() {
                    return (double) s.current;
                }

                @Override
                public void setValue(Double value) {
                    s.current = value.floatValue();
                }
            };
        }
        if (raw instanceof ru.fluxvisuals.module.api.setting.impl.ModeSetting s) {
            return new ModeSetting(s.name, s.currentMode, visible, s.modes.toArray(new String[0])) {
                @Override
                public String getValue() {
                    return s.get();
                }

                @Override
                public void setValue(String mode) {
                    s.currentMode = mode;
                    int i = s.modes.indexOf(mode);
                    if (i >= 0) {
                        s.index = i;
                    }
                }
            };
        }
        if (raw instanceof ru.fluxvisuals.module.api.setting.impl.StringSetting s) {
            return new StringSetting(s.name, s.input, visible) {
                @Override
                public String getValue() {
                    return s.get();
                }

                @Override
                public void setValue(String value) {
                    s.set(value);
                }
            };
        }
        if (raw instanceof ru.fluxvisuals.module.api.setting.impl.BindSettings s) {
            return new BindSetting(s.name, s.key, visible) {
                @Override
                public Integer getValue() {
                    return s.key;
                }

                @Override
                public void setValue(Integer value) {
                    s.key = value;
                }
            };
        }
        if (raw instanceof ru.fluxvisuals.module.api.setting.impl.ButtonSetting s) {
            return new ActionSetting(s.name, () -> s.mode = s.mode + 1, visible);
        }
        if (raw instanceof ru.fluxvisuals.module.api.setting.impl.NoneSetting s) {
            return new LabelSetting(s.name != null && !s.name.isBlank() ? s.name : "");
        }
        if (raw instanceof ru.fluxvisuals.module.api.setting.impl.ListSetting s) {
            return new LabelSetting(s.name + ": " + s.getFormattedList());
        }
        if (raw instanceof ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting s) {
            return new LabelSetting(s.name + ": " + String.join(", ", s.settings.stream().map(x -> x.name).toList()));
        }
        String label = raw.name != null && !raw.name.isBlank() ? raw.name : "Настройка";
        return new LabelSetting(label);
    }

    public ru.fluxvisuals.module.api.Module getDelegate() {
        return delegate;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return delegate.enable;
    }

    public void toggle() {
        delegate.toggle();
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public boolean isBinding() {
        return binding || delegate.binding;
    }

    public void setBinding(boolean binding) {
        this.binding = binding;
        delegate.binding = binding;
    }

    public int getKey() {
        return delegate.bind;
    }

    public void setKey(int key) {
        delegate.bind = key;
        // Кеш биндов строится только при создании менеджера, поэтому без его
        // пересборки новый код клавиши не срабатывает, хотя GUI показывает бинд.
        try {
            if (ru.fluxvisuals.client.FluxVisualsClient.get != null) {
                if (ru.fluxvisuals.client.FluxVisualsClient.get.manager != null) {
                    ru.fluxvisuals.client.FluxVisualsClient.get.manager.invalidateCaches();
                }
                if (ru.fluxvisuals.client.FluxVisualsClient.get.configManager != null) {
                    ru.fluxvisuals.client.FluxVisualsClient.get.configManager.autoSave();
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    public String getConfigKey() {
        return delegate.getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Module m && m.delegate == delegate;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(delegate);
    }
}
