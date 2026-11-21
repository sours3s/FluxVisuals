package ru.fluxvisuals.ui.fluxgui.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;
    private int index;

    public ModeSetting(String name, String defaultMode, String... modes) {
        super(name, defaultMode);
        this.modes = new ArrayList<>(Arrays.asList(modes));
        this.index = this.modes.indexOf(defaultMode);
        if (this.index == -1) {
            this.index = 0;
        }
    }

    public ModeSetting(String name, String defaultMode, Supplier<Boolean> visibility, String... modes) {
        super(name, defaultMode, visibility);
        this.modes = new ArrayList<>(Arrays.asList(modes));
        this.index = this.modes.indexOf(defaultMode);
        if (this.index == -1) {
            this.index = 0;
        }
    }

    public void setModes(List<String> newModes) {
        String current = this.getValue();
        this.modes.clear();
        this.modes.addAll(newModes);
        if (this.modes.isEmpty()) {
            this.index = 0;
        } else {
            int found = this.modes.indexOf(current);
            this.index = found >= 0 ? found : 0;
        }
    }

    public String getValue() {
        return this.modes.get(this.index);
    }

    public String getSelected() {
        return this.getValue();
    }

    public void setValue(String mode) {
        this.index = this.modes.indexOf(mode);
        if (this.index == -1 && mode != null) {
            for (int i = 0; i < this.modes.size(); i++) {
                if (this.modes.get(i).equalsIgnoreCase(mode)) {
                    this.index = i;
                    break;
                }
            }
        }

        if (this.index == -1) {
            this.index = 0;
        }

        super.setValue(this.modes.get(this.index));
    }

    public void cycle() {
        this.index = this.index < this.modes.size() - 1 ? ++this.index : 0;
    }

    public void setIndex(int i) {
        if (i >= 0 && i < this.modes.size()) {
            this.index = i;
        }
    }

    public int getIndex() {
        return this.index;
    }

    public List<String> getModes() {
        return this.modes;
    }

    public boolean is(String mode) {
        return this.getValue().equalsIgnoreCase(mode);
    }
}
