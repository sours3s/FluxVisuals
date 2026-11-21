package ru.fluxvisuals.ui.fluxgui.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;

public class ThemeManager {
    public static final ThemeManager INSTANCE = new ThemeManager();
    private static final String SELECTED_THEME_FILE = "selected_theme.json";
    private final List<Theme> themes = new ArrayList<>();
    private Theme currentTheme;
    private final File themeDir = new File(MinecraftClient.getInstance().runDirectory, "fluxvisuals/themes");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ThemeManager() {
        if (!this.themeDir.exists()) {
            this.themeDir.mkdirs();
        }
    }

    public void init() {
        this.themes.clear();
        Theme defaultTheme = new Theme("Default", false);
        this.themes.add(defaultTheme);
        this.loadCustomThemes();
        Theme defaultNewTheme = this.themes.stream().filter(t -> t.getName().equals("Новая тема")).findFirst().orElse(null);
        if (defaultNewTheme == null) {
            boolean hasCustomThemes = this.themes.stream().anyMatch(Theme::isMutable);
            if (!hasCustomThemes) {
                defaultNewTheme = this.createTheme("Новая тема", defaultTheme, true);
                this.saveCustomTheme(defaultNewTheme);
            }
        }

        this.currentTheme = defaultNewTheme != null ? defaultNewTheme : defaultTheme;
        this.loadPersistedSelectedTheme();
    }

