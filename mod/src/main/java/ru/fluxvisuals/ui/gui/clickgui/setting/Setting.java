package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Setting {
    public final Translation name;
    public final Translation description;
    public Supplier<Boolean> visible;

    public Setting(Translation translation) {
        this(translation, null);
    }

    public Setting setVisible(Supplier<Boolean> supplier) {
        this.visible = supplier;
        return this;
    }

    public Map<String, Object> toSerializedData() {
        return Map.of();
    }

    public void loadSerializedData(Map<String, Object> map) {
    }

    public List<Setting> getSerializableChildren() {
        return List.of();
    }

    public Translation getName() {
        return this.name;
    }

    public Translation getDescription() {
        return this.description;
    }

    public Supplier<Boolean> getVisible() {
        return this.visible;
    }

    public Setting(Translation translation, Translation class254Var2) {
        this.name = translation;
        this.description = class254Var2;
    }
}
