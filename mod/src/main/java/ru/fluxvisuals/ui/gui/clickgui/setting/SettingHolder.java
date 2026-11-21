package ru.fluxvisuals.ui.gui.clickgui.setting;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;

public class SettingHolder {
    public final List<Setting> settings = Lists.newArrayList();

    public void addSettings(Setting... settings661) {
        this.settings.addAll(Arrays.asList(settings661));
    }

    public Setting get(String str) {
        return this.settings.stream().filter(setting -> {
            return setting.getName().effective().equalsIgnoreCase(str);
        }).findFirst().orElse(null);
    }

    public List<Setting> getSettings() {
        return this.settings;
    }
}