    public void loadPersistedSelectedTheme() {
        File f = new File(this.themeDir, "selected_theme.json");
        if (f.isFile()) {
            try {
                try (FileReader reader = new FileReader(f)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!json.has("name") || !json.get("name").isJsonPrimitive()) {
                        return;
                    }

                    String name = json.get("name").getAsString();

                    for (Theme theme : this.themes) {
                        if (theme.getName().equals(name)) {
                            this.currentTheme = theme;
                            return;
                        }
                    }
                }

                return;
            } catch (Exception var9) {
                var9.printStackTrace();
            }
        }
    }

    private void savePersistedSelectedTheme() {
        if (this.currentTheme != null) {
            File f = new File(this.themeDir, "selected_theme.json");

            try (FileWriter writer = new FileWriter(f)) {
                JsonObject json = new JsonObject();
                json.addProperty("name", this.currentTheme.getName());
                this.gson.toJson(json, writer);
            } catch (IOException var7) {
                var7.printStackTrace();
            }
        }
    }

    public List<Theme> getThemes() {
        return this.themes;
    }

    public List<Theme> getSavedCustomThemes() {
        return this.themes;
    }

    public Theme getCurrentTheme() {
        if (this.currentTheme == null && !this.themes.isEmpty()) {
            this.currentTheme = this.themes.get(0);
        }

        return this.currentTheme;
    }

    public Theme getDefaultTheme() {
        return this.themes.stream().filter(t -> t.getName().equals("Default")).findFirst().orElse(null);
    }

    public int getPrimaryColor() {
        return this.getCurrentTheme().getPrimaryColor();
    }

    public void setTheme(Theme theme) {
        if (theme != null) {
            this.currentTheme = theme;
            this.savePersistedSelectedTheme();
        }
    }

    public void setCurrentThemeByName(String name) {
        for (Theme theme : this.themes) {
            if (theme.getName().equalsIgnoreCase(name)) {
                this.currentTheme = theme;
                this.savePersistedSelectedTheme();
                return;
            }
        }
    }

    public Theme createTheme(String name, Theme base, boolean mutable) {
        Theme newTheme = new Theme(name, mutable);
        if (base != null) {
            newTheme.setGuiIconColor(base.getGuiIconColor());
            newTheme.setHudIconColor(base.getHudIconColor());
            newTheme.setCategoryNameColor(base.getCategoryNameColor());
            newTheme.setEnabledModuleNameColor(base.getEnabledModuleNameColor());
            newTheme.setDisabledModuleNameColor(base.getDisabledModuleNameColor());
            newTheme.setHudTextColor(base.getHudTextColor());
            newTheme.setHudListTextColor(base.getHudListTextColor());
            newTheme.setHudTitleColor(base.getHudTitleColor());
            newTheme.setSliderColor(base.getSliderColor());
            newTheme.setSliderKnobColor(base.getSliderKnobColor());
            newTheme.setTooltipTextColor(base.getTooltipTextColor());
            newTheme.setTooltipBackgroundColor(base.getTooltipBackgroundColor());
            newTheme.setHudBackgroundColor(base.getHudBackgroundColor());
            newTheme.setButtonsColor(base.getButtonsColor());
            newTheme.setEnabledModuleButtonColor(base.getEnabledModuleButtonColor());
            newTheme.setDisabledModuleButtonColor(base.getDisabledModuleButtonColor());
            newTheme.setPanelColor(base.getPanelColor());
            newTheme.setSectionBackgroundColor(base.getSectionBackgroundColor());
            newTheme.setVisualModulesColor(base.getVisualModulesColor());
        }

        this.themes.add(newTheme);
        return newTheme;
    }

    public void removeTheme(Theme theme) {
        if (this.themes.contains(theme) && theme.isMutable()) {
            this.themes.remove(theme);
            File file = new File(this.themeDir, theme.getName() + ".json");
            if (file.exists()) {
                file.delete();
            }

            if (this.currentTheme == theme) {
                this.currentTheme = this.getDefaultTheme();
                this.savePersistedSelectedTheme();
            }
        }
    }

    public void saveCustomTheme(Theme theme) {
        if (theme.isMutable()) {
            File file = new File(this.themeDir, theme.getName() + ".json");

            try (FileWriter writer = new FileWriter(file)) {
                JsonObject json = new JsonObject();
                json.addProperty("name", theme.getName());
                json.addProperty("guiIconColor", theme.getGuiIconColor());
                json.addProperty("hudIconColor", theme.getHudIconColor());
                json.addProperty("categoryNameColor", theme.getCategoryNameColor());
                json.addProperty("enabledModuleNameColor", theme.getEnabledModuleNameColor());
                json.addProperty("disabledModuleNameColor", theme.getDisabledModuleNameColor());
                json.addProperty("hudTextColor", theme.getHudTextColor());
                json.addProperty("hudListTextColor", theme.getHudListTextColor());
                json.addProperty("hudTitleColor", theme.getHudTitleColor());
                json.addProperty("sliderColor", theme.getSliderColor());
                json.addProperty("sliderKnobColor", theme.getSliderKnobColor());
                json.addProperty("tooltipTextColor", theme.getTooltipTextColor());
                json.addProperty("tooltipBackgroundColor", theme.getTooltipBackgroundColor());
                json.addProperty("hudBackgroundColor", theme.getHudBackgroundColor());
                json.addProperty("buttonsColor", theme.getButtonsColor());
                json.addProperty("enabledModuleButtonColor", theme.getEnabledModuleButtonColor());
                json.addProperty("disabledModuleButtonColor", theme.getDisabledModuleButtonColor());
                json.addProperty("panelColor", theme.getPanelColor());
                json.addProperty("sectionBackgroundColor", theme.getSectionBackgroundColor());
                json.addProperty("visualModulesColor", theme.getVisualModulesColor());
                this.gson.toJson(json, writer);
            } catch (IOException var8) {
                var8.printStackTrace();
            }
        }
    }

    public void loadCustomThemes(List<Theme> loadedThemes) {
        if (loadedThemes != null) {
            for (Theme t : loadedThemes) {
                if (this.themes.stream().noneMatch(exist -> exist.getName().equals(t.getName()))) {
                    this.themes.add(t);
                }
            }
        }
    }

    public void loadCustomThemes() {
        File[] files = this.themeDir.listFiles((dir, namex) -> namex.endsWith(".json") && !"selected_theme.json".equals(namex));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    String name = json.get("name").getAsString();
                    if (!this.themes.stream().anyMatch(t -> t.getName().equals(name))) {
                        Theme theme = new Theme(name, true);
                        if (json.has("guiIconColor")) {
                            theme.setGuiIconColor(json.get("guiIconColor").getAsInt());
                        }

                        if (json.has("hudIconColor")) {
                            theme.setHudIconColor(json.get("hudIconColor").getAsInt());
                        }

                        if (json.has("categoryNameColor")) {
                            theme.setCategoryNameColor(json.get("categoryNameColor").getAsInt());
                        }

                        if (json.has("enabledModuleNameColor")) {
                            theme.setEnabledModuleNameColor(json.get("enabledModuleNameColor").getAsInt());
                        }

                        if (json.has("disabledModuleNameColor")) {
                            theme.setDisabledModuleNameColor(json.get("disabledModuleNameColor").getAsInt());
                        }

                        if (!json.has("enabledModuleNameColor") && json.has("moduleNameColor")) {
                            int moduleNameColor = json.get("moduleNameColor").getAsInt();
                            theme.setEnabledModuleNameColor(moduleNameColor);
                            theme.setDisabledModuleNameColor(moduleNameColor);
                        }

                        if (json.has("hudTextColor")) {
                            theme.setHudTextColor(json.get("hudTextColor").getAsInt());
                        }

                        if (json.has("hudListTextColor")) {
                            theme.setHudListTextColor(json.get("hudListTextColor").getAsInt());
                        }

                        if (json.has("hudTitleColor")) {
                            theme.setHudTitleColor(json.get("hudTitleColor").getAsInt());
                        }

                        if (json.has("sliderColor")) {
                            theme.setSliderColor(json.get("sliderColor").getAsInt());
                        }

                        if (json.has("sliderKnobColor")) {
                            theme.setSliderKnobColor(json.get("sliderKnobColor").getAsInt());
                        }

                        if (json.has("tooltipTextColor")) {
                            theme.setTooltipTextColor(json.get("tooltipTextColor").getAsInt());
                        }

                        if (json.has("tooltipBackgroundColor")) {
                            theme.setTooltipBackgroundColor(json.get("tooltipBackgroundColor").getAsInt());
                        }

                        if (json.has("hudBackgroundColor")) {
                            theme.setHudBackgroundColor(json.get("hudBackgroundColor").getAsInt());
                        }

                        if (json.has("buttonsColor")) {
                            theme.setButtonsColor(json.get("buttonsColor").getAsInt());
                        }

                        if (!json.has("enabledModuleButtonColor")) {
                            theme.setEnabledModuleButtonColor(theme.getButtonsColor());
                        }

                        if (!json.has("disabledModuleButtonColor")) {
                            theme.setDisabledModuleButtonColor(theme.getButtonsColor());
                        }

                        if (json.has("enabledModuleButtonColor")) {
                            theme.setEnabledModuleButtonColor(json.get("enabledModuleButtonColor").getAsInt());
                        }

                        if (json.has("disabledModuleButtonColor")) {
                            theme.setDisabledModuleButtonColor(json.get("disabledModuleButtonColor").getAsInt());
                        }

                        if (json.has("panelColor")) {
                            theme.setPanelColor(json.get("panelColor").getAsInt());
                        }

                        if (json.has("gradientStartColor") && !json.has("panelColor")) {
                            theme.setPanelColor(json.get("gradientStartColor").getAsInt());
                        }

                        if (json.has("sectionBackgroundColor")) {
                            theme.setSectionBackgroundColor(json.get("sectionBackgroundColor").getAsInt());
                        } else {
                            theme.setSectionBackgroundColor(theme.getDisabledModuleButtonColor());
                        }

                        if (json.has("visualModulesColor")) {
                            theme.setVisualModulesColor(json.get("visualModulesColor").getAsInt());
                        }

                        this.themes.add(theme);
                    }
                } catch (Exception var13) {
                    var13.printStackTrace();
                }
            }
        }
    }

    public int getGuiIconColor() {
        return this.getCurrentTheme().getGuiIconColor();
    }

    public int getHudIconColor() {
        return this.getCurrentTheme().getHudIconColor();
    }

    public int getCategoryNameColor() {
        return this.getCurrentTheme().getCategoryNameColor();
    }

    public int getEnabledModuleNameColor() {
        return this.getCurrentTheme().getEnabledModuleNameColor();
    }

    public int getDisabledModuleNameColor() {
        return this.getCurrentTheme().getDisabledModuleNameColor();
    }

    public int getHudTextColor() {
        return this.getCurrentTheme().getHudTextColor();
    }

    public int getHudListTextColor() {
        return this.getCurrentTheme().getHudListTextColor();
    }

    public int getHudTitleColor() {
        return this.getCurrentTheme().getHudTitleColor();
    }

    public int getSliderColor() {
        return this.getCurrentTheme().getSliderColor();
    }

    public int getSliderKnobColor() {
        return this.getCurrentTheme().getSliderKnobColor();
    }

    public int getTooltipTextColor() {
        return this.getCurrentTheme().getTooltipTextColor();
    }

    public int getHudBackgroundColor() {
        return this.getCurrentTheme().getHudBackgroundColor();
    }

    public int getButtonsColor() {
        return this.getCurrentTheme().getButtonsColor();
    }

    public int getEnabledModuleButtonColor() {
        return this.getCurrentTheme().getEnabledModuleButtonColor();
    }

    public int getDisabledModuleButtonColor() {
        return this.getCurrentTheme().getDisabledModuleButtonColor();
    }

    public int getPanelColor() {
        return this.getCurrentTheme().getPanelColor();
    }

    public int getSectionBackgroundColor() {
        return this.getCurrentTheme().getSectionBackgroundColor();
    }

    public int getVisualModulesColor() {
        return this.getCurrentTheme().getVisualModulesColor();
    }

    public int getTestGuiSectionsColor() {
        return this.getCurrentTheme().getTestGuiSectionsColor();
    }
}
