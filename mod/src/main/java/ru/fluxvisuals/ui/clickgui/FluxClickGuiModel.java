package ru.fluxvisuals.ui.clickgui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.HueSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

public final class FluxClickGuiModel {

    public record CategoryData(String name, int icon, List<Module> modules) {
        public String description() {
            return switch (name) {
                case "Visuals" -> "Render effects, HUD overlays and visual enhancements.";
                case "Utils" -> "Utility helpers, tools and gameplay tweaks.";
                case "Configs" -> "Manage client profiles and configuration presets.";
                case "Friends" -> "Friend list, friendly player highlighting and protection.";
                case "Cosmetics" -> "Capes, wings and cosmetic accessories.";
                default -> "Module settings and options.";
            };
        }
    }

    public static final class Module {
        public final String name, description;
        public final int icon;
        public final List<Setting> settings;
        public final AnimatedFloat animation;
        public boolean enabled;
        private final ru.fluxvisuals.module.api.Module backing;

        Module(ru.fluxvisuals.module.api.Module backing, List<Setting> settings) {
            this.backing = backing;
            this.name = backing.name;
            this.description = backing.description != null ? backing.description : "";
            this.icon = categoryIcon(backing.category);
            this.enabled = backing.enable;
            this.settings = List.copyOf(settings);
            this.animation = new AnimatedFloat(enabled ? 1 : 0);
        }

        public String id() {
            return backing.name;
        }

        public void toggle() {
            enabled = !enabled;
            backing.setState(enabled);
            animation.setTarget(enabled ? 1 : 0);
        }

        public ru.fluxvisuals.module.api.Module getBacking() {
            return backing;
        }
    }

    public sealed interface Setting permits Toggle, Slider, Choice, Group, ColorValue, Key {
        String name();
        String hint();
        default boolean visible() { return true; }
    }

    public static final class Toggle implements Setting {
        private final BooleanSetting backing;
        public final AnimatedFloat animation;
        public boolean value;

        public Toggle(BooleanSetting backing) {
            this.backing = backing;
            this.value = backing.get();
            this.animation = new AnimatedFloat(value ? 1 : 0);
        }

        public String name() { return backing.name != null ? backing.name : ""; }
        public String hint() { return backing.description != null ? backing.description : ""; }
        public boolean visible() { return backing.hidden == null || !backing.hidden.get(); }

        public void toggle() {
            value = !value;
            backing.set(value);
            animation.setTarget(value ? 1 : 0);
        }
    }

    public static final class Slider implements Setting {
        private final SliderSetting backing;
        public final SliderModel model;

        public Slider(SliderSetting backing) {
            this.backing = backing;
            this.model = new SliderModel(backing.minimum, backing.maximum, backing.current);
        }

        public String name() { return backing.name != null ? backing.name : ""; }
        public String hint() { return backing.description != null ? backing.description : ""; }
        public boolean visible() { return backing.hidden == null || !backing.hidden.get(); }

        public String value() {
            float v = model.value();
            String formatted = Math.abs(v - Math.round(v)) < 0.01f
                    ? Integer.toString(Math.round(v))
                    : String.format(java.util.Locale.ROOT, "%.1f", v);
            return backing.percent ? formatted + "%" : formatted;
        }

        public void sync() {
            backing.current = model.value();
        }
    }

    public static final class Choice implements Setting {
        private final ModeSetting backing;
        public final List<String> values;
        public int selected;

        public Choice(ModeSetting backing) {
            this.backing = backing;
            this.values = backing.modes != null ? backing.modes : List.of();
            this.selected = Math.max(0, this.values.indexOf(backing.get()));
        }

        public String name() { return backing.name != null ? backing.name : ""; }
        public String hint() { return backing.description != null ? backing.description : ""; }
        public boolean visible() { return backing.hidden == null || !backing.hidden.get(); }
        public String value() { return values.isEmpty() ? "" : values.get(selected); }

        public void next() {
            if (!values.isEmpty()) {
                select((selected + 1) % values.size());
            }
        }

