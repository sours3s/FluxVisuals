package ru.fluxvisuals.ui.gui.clickgui.setting;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class LabelSelectSetting extends Setting {
    public List<Translation> list;
    public Runnable action;
    public Translation selected;

    public LabelSelectSetting(Translation translation, Translation class254Var2) {
        super(translation, class254Var2);
    }

    public LabelSelectSetting(Translation translation) {
        this(translation, null);
    }

    public LabelSelectSetting value(Translation... translations) {
        List<Translation> listAsList = Arrays.asList(translations);
        this.selected = (Translation) listAsList.getFirst();
        this.list = listAsList;
        return this;
    }

    public LabelSelectSetting selected(int i) {
        this.selected = this.list.get(i);
        return this;
    }

    public LabelSelectSetting visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    public boolean isSelected(Translation translation) {
        return this.selected.original().equalsIgnoreCase(translation.original());
    }

    public void setSelected(Translation translation) {
        this.selected = translation;
        if (this.action != null) {
            this.action.run();
        }
    }

    public int getSelectedIndex() {
        return this.list.indexOf(this.selected);
    }

    @Override
    public Map<String, Object> toSerializedData() {
        return Map.of("index", Integer.valueOf(getSelectedIndex()));
    }

    @Override
    public void loadSerializedData(Map<String, Object> map) {
        Object obj = map.get("index");
        if (obj instanceof Number) {
            selected(((Number) obj).intValue());
        }
    }

    public List<Translation> getList() {
        return this.list;
    }

    public Runnable getAction() {
        return this.action;
    }

    public Translation getSelected() {
        return this.selected;
    }

    public void setAction(Runnable runnable) {
        this.action = runnable;
    }
}
