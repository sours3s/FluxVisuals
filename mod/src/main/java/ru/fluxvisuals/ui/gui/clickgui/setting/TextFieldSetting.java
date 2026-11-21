package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

import java.util.Map;
import java.util.function.Supplier;

public class TextFieldSetting extends Setting {
    public Translation placeholder;
    public String text;
    public int min;
    public int max;
    public boolean onlyDigits;
    public boolean password;

    public TextFieldSetting(Translation translation, Translation class254Var2) {
        super(translation, class254Var2);
        this.min = 0;
        this.max = Integer.MAX_VALUE;
        this.onlyDigits = false;
        this.password = false;
    }

    public TextFieldSetting(Translation translation) {
        this(translation, null);
    }

    public TextFieldSetting visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    @Override
    public Map<String, Object> toSerializedData() {
        return Map.of("text", this.text != null ? this.text : "");
    }

    @Override
    public void loadSerializedData(Map<String, Object> map) {
        Object obj = map.get("text");
        if (obj instanceof String) {
            setText((String) obj);
        }
    }

    public Translation getPlaceholder() {
        return this.placeholder;
    }

    public String getText() {
        return this.text;
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }

    public boolean isOnlyDigits() {
        return this.onlyDigits;
    }

    public boolean isPassword() {
        return this.password;
    }

    public TextFieldSetting setPlaceholder(Translation translation) {
        this.placeholder = translation;
        return this;
    }

    public TextFieldSetting setText(String str) {
        this.text = str;
        return this;
    }

    public TextFieldSetting setMin(int i) {
        this.min = i;
        return this;
    }

    public TextFieldSetting setMax(int i) {
        this.max = i;
        return this;
    }

    public TextFieldSetting setOnlyDigits(boolean z) {
        this.onlyDigits = z;
        return this;
    }

    public TextFieldSetting setPassword(boolean z) {
        this.password = z;
        return this;
    }
}
