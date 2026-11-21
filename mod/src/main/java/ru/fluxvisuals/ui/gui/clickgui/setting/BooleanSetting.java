package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.type.BindMode;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class BooleanSetting extends Setting {
    public BindMode type;
    public boolean value;
    public final List<Integer> keyBinds;

    public List<Integer> getKeyBind() {
        return Collections.unmodifiableList(this.keyBinds);
    }

    public BooleanSetting(Translation translation, Translation class254Var2) {
        super(translation, class254Var2);
        this.type = BindMode.TOGGLE;
        this.keyBinds = new ArrayList();
    }

    public BooleanSetting(Translation translation) {
        this(translation, null);
    }

    public BooleanSetting visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    public void switchValue() {
        this.value = !this.value;
    }

    public int getKey() {
        if (this.keyBinds.isEmpty()) {
            return -1;
        }
        return ((Integer) this.keyBinds.getFirst()).intValue();
    }

    public void setKey(int i) {
        setKey(i == -1 ? List.of() : List.of(Integer.valueOf(i)));
    }

    public void setKey(List<Integer> list) {
        this.keyBinds.clear();
        if (list == null) {
            return;
        }
        Stream<Integer> streamLimit = list.stream().filter(num -> {
            return num.intValue() != -1;
        }).limit(2L);
        List<Integer> list2 = this.keyBinds;
        Objects.requireNonNull(list2);
        streamLimit.forEach((v1) -> {
            list2.add(v1);
        });
    }

    @Override
    public Map<String, Object> toSerializedData() {
        return Map.of("value", Boolean.valueOf(this.value), "key", Integer.valueOf(getKey()), "bindType", this.type.name());
    }

    @Override
    public void loadSerializedData(Map<String, Object> map) {
        Object obj = map.get("value");
        if (obj instanceof Boolean) {
            setValue(((Boolean) obj).booleanValue());
        }
        Object obj2 = map.get("key");
        if (obj2 instanceof Number) {
            setKey(((Number) obj2).intValue());
        }
        Object obj3 = map.get("bindType");
        if (obj3 instanceof String) {
            setType(BindMode.valueOf((String) obj3));
        }
    }

    public BindMode getType() {
        return this.type;
    }

    public boolean isValue() {
        return this.value;
    }

    public BooleanSetting setType(BindMode bindMode) {
        this.type = bindMode;
        return this;
    }

    public BooleanSetting setValue(boolean z) {
        this.value = z;
        return this;
    }
}
