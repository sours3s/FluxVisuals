package ru.fluxvisuals.ui.fluxgui.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MultiBooleanSetting extends Setting<Map<String, Boolean>> {
    private List<String> values;
    private boolean expanded = false;

    public MultiBooleanSetting(String name, String... values) {
        super(name, new HashMap<>());
        this.values = new ArrayList<>(Arrays.asList(values));

        for (String v : values) {
            this.value.put(v, true);
        }
    }

    public MultiBooleanSetting(String name, Supplier<Boolean> visibility, String... values) {
        super(name, new HashMap<>(), visibility);
        this.values = new ArrayList<>(Arrays.asList(values));

        for (String v : values) {
            this.value.put(v, true);
        }
    }

    public void setValues(List<String> newValues) {
        if (newValues != null) {
            this.values = new ArrayList<>(newValues);

            for (String v : newValues) {
                if (!this.value.containsKey(v)) {
                    this.value.put(v, false);
                }
            }
        }
    }

    public boolean isEnabled(String key) {
        return this.value.getOrDefault(key, false);
    }

    public void toggle(String key) {
        if (this.value.containsKey(key)) {
            this.value.put(key, !this.value.get(key));
        }
    }

    public void set(String key, boolean state) {
        if (this.value.containsKey(key)) {
            this.value.put(key, state);
        }
    }

    public void disableAll() {
        for (String key : this.value.keySet()) {
            this.value.put(key, false);
        }
    }

    public List<String> getValues() {
        return this.values;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}
