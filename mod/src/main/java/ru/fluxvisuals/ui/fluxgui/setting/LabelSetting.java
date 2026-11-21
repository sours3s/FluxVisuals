package ru.fluxvisuals.ui.fluxgui.setting;

public class LabelSetting extends Setting<String> {
    private boolean centered;

    public LabelSetting(String name) {
        super(name, name);
    }

    public boolean isCentered() {
        return this.centered;
    }

    public LabelSetting centered() {
        this.centered = true;
        return this;
    }
}
