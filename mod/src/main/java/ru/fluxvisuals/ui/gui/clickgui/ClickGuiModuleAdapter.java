package ru.fluxvisuals.ui.gui.clickgui;

import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BindSettings;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ButtonSetting;
import ru.fluxvisuals.module.api.setting.impl.HueSetting;
import ru.fluxvisuals.module.api.setting.impl.ListSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.module.api.setting.impl.StringSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.ColorSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.ExpandableSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.KeybindSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.LabelMultiSelectSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.LabelSelectSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.NumberSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.TextFieldSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.type.BindMode;
import ru.fluxvisuals.ui.gui.clickgui.util.type.SettingUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClickGuiModuleAdapter implements ClickGuiModule {
    private final Module module;

    public ClickGuiModuleAdapter(Module module) {
        this.module = module;
    }

    public String getName() {
        return module.getDisplayName() != null ? module.getDisplayName() : module.name;
    }

    public List<Integer> getKeyBind() {
        List<Integer> keys = new ArrayList<>();
        if (module.bind > 0) {
            keys.add(module.bind);
        }
        return keys;
    }

    public void setKey(List<Integer> keys) {
        if (keys != null && !keys.isEmpty()) {
            module.bind = keys.get(0);
        } else {
            module.bind = -1;
        }
    }

    public void setBind(List<Integer> keys, BindMode type) {
        setKey(keys);
    }

    public BindMode getType() {
        return BindMode.TOGGLE;
    }

    public void setType(BindMode type) {
        // BindMode is not used in user's module system
    }

    public boolean isState() {
        return module.enable;
    }

    public void setState(boolean enable) {
        module.setState(enable);
    }

    public void setStateSilent(boolean enable) {
        module.enable = enable;
        if (enable) {
            module.onEnable();
        } else {
            module.onDisable();
        }
    }

    public void switchState() {
        module.toggle();
    }

    public List<ru.fluxvisuals.ui.gui.clickgui.setting.Setting> getSettings() {
        return convertSettings(module.getSettings());
    }

    public ModuleTab getModuleTab() {
        return mapCategoryToTab(module.category);
    }

    private ModuleTab mapCategoryToTab(Category category) {
        if (category == null) {
            return ModuleTab.MISC;
        }
        return switch (category) {
            case Visuals -> ModuleTab.RENDER;
            case Utils -> ModuleTab.MISC;
            case Configs -> ModuleTab.CONFIGS;
            case Friends -> ModuleTab.MISC;
            case Cosmetics -> ModuleTab.RENDER;
            default -> ModuleTab.MISC;
        };
    }

    private List<ru.fluxvisuals.ui.gui.clickgui.setting.Setting> convertSettings(List<Setting> settings) {
        List<ru.fluxvisuals.ui.gui.clickgui.setting.Setting> result = new ArrayList<>();
        for (Setting setting : settings) {
            ru.fluxvisuals.ui.gui.clickgui.setting.Setting converted = convertSetting(setting);
            if (converted != null) {
                result.add(converted);
            }
        }
        return result;
    }

    private ru.fluxvisuals.ui.gui.clickgui.setting.Setting convertSetting(Setting setting) {
        String name = setting.name;
        if (name == null) return null;

        Translation translationName = Translation.clearText(name);
        String desc = setting instanceof BooleanSetting bs && bs.description != null ? bs.description : "";
        Translation translationDesc = Translation.clearText(desc);

        if (setting instanceof BooleanSetting bs) {
            ru.fluxvisuals.ui.gui.clickgui.setting.BooleanSetting clickGuiSetting = new ru.fluxvisuals.ui.gui.clickgui.setting.BooleanSetting(translationName, translationDesc);
            clickGuiSetting.setVisible(() -> !setting.hidden.get());
            clickGuiSetting.setValue(bs.get());
            return clickGuiSetting;
        } else if (setting instanceof ModeSetting ms) {
            // Use LabelSelectSetting for string-based modes
            Translation[] modeTranslations = new Translation[ms.modes.size()];
            for (int i = 0; i < ms.modes.size(); i++) {
                modeTranslations[i] = Translation.clearText(ms.modes.get(i));
            }
            LabelSelectSetting clickGuiSetting = new LabelSelectSetting(translationName, translationDesc);
            clickGuiSetting.setVisible(() -> !setting.hidden.get());
            clickGuiSetting.value(modeTranslations);
            // Find current mode index
            int currentIndex = ms.modes.indexOf(ms.currentMode);
            if (currentIndex >= 0) {
                clickGuiSetting.selected(currentIndex);
            }
            return clickGuiSetting;
        } else if (setting instanceof SliderSetting ss) {
            NumberSetting clickGuiSetting = new NumberSetting(translationName, translationDesc);
            clickGuiSetting.setVisible(() -> !setting.hidden.get());
            clickGuiSetting.min(ss.minimum);
            clickGuiSetting.max(ss.maximum);
            clickGuiSetting.step(ss.increment);
            clickGuiSetting.currentValue(ss.current);
            clickGuiSetting.unit(ss.percent ? SettingUnit.PERCENTS : SettingUnit.UNITS);
            return clickGuiSetting;
        } else if (setting instanceof MultiBooleanSetting mbs) {
            ExpandableSetting clickGuiSetting = new ExpandableSetting(translationName, translationDesc);
            clickGuiSetting.setVisible(() -> !setting.hidden.get());
            for (ru.fluxvisuals.module.api.setting.impl.BooleanSetting subSetting : mbs.settings) {
                ru.fluxvisuals.ui.gui.clickgui.setting.BooleanSetting subClickGui = new ru.fluxvisuals.ui.gui.clickgui.setting.BooleanSetting(
                        Translation.clearText(subSetting.name),
                        Translation.clearText(subSetting.description != null ? subSetting.description : "")
                );
                subClickGui.setVisible(() -> !subSetting.hidden.get());
                subClickGui.setValue(subSetting.get());
                clickGuiSetting.serializableChildren.add(subClickGui);
            }
            return clickGuiSetting;
        } else if (setting instanceof BindSettings bs) {
            KeybindSetting clickGuiSetting = new KeybindSetting(translationName, translationDesc);
            clickGuiSetting.setVisible(() -> !setting.hidden.get());
            clickGuiSetting.setKey(bs.get());
            return clickGuiSetting;
        } else if (setting instanceof StringSetting ss) {
            TextFieldSetting clickGuiSetting = new TextFieldSetting(translationName, translationDesc);
            clickGuiSetting.setVisible(() -> !setting.hidden.get());
            return clickGuiSetting;
        } else if (setting instanceof HueSetting hs) {
            ColorSetting clickGuiSetting = new ColorSetting(translationName, translationDesc);
            clickGuiSetting.setVisible(() -> !setting.hidden.get());
            clickGuiSetting.setColor(hs.getRGB());
            return clickGuiSetting;
        } else if (setting instanceof ListSetting ls) {
            // Map ListSetting to LabelMultiSelectSetting for string-based multi-select
            Translation[] optionTranslations = new Translation[ls.list.size()];
            for (int i = 0; i < ls.list.size(); i++) {
                optionTranslations[i] = Translation.clearText(ls.list.get(i));
            }
            LabelMultiSelectSetting clickGuiSetting = new LabelMultiSelectSetting(translationName, translationDesc);
            clickGuiSetting.setVisible(() -> !setting.hidden.get());
            clickGuiSetting.value(optionTranslations);
            // Set selected values
            List<Translation> selectedTranslations = new ArrayList<>();
            for (String selected : ls.selected) {
                for (Translation opt : optionTranslations) {
                    if (opt.original().equals(selected)) {
                        selectedTranslations.add(opt);
                        break;
                    }
                }
            }
            clickGuiSetting.setSelected(selectedTranslations);
            return clickGuiSetting;
        } else if (setting instanceof ButtonSetting bs) {
            ru.fluxvisuals.ui.gui.clickgui.setting.ButtonSetting clickGuiSetting = new ru.fluxvisuals.ui.gui.clickgui.setting.ButtonSetting(translationName, translationDesc);
            clickGuiSetting.setVisible(() -> !setting.hidden.get());
            clickGuiSetting.setButtonName(Translation.clearText(bs.description != null ? bs.description : ""));
            // Note: Runnable from user's setting cannot be directly transferred
            return clickGuiSetting;
        }
        return null;
    }
}