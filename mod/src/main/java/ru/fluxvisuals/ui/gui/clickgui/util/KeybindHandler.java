package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.type.BindMode;
import ru.fluxvisuals.ui.gui.clickgui.setting.BooleanSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ButtonAction;
import ru.fluxvisuals.ui.gui.clickgui.events.api.ClientListener;
import ru.fluxvisuals.ui.gui.clickgui.setting.ExpandableSetting;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.util.type.KeyAction;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.KeyInputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.type.KeyPressState;
import ru.fluxvisuals.ui.gui.clickgui.setting.KeybindSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;
import ru.fluxvisuals.ui.gui.clickgui.ClickGuiModule;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.MouseButtonEvent;
import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;
import ru.fluxvisuals.ui.gui.clickgui.util.WavSoundPlayer;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.SwitchBootstraps;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class KeybindHandler implements ClientListener {
    public static final int codeBufferLength = 6;
    public static final String secretCode = "mxrbid";
    public final StringBuilder typedBuffer = new StringBuilder();
    public final Set<Integer> pressedKeys = new HashSet();

    public KeybindHandler() {
        FluxVisuals.INSTANCE.eventDispatcher().register(KeyInputEvent.class, this::onKeyInput);
        FluxVisuals.INSTANCE.eventDispatcher().register(MouseButtonEvent.class, this::onMouseButton);
    }

    public void onKeyInput(KeyInputEvent keyInputEvent) {
        if (Mc.INSTANCE.getWorld() == null) {
            return;
        }
        if (keyInputEvent.action() == KeyPressState.PRESS) {
            handleTypedChar((char) keyInputEvent.key());
            this.pressedKeys.add(Integer.valueOf(keyInputEvent.key()));
        }
        forEachModuleSetting((module, setting) -> {
            switch (typeSwitch00252(setting)) {
                case -1: // Module.KEY_UNBOUND == -1 (stub: ClickGuiModule has no KEY_UNBOUND constant)
                    processModuleBind(keyInputEvent, module);
                    break;
                case 0:
                    processBooleanSettingBind(keyInputEvent, (BooleanSetting) setting);
                    break;
                case 1:
                    ExpandableSetting expandableSetting = (ExpandableSetting) setting;
                    processExpandableSettingBind(keyInputEvent, expandableSetting);
                    if (expandableSetting.isValue()) {
                        expandableSetting.getSubSettings().forEach(class661Var2 -> {
                            if (class661Var2 instanceof KeybindSetting) {
                                processKeybindSettingBind(keyInputEvent, (KeybindSetting) class661Var2);
                            }
                        });
                    }
                    break;
                case 2:
                    processKeybindSettingBind(keyInputEvent, (KeybindSetting) setting);
                    break;
            }
        });
        if (keyInputEvent.action() == KeyPressState.RELEASE) {
            this.pressedKeys.remove(Integer.valueOf(keyInputEvent.key()));
        }
    }

    public void onMouseButton(MouseButtonEvent mouseButtonEvent) {
        if (Mc.INSTANCE.getWorld() == null) {
            return;
        }
        if (mouseButtonEvent.action() == ButtonAction.PRESS) {
            this.pressedKeys.add(Integer.valueOf(mouseButtonEvent.button()));
        }
        forEachModuleSetting((module, setting) -> {
            switch (typeSwitch00252(setting)) {
                case -1: // Module.KEY_UNBOUND == -1 (stub: ClickGuiModule has no KEY_UNBOUND constant)
                    processModuleBind(mouseButtonEvent, module);
                    break;
                case 0:
                    processBooleanSettingBind(mouseButtonEvent, (BooleanSetting) setting);
                    break;
                case 1:
                    ExpandableSetting expandableSetting = (ExpandableSetting) setting;
                    processExpandableSettingBind(mouseButtonEvent, expandableSetting);
                    if (expandableSetting.isValue()) {
                        expandableSetting.getSubSettings().forEach(class661Var2 -> {
                            if (class661Var2 instanceof KeybindSetting) {
                                processKeybindSettingBind(mouseButtonEvent, (KeybindSetting) class661Var2);
                            }
                        });
                    }
                    break;
                case 2:
                    processKeybindSettingBind(mouseButtonEvent, (KeybindSetting) setting);
                    break;
            }
        });
        if (mouseButtonEvent.action() == ButtonAction.RELEASE) {
            this.pressedKeys.remove(Integer.valueOf(mouseButtonEvent.button()));
        }
    }

    public void handleTypedChar(char c) {
        this.typedBuffer.append(c);
        if (this.typedBuffer.length() > codeBufferLength) {
            this.typedBuffer.delete(0, this.typedBuffer.length() - codeBufferLength);
        }
        if (secretCode.equalsIgnoreCase(this.typedBuffer.toString())) {
            WavSoundPlayer.INSTANCE.playSound(secretCode, 80.0f, false);
        }
    }

    public void forEachModuleSetting(ModuleSettingConsumer moduleSettingConsumer) {
        for (ClickGuiModule module : FluxVisuals.INSTANCE.moduleRepository().getModules()) {
            moduleSettingConsumer.process(module, null);
            if (module.isState()) {
                Iterator<Setting> it = module.getSettings().iterator();
                while (it.hasNext()) {
                    moduleSettingConsumer.process(module, it.next());
                }
            }
        }
    }

    public void processModuleBind(Object obj, ClickGuiModule module) {
        if (module.getKeyBind().isEmpty()) {
            return;
        }
        if (obj instanceof KeyInputEvent) {
            KeyInputEvent keyInputEvent = (KeyInputEvent) obj;
            if (matchesKeyBind(keyInputEvent, module.getKeyBind())) {
                BindMode type = module.getType();
                boolean z = keyInputEvent.action() == KeyPressState.PRESS;
                Objects.requireNonNull(module);
                applyBindAction(type, z, module::switchState);
                return;
            }
        }
        if (obj instanceof MouseButtonEvent) {
            MouseButtonEvent mouseButtonEvent = (MouseButtonEvent) obj;
            if (matchesMouseBind(mouseButtonEvent, module.getKeyBind())) {
                BindMode type2 = module.getType();
                boolean z2 = mouseButtonEvent.action() == ButtonAction.PRESS;
                Objects.requireNonNull(module);
                applyBindAction(type2, z2, module::switchState);
            }
        }
    }

    public void processBooleanSettingBind(Object obj, BooleanSetting booleanSetting) {
        if (booleanSetting.getKeyBind().isEmpty()) {
            return;
        }
        if (obj instanceof KeyInputEvent) {
            KeyInputEvent keyInputEvent = (KeyInputEvent) obj;
            if (matchesKeyBind(keyInputEvent, booleanSetting.getKeyBind())) {
                BindMode type = booleanSetting.getType();
                boolean z = keyInputEvent.action() == KeyPressState.PRESS;
                Objects.requireNonNull(booleanSetting);
                applyBindAction(type, z, booleanSetting::switchValue);
                return;
            }
        }
        if (obj instanceof MouseButtonEvent) {
            MouseButtonEvent mouseButtonEvent = (MouseButtonEvent) obj;
            if (matchesMouseBind(mouseButtonEvent, booleanSetting.getKeyBind())) {
                BindMode type2 = booleanSetting.getType();
                boolean z2 = mouseButtonEvent.action() == ButtonAction.PRESS;
                Objects.requireNonNull(booleanSetting);
                applyBindAction(type2, z2, booleanSetting::switchValue);
            }
        }
    }

    public void processExpandableSettingBind(Object obj, ExpandableSetting expandableSetting) {
        if (expandableSetting.getKeybinds().isEmpty()) {
            return;
        }
        Runnable runnable = () -> {
            expandableSetting.setValue(!expandableSetting.isValue());
        };
        if (obj instanceof KeyInputEvent) {
            KeyInputEvent keyInputEvent = (KeyInputEvent) obj;
            if (matchesKeyBind(keyInputEvent, expandableSetting.getKeybinds())) {
                applyBindAction(expandableSetting.getType(), keyInputEvent.action() == KeyPressState.PRESS, runnable);
                return;
            }
        }
        if (obj instanceof MouseButtonEvent) {
            MouseButtonEvent mouseButtonEvent = (MouseButtonEvent) obj;
            if (matchesMouseBind(mouseButtonEvent, expandableSetting.getKeybinds())) {
                applyBindAction(expandableSetting.getType(), mouseButtonEvent.action() == ButtonAction.PRESS, runnable);
            }
        }
    }

    public void processKeybindSettingBind(Object obj, KeybindSetting keybindSetting) {
        if (keybindSetting.getKeyBind().isEmpty()) {
            return;
        }
        if (obj instanceof KeyInputEvent) {
            KeyInputEvent keyInputEvent = (KeyInputEvent) obj;
            if (matchesKeyBind(keyInputEvent, keybindSetting.getKeyBind())) {
                KeyAction keyAction = keyInputEvent.action() == KeyPressState.PRESS ? KeyAction.PRESS : KeyAction.RELEASE;
                applyBindAction(keybindSetting.getType(), keyInputEvent.action() == KeyPressState.PRESS, () -> {
                    keybindSetting.getConsumer().accept(keyAction);
                });
                return;
            }
        }
        if (obj instanceof MouseButtonEvent) {
            MouseButtonEvent mouseButtonEvent = (MouseButtonEvent) obj;
            if (matchesMouseBind(mouseButtonEvent, keybindSetting.getKeyBind())) {
                KeyAction class664Var2 = mouseButtonEvent.action() == ButtonAction.PRESS ? KeyAction.PRESS : KeyAction.RELEASE;
                applyBindAction(keybindSetting.getType(), mouseButtonEvent.action() == ButtonAction.PRESS, () -> {
                    keybindSetting.getConsumer().accept(class664Var2);
                });
            }
        }
    }

    public void applyBindAction(BindMode bindMode, boolean z, Runnable runnable) {
        if (bindMode == BindMode.TOGGLE && z) {
            runnable.run();
        } else if (bindMode == BindMode.HOLD) {
            runnable.run();
        }
    }

    public boolean matchesKeyBind(KeyInputEvent keyInputEvent, List<Integer> list) {
        if ((keyInputEvent.action() == KeyPressState.PRESS || keyInputEvent.action() == KeyPressState.RELEASE) && keyInputEvent.key() == ((Integer) list.getLast()).intValue()) {
            if (isCombinationPressed(keyInputEvent.action() == KeyPressState.RELEASE, list)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesMouseBind(MouseButtonEvent mouseButtonEvent, List<Integer> list) {
        if ((mouseButtonEvent.action() == ButtonAction.PRESS || mouseButtonEvent.action() == ButtonAction.RELEASE) && mouseButtonEvent.button() == ((Integer) list.getLast()).intValue()) {
            if (isCombinationPressed(mouseButtonEvent.action() == ButtonAction.RELEASE, list)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCombinationPressed(boolean z, List<Integer> list) {
        HashSet hashSet = new HashSet(this.pressedKeys);
        if (z) {
            hashSet.add((Integer) list.getLast());
        }
        Set<Integer> setMethod027 = collectAllBoundKeys();
        HashSet hashSet2 = new HashSet();
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            int iIntValue = ((Integer) it.next()).intValue();
            if (setMethod027.contains(Integer.valueOf(iIntValue))) {
                hashSet2.add(Integer.valueOf(iIntValue));
            }
        }
        return hashSet2.containsAll(list) && !isOverriddenByLongerBind(hashSet2, list, ((Integer) list.getLast()).intValue());
    }

    public boolean isOverriddenByLongerBind(Set<Integer> set, List<Integer> list, int i) {
        HashSet hashSet = new HashSet(list);
        return collectAllBinds().stream().filter(list2 -> {
            return !new HashSet(list2).equals(hashSet);
        }).filter(list3 -> {
            return !list3.isEmpty() && ((Integer) list3.getLast()).intValue() == i;
        }).anyMatch(list4 -> {
            return set.equals(new HashSet(list4)) && list4.size() > list.size();
        });
    }

    public Set<List<Integer>> collectAllBinds() {
        HashSet hashSet = new HashSet();
        forEachModuleSetting((module, setting) -> {
            switch (typeSwitch00252(setting)) {
                case -1: // Module.KEY_UNBOUND == -1 (stub: ClickGuiModule has no KEY_UNBOUND constant)
                    addBindList(hashSet, module.getKeyBind());
                    break;
                case 0:
                    addBindList(hashSet, ((BooleanSetting) setting).getKeyBind());
                    break;
                case 1:
                    ExpandableSetting expandableSetting = (ExpandableSetting) setting;
                    addBindList(hashSet, expandableSetting.getKeybinds());
                    expandableSetting.getSubSettings().forEach(class661Var2 -> {
                        if (class661Var2 instanceof KeybindSetting) {
                            addBindList(hashSet, ((KeybindSetting) class661Var2).getKeyBind());
                        }
                    });
                    break;
                case 2:
                    addBindList(hashSet, ((KeybindSetting) setting).getKeyBind());
                    break;
            }
        });
        return hashSet;
    }

    public Set<Integer> collectAllBoundKeys() {
        HashSet hashSet = new HashSet();
        forEachModuleSetting((module, setting) -> {
            switch (typeSwitch00252(setting)) {
                case -1: // Module.KEY_UNBOUND == -1 (stub: ClickGuiModule has no KEY_UNBOUND constant)
                    addBindKeys(hashSet, module.getKeyBind());
                    break;
                case 0:
                    addBindKeys(hashSet, ((BooleanSetting) setting).getKeyBind());
                    break;
                case 1:
                    ExpandableSetting expandableSetting = (ExpandableSetting) setting;
                    addBindKeys(hashSet, expandableSetting.getKeybinds());
                    expandableSetting.getSubSettings().forEach(class661Var2 -> {
                        if (class661Var2 instanceof KeybindSetting) {
                            addBindKeys(hashSet, ((KeybindSetting) class661Var2).getKeyBind());
                        }
                    });
                    break;
                case 2:
                    addBindKeys(hashSet, ((KeybindSetting) setting).getKeyBind());
                    break;
            }
        });
        return hashSet;
    }

    public void addBindList(Set<List<Integer>> set, List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        set.add(list);
    }

    public void addBindKeys(Set<Integer> set, List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        set.addAll(list);
    }

    static int typeSwitch00252(Object o) {
        if (o == null) {
            return -1;
        }
        if (o instanceof BooleanSetting) {
            return 0;
        }
        if (o instanceof ExpandableSetting) {
            return 1;
        }
        if (o instanceof KeybindSetting) {
            return 2;
        }
        return 3;
    }
}