        public void select(int index) {
            if (values.isEmpty()) return;
            selected = Math.max(0, Math.min(values.size() - 1, index));
            backing.currentMode = value();
        }
    }

    public static final class Group implements Setting {
        private final MultiBooleanSetting backing;
        public final List<Toggle> values;

        public Group(MultiBooleanSetting backing) {
            this.backing = backing;
            List<Toggle> list = new ArrayList<>();
            if (backing.settings != null) {
                for (BooleanSetting b : backing.settings) {
                    list.add(new Toggle(b));
                }
            }
            this.values = List.copyOf(list);
        }

        public String name() { return backing.name != null ? backing.name : ""; }
        public String hint() { return ""; }
        public boolean visible() { return backing.hidden == null || !backing.hidden.get(); }

        public String summary() {
            long enabled = values.stream().filter(v -> v.value).count();
            return enabled + "/" + values.size() + " enabled";
        }
    }

    public static final class ColorValue implements Setting {
        private final HueSetting backing;

        public ColorValue(HueSetting backing) {
            this.backing = backing;
        }

        public String name() { return backing.name != null ? backing.name : ""; }
        public String hint() { return backing.description != null ? backing.description : ""; }
        public boolean visible() { return backing.hidden == null || !backing.hidden.get(); }

        public int argb() {
            Color c = backing.getColor();
            return 0xFF000000 | (c.getRGB() & 0xFFFFFF);
        }

        public String value() {
            return String.format(java.util.Locale.ROOT, "#%06X", backing.getColor().getRGB() & 0xFFFFFF);
        }

        public float hue() { return backing.getHue(); }
        public float saturation() { return backing.saturation; }
        public float brightness() { return backing.brightness; }
        public float alpha() { return 1.0f; }

        public void setHsb(float hue, float saturation, float brightness) {
            backing.saturation = saturation;
            backing.brightness = brightness;
            backing.current = hue * backing.maximum;
        }

        public void setAlpha(float alpha) {}
    }

    public record Key(String name, String hint, String key) implements Setting {}

    public static List<CategoryData> create() {
        List<CategoryData> list = new ArrayList<>();
        if (FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null) {
            for (Category cat : Category.values()) {
                List<Module> mods = new ArrayList<>();
                for (ru.fluxvisuals.module.api.Module m : FluxVisualsClient.get.manager.getType(cat)) {
                    mods.add(live(m));
                }
                list.add(new CategoryData(cat.name(), categoryIcon(cat), mods));
            }
        }
        if (list.isEmpty()) {
            list.add(new CategoryData("Visuals", 0xE3A6, List.of()));
            list.add(new CategoryData("Utils", 0xE5D2, List.of()));
            list.add(new CategoryData("Configs", 0xE871, List.of()));
            list.add(new CategoryData("Friends", 0xE7FD, List.of()));
            list.add(new CategoryData("Cosmetics", 0xE001, List.of()));
        }
        return list;
    }

    private static Module live(ru.fluxvisuals.module.api.Module core) {
        List<Setting> settings = new ArrayList<>();
        if (core.getSettings() != null) {
            for (ru.fluxvisuals.module.api.setting.Setting setting : core.getSettings()) {
                if (setting instanceof BooleanSetting value) {
                    settings.add(new Toggle(value));
                } else if (setting instanceof SliderSetting value) {
                    settings.add(new Slider(value));
                } else if (setting instanceof ModeSetting value) {
                    settings.add(new Choice(value));
                } else if (setting instanceof MultiBooleanSetting value) {
                    settings.add(new Group(value));
                } else if (setting instanceof HueSetting value) {
                    settings.add(new ColorValue(value));
                }
            }
        }
        return new Module(core, settings);
    }

    private static int categoryIcon(Category category) {
        if (category == null) return 0xE8B8;
        return switch (category) {
            case Visuals -> 0xE3A6;
            case Utils -> 0xE5D2;
            case Configs -> 0xE871;
            case Friends -> 0xE7FD;
            case Cosmetics -> 0xE001;
        };
    }

    private FluxClickGuiModel() {}
}
