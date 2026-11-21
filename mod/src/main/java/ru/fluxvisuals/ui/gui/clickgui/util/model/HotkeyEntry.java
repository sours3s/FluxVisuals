package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.ClickGuiModule;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;

import java.util.ArrayList;
import java.util.List;

public class HotkeyEntry {
    public final String name;
    public final String moduleName;
    public final List<Integer> keys;
    public final ToggleAnimator anim;

    public HotkeyEntry(String str, List<Integer> list) {
        this.keys = new ArrayList();
        this.anim = ToggleAnimator.times(2, 80);
        this.moduleName = null;
        this.name = str;
        if (list != null) {
            this.keys.addAll(list);
        }
    }

    public HotkeyEntry(ClickGuiModule module, String str, List<Integer> list) {
        this.keys = new ArrayList();
        this.anim = ToggleAnimator.times(2, 80);
        this.moduleName = module != null ? module.getName() : null;
        this.name = str;
        if (list != null) {
            this.keys.addAll(list);
        }
    }

    public boolean hasModule() {
        return this.moduleName != null;
    }

    public String name() {
        return this.name;
    }

    public String moduleName() {
        return this.moduleName;
    }

    public List<Integer> keys() {
        return this.keys;
    }

    public ToggleAnimator anim() {
        return this.anim;
    }
}
