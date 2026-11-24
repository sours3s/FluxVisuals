package ru.fluxvisuals.ui.gui.clickgui.config;
import ru.fluxvisuals.ui.gui.clickgui.util.type.BindMode;
import ru.fluxvisuals.ui.gui.clickgui.setting.BooleanSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.ColorSetting;
import ru.fluxvisuals.ui.gui.clickgui.Draggable;
import ru.fluxvisuals.ui.gui.clickgui.setting.EnumSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.ExpandableSetting;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.setting.KeybindSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.LabelMultiSelectSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.LabelSelectSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutNode;
import ru.fluxvisuals.ui.gui.clickgui.setting.ModeSetting;
import ru.fluxvisuals.ui.gui.clickgui.ClickGuiModule;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ModuleConfigData;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ModuleConfigEntry;
import ru.fluxvisuals.ui.gui.clickgui.setting.MultiSelectSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.NumberSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.OrderedEnumSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;
import ru.fluxvisuals.ui.gui.clickgui.setting.TextFieldSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.WidgetStack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.SwitchBootstraps;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class ConfigSerializer {
    public final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public byte[] serialize(ModuleRepository moduleRepository, WidgetStack widgetStack) throws IOException {
        return this.gson.toJson(buildConfigFile(moduleRepository, widgetStack)).getBytes(StandardCharsets.UTF_8);
    }

    public ConfigFile deserialize(byte[] bArr) throws IOException {
        return (ConfigFile) this.gson.fromJson(new String(bArr, StandardCharsets.UTF_8), ConfigFile.class);
    }

    public Optional<ConfigFile> tryDeserialize(byte[] bArr) {
        try {
            return Optional.of(deserialize(bArr));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void applyConfig(LoadedConfig loadedConfig) throws IOException {
        ModuleRepository moduleRepository795 = FluxVisuals.INSTANCE.moduleRepository();
        WidgetStack widgetStack814 = FluxVisuals.INSTANCE.widgetStack();
        ConfigFile configFile = loadedConfig.bundle();
        HashMap map = new HashMap();
        for (ClickGuiModule module : moduleRepository795.getModules()) {
            map.put(module.getName(), module);
        }
        if (configFile.modules() != null) {
            for (ModuleConfigData moduleConfigData : configFile.modules()) {
                ClickGuiModule class605Var2 = (ClickGuiModule) map.get(moduleConfigData.name());
                if (class605Var2 != null) {
                    boolean enabled = "Discord RPC".equals(moduleConfigData.name()) || moduleConfigData.enabled();
                    class605Var2.setStateSilent(enabled);
                    class605Var2.setBind(moduleConfigData.keys(), moduleConfigData.bindType() != null ? moduleConfigData.bindType() : class605Var2.getType());
                    if (moduleConfigData.settings() != null) {
                        Iterator<LayoutNode> it = moduleConfigData.settings().iterator();
                        while (it.hasNext()) {
                            applySettingNode(class605Var2.getSettings(), it.next());
                        }
                    }
                }
            }
        }
        if (configFile.widgets() != null) {
            Iterator<ModuleConfigEntry> it2 = configFile.widgets().iterator();
            while (it2.hasNext()) {
                applyWidgetEntry(widgetStack814, it2.next());
            }
        }
    }

    public void applySettingNode(List<Setting> list, LayoutNode layoutNode) {
        if (layoutNode == null || layoutNode.name() == null) {
            return;
        }
        list.stream().filter((v0) -> {
            return Objects.nonNull(v0);
        }).filter(setting -> {
            return (setting.getName() == null || setting.getName().original() == null) ? false : true;
        }).filter(class661Var2 -> {
            return class661Var2.getName().original().equalsIgnoreCase(layoutNode.name());
        }).findFirst().ifPresent(class661Var3 -> {
            applySettingData(class661Var3, layoutNode);
        });
    }

    public void applyWidgetEntry(WidgetStack widgetStack, ModuleConfigEntry moduleConfigEntry) {
        String strTrim = moduleConfigEntry.name() == null ? "" : moduleConfigEntry.name().trim();
        widgetStack.widgets().stream().filter((v0) -> {
            return Objects.nonNull(v0);
        }).filter(draggable -> {
            return draggable.getName() != null;
        }).filter(class806Var2 -> {
            return class806Var2.getName().equalsIgnoreCase(strTrim);
        }).findFirst().ifPresent(class806Var3 -> {
            if (moduleConfigEntry.settings() != null) {
                Iterator<LayoutNode> it = moduleConfigEntry.settings().iterator();
                while (it.hasNext()) {
                    applySettingNode(class806Var3.getSettings(), it.next());
                }
            }
            class806Var3.applySavedPosition(moduleConfigEntry.x(), moduleConfigEntry.y());
        });
    }

    public void applySettingData(Setting setting, LayoutNode layoutNode) {
        int iIntValue;
        if (setting instanceof ExpandableSetting) {
            ExpandableSetting expandableSetting = (ExpandableSetting) setting;
            if (layoutNode.children() != null) {
                for (LayoutNode class301Var2 : layoutNode.children()) {
                    expandableSetting.getSubSettings().stream().filter(class661Var2 -> {
                        return class661Var2.getName().original().equalsIgnoreCase(class301Var2.name());
                    }).findFirst().ifPresent(class661Var3 -> {
                        applySettingData(class661Var3, class301Var2);
                    });
                }
            }
            Object obj = layoutNode.data().get("value");
            if (obj instanceof Boolean) {
                expandableSetting.setValue(((Boolean) obj).booleanValue());
            }
            Object obj2 = layoutNode.data().get("keys");
            if (obj2 instanceof List) {
                Stream stream = ((List) obj2).stream();
                Class<Number> cls = Number.class;
                Objects.requireNonNull(Number.class);
                Stream streamFilter = stream.filter(cls::isInstance);
                Class<Number> cls2 = Number.class;
                Objects.requireNonNull(Number.class);
                expandableSetting.setKey(streamFilter.map(cls2::cast).map((v0) -> {
                    return ((Number) v0).intValue();
                }).toList());
            } else {
                Object obj3 = layoutNode.data().get("key");
                if (obj3 instanceof Number) {
                    expandableSetting.setKey(((Number) obj3).intValue());
                }
            }
            Object obj4 = layoutNode.data().get("bindType");
            if (obj4 instanceof String) {
                expandableSetting.setType(BindMode.valueOf((String) obj4));
                return;
            }
            return;
        }
        if (setting instanceof KeybindSetting) {
            KeybindSetting keybindSetting = (KeybindSetting) setting;
            Object obj5 = layoutNode.data().get("keys");
            if (obj5 instanceof List) {
                Stream stream2 = ((List) obj5).stream();
                Class<Number> cls3 = Number.class;
                Objects.requireNonNull(Number.class);
                Stream streamFilter2 = stream2.filter(cls3::isInstance);
                Class<Number> cls4 = Number.class;
                Objects.requireNonNull(Number.class);
                keybindSetting.setKey(streamFilter2.map(cls4::cast).map((v0) -> {
                    return ((Number) v0).intValue();
                }).toList());
            } else {
                Object obj6 = layoutNode.data().get("key");
                if (obj6 instanceof Number) {
                    keybindSetting.setKey(((Number) obj6).intValue());
                }
            }
            Object obj7 = layoutNode.data().get("bindType");
            if (obj7 instanceof String) {
                keybindSetting.setType(BindMode.valueOf((String) obj7));
                return;
            }
            return;
        }
        if (setting instanceof BooleanSetting) {
            BooleanSetting booleanSetting = (BooleanSetting) setting;
            Object obj8 = layoutNode.data().get("value");
            if (obj8 instanceof Boolean) {
                booleanSetting.setValue(((Boolean) obj8).booleanValue());
            }
            Object obj9 = layoutNode.data().get("key");
            if (obj9 instanceof Number) {
                booleanSetting.setKey(((Number) obj9).intValue());
            }
            Object obj10 = layoutNode.data().get("bindType");
            if (obj10 instanceof String) {
                booleanSetting.setType(BindMode.valueOf((String) obj10));
                return;
            }
            return;
        }
        if (setting instanceof ModeSetting) {
            ModeSetting modeSetting = (ModeSetting) setting;
            Object obj11 = layoutNode.data().get("value");
            if (obj11 instanceof String) {
                try {
                    modeSetting.select(Enum.valueOf(modeSetting.currentValue().getDeclaringClass(), (String) obj11));
                    return;
                } catch (IllegalArgumentException e) {
                    return;
                }
            }
            return;
        }
        if (setting instanceof EnumSetting) {
            EnumSetting enumSetting = (EnumSetting) setting;
            Object obj12 = layoutNode.data().get("value");
            if (obj12 instanceof String) {
                try {
                    enumSetting.select(Enum.valueOf(enumSetting.currentValue().getDeclaringClass(), (String) obj12));
                    return;
                } catch (IllegalArgumentException e2) {
                    return;
                }
            }
            return;
        }
        if (setting instanceof MultiSelectSetting) {
            MultiSelectSetting multiSelectSetting = (MultiSelectSetting) setting;
            Object obj13 = layoutNode.data().get("selected");
            if (obj13 instanceof List) {
                List list = (List) obj13;
                multiSelectSetting.selectedValues().clear();
                Class declaringClass = multiSelectSetting.options()[0].getDeclaringClass();
                for (Object obj14 : list) {
                    if (obj14 instanceof String) {
                        try {
                            multiSelectSetting.selectedValues().add(Enum.valueOf(declaringClass, (String) obj14));
                        } catch (IllegalArgumentException e3) {
                        }
                    }
                }
                return;
            }
            return;
        }
        if (setting instanceof OrderedEnumSetting) {
            OrderedEnumSetting orderedEnumSetting = (OrderedEnumSetting) setting;
            Class declaringClass2 = orderedEnumSetting.options()[0].getDeclaringClass();
            Object obj15 = layoutNode.data().get("selected");
            if (obj15 instanceof List) {
                getSelectedEnums(orderedEnumSetting).clear();
                for (Object obj16 : (List) obj15) {
                    if (obj16 instanceof String) {
                        getSelectedEnums(orderedEnumSetting).add(Enum.valueOf(declaringClass2, (String) obj16));
                    }
                }
            }
            Object obj17 = layoutNode.data().get("order");
            if (obj17 instanceof List) {
                orderedEnumSetting.order().clear();
                for (Object obj18 : (List) obj17) {
                    if (obj18 instanceof String) {
                        orderedEnumSetting.order().add(Enum.valueOf(declaringClass2, (String) obj18));
                    }
                }
                return;
            }
            return;
        }
        if (setting instanceof ColorSetting) {
            ColorSetting colorSetting = (ColorSetting) setting;
            Object obj19 = layoutNode.data().get("color");
            if (obj19 instanceof Number) {
                colorSetting.setColor(((Number) obj19).intValue());
                return;
            }
            return;
        }
        if (setting instanceof NumberSetting) {
            NumberSetting numberSetting = (NumberSetting) setting;
            Object obj20 = layoutNode.data().get("value");
            if (obj20 instanceof Number) {
                numberSetting.currentValue(((Number) obj20).floatValue());
                return;
            }
            return;
        }
        if (setting instanceof TextFieldSetting) {
            TextFieldSetting textFieldSetting = (TextFieldSetting) setting;
            Object obj21 = layoutNode.data().get("text");
            if (obj21 instanceof String) {
                textFieldSetting.setText((String) obj21);
                return;
            }
            return;
        }
        if (setting instanceof LabelSelectSetting) {
            LabelSelectSetting labelSelectSetting = (LabelSelectSetting) setting;
            Object obj22 = layoutNode.data().get("index");
            if (obj22 instanceof Number) {
                labelSelectSetting.selected(((Number) obj22).intValue());
                return;
            }
            return;
        }
        if (setting instanceof LabelMultiSelectSetting) {
            LabelMultiSelectSetting labelMultiSelectSetting = (LabelMultiSelectSetting) setting;
            Object obj23 = layoutNode.data().get("indexes");
            if (obj23 instanceof List) {
                labelMultiSelectSetting.getSelected().clear();
                for (Object obj24 : (List) obj23) {
                    if ((obj24 instanceof Number) && (iIntValue = ((Number) obj24).intValue()) >= 0 && iIntValue < labelMultiSelectSetting.getList().size()) {
                        labelMultiSelectSetting.getSelected().add(labelMultiSelectSetting.getList().get(iIntValue));
                    }
                }
            }
        }
    }

    public ConfigFile buildConfigFile(ModuleRepository moduleRepository, WidgetStack widgetStack) {
        return new ConfigFile().version(1).modules(serializeModules(moduleRepository)).widgets(serializeWidgets(widgetStack));
    }

    public List<ModuleConfigData> serializeModules(ModuleRepository moduleRepository) {
        ArrayList arrayList = new ArrayList();
        for (ClickGuiModule module : moduleRepository.getModules()) {
            boolean enabled = "Discord RPC".equals(module.getName()) || module.isState();
            ModuleConfigData moduleConfigData = new ModuleConfigData().name(module.getName()).enabled(enabled).keys(new ArrayList(module.getKeyBind())).bindType(module.getType());
            for (Setting setting : module.getSettings()) {
                if (setting != null && setting.getName() != null && setting.getName().original() != null) {
                    moduleConfigData.settings().add(serializeSetting(setting));
                }
            }
            arrayList.add(moduleConfigData);
        }
        return arrayList;
    }

    public List<ModuleConfigEntry> serializeWidgets(WidgetStack widgetStack) {
        ArrayList arrayList = new ArrayList();
        for (Draggable draggable : widgetStack.widgets()) {
            ModuleConfigEntry moduleConfigEntry = new ModuleConfigEntry().name(draggable.getName()).x(draggable.getX()).y(draggable.getY());
            for (Setting setting : draggable.getSettings()) {
                if (setting != null && setting.getName() != null && setting.getName().original() != null) {
                    moduleConfigEntry.settings().add(serializeSetting(setting));
                }
            }
            arrayList.add(moduleConfigEntry);
        }
        return arrayList;
    }

    static int typeSwitch00351(Object o) {
        if (o == null) return -1;
        if (o instanceof ExpandableSetting) return 0;
        if (o instanceof BooleanSetting) return 1;
        if (o instanceof KeybindSetting) return 2;
        if (o instanceof ModeSetting) return 3;
        if (o instanceof EnumSetting) return 4;
        if (o instanceof MultiSelectSetting) return 5;
        if (o instanceof OrderedEnumSetting) return 6;
        if (o instanceof ColorSetting) return 7;
        if (o instanceof NumberSetting) return 8;
        if (o instanceof TextFieldSetting) return 9;
        if (o instanceof LabelSelectSetting) return 10;
        if (o instanceof LabelMultiSelectSetting) return 11;
        return 12;
    }

    public LayoutNode serializeSetting(Setting setting) {
        LayoutNode layoutNode = new LayoutNode().name(setting.getName().original()).type(setting.getClass().getSimpleName());
        Objects.requireNonNull(setting);
        switch (typeSwitch00351(setting)) {
            case 0:
                ExpandableSetting expandableSetting = (ExpandableSetting) setting;
                layoutNode.data().put("value", Boolean.valueOf(expandableSetting.isValue()));
                layoutNode.data().put("keys", expandableSetting.getKeybinds());
                layoutNode.data().put("bindType", expandableSetting.getType().name());
                Iterator<Setting> it = expandableSetting.getSubSettings().iterator();
                while (it.hasNext()) {
                    layoutNode.children().add(serializeSetting(it.next()));
                }
                break;
            case 1:
                BooleanSetting booleanSetting = (BooleanSetting) setting;
                layoutNode.data().put("value", Boolean.valueOf(booleanSetting.isValue()));
                layoutNode.data().put("key", Integer.valueOf(booleanSetting.getKey()));
                layoutNode.data().put("bindType", booleanSetting.getType().name());
                break;
            case 2:
                KeybindSetting keybindSetting = (KeybindSetting) setting;
                layoutNode.data().put("keys", keybindSetting.getKeyBind());
                layoutNode.data().put("bindType", keybindSetting.getType().name());
                break;
            case 3:
                layoutNode.data().put("value", ((ModeSetting) setting).currentValue().name());
                break;
            case 4:
                layoutNode.data().put("value", ((EnumSetting) setting).currentValue().name());
                break;
            case 5:
                ArrayList arrayList = new ArrayList();
                Iterator it2 = ((MultiSelectSetting) setting).selectedValues().iterator();
                while (it2.hasNext()) {
                    arrayList.add(((Enum) it2.next()).name());
                }
                layoutNode.data().put("selected", arrayList);
                break;
            case 6:
                OrderedEnumSetting<?> orderedEnumSetting = (OrderedEnumSetting) setting;
                ArrayList arrayList2 = new ArrayList();
                Iterator<Enum<?>> it3 = getSelectedEnums(orderedEnumSetting).iterator();
                while (it3.hasNext()) {
                    arrayList2.add(it3.next().name());
                }
                ArrayList arrayList3 = new ArrayList();
                Iterator it4 = orderedEnumSetting.order().iterator();
                while (it4.hasNext()) {
                    arrayList3.add(((Enum) it4.next()).name());
                }
                layoutNode.data().put("selected", arrayList2);
                layoutNode.data().put("order", arrayList3);
                break;
            case 7:
                layoutNode.data().put("color", Integer.valueOf(((ColorSetting) setting).getColor()));
                break;
            case 8:
                layoutNode.data().put("value", Float.valueOf(((NumberSetting) setting).currentValue()));
                break;
            case 9:
                layoutNode.data().put("text", ((TextFieldSetting) setting).getText());
                break;
            case 10:
                layoutNode.data().put("index", Integer.valueOf(((LabelSelectSetting) setting).getSelectedIndex()));
                break;
            case 11:
                LabelMultiSelectSetting labelMultiSelectSetting = (LabelMultiSelectSetting) setting;
                ArrayList arrayList4 = new ArrayList();
                Iterator<Translation> it5 = labelMultiSelectSetting.getSelected().iterator();
                while (it5.hasNext()) {
                    arrayList4.add(Integer.valueOf(labelMultiSelectSetting.getList().indexOf(it5.next())));
                }
                layoutNode.data().put("indexes", arrayList4);
                break;
        }
        return layoutNode;
    }

    public Set<Enum<?>> getSelectedEnums(OrderedEnumSetting orderedEnumSetting) {
        return orderedEnumSetting.getSelected();
    }
}
