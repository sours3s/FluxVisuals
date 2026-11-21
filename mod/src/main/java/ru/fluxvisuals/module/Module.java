package ru.fluxvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.setting.impl.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Базовый модуль. Всё, что он делает — рисует на клиенте. Никаких пакетов.
 */
public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    private boolean enabled;
    private int key; // GLFW keycode, 0 = none
    private final List<Setting> settings = new ArrayList<>();

    // ClickGUI fields
    public boolean favorite = false;
    public boolean expanded = false;
    public String displayName; // For getBigName() compatibility

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected MinecraftClient mc() { return MinecraftClient.getInstance(); }

    protected void addSetting(Setting setting) {
        settings.add(setting);
    }

    // Return Config.Setting for config system compatibility
    public List<Setting> getSettings() { return settings; }

    // ClickGUI compatibility - returns api.setting.Setting
    public List<ru.fluxvisuals.module.api.setting.Setting> getSettingsForAPI() {
        List<ru.fluxvisuals.module.api.setting.Setting> apiSettings = new ArrayList<>();
        for (Setting s : settings) {
            // Convert FluxVisuals config settings to api settings
            if (s.type == Setting.Type.BOOLEAN) {
                BooleanSetting bs = new BooleanSetting(s.name, s.getBoolean());
                bs.description = s.description;
                apiSettings.add(bs);
            } else if (s.type == Setting.Type.FLOAT || s.type == Setting.Type.INT) {
                SliderSetting ss = new SliderSetting(s.name, s.getFloat(), s.min, s.max, 0.1f, s.type == Setting.Type.FLOAT);
                ss.description = s.description;
                apiSettings.add(ss);
            } else if (s.type == Setting.Type.MODE) {
                ModeSetting ms = new ModeSetting(s.name, s.getMode(), s.modes);
                ms.description = s.description;
                apiSettings.add(ms);
            } else if (s.type == Setting.Type.COLOR) {
                HueSetting hs = new HueSetting(s.name, s.getColor());
                hs.description = s.description;
                apiSettings.add(hs);
            } else if (s.type == Setting.Type.STRING) {
                StringSetting ss = new StringSetting(s.name, s.getString());
                ss.description = s.description;
                apiSettings.add(ss);
            }
        }
        return apiSettings;
    }

    public List<Setting> getSettingsForGUI() { return settings; }
    public String getName() { return name; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public int bind() { return key; }

    // ClickGUI expand methods
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }

    public String getBigName() { return displayName != null ? displayName : name; }

    public Category getCategory() { return category; }

    // Keybind compatibility
    public int getBind() { return key; }
    public void setBind(int key) { this.key = key; }

    public boolean isEnabled() { return enabled; }

    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        if (enabled) enable(); else disable();
    }

    public void toggle() {
        if (enabled) disable(); else enable();
    }

    public void enable() {
        enabled = true;
        onEnable();
    }

    public void disable() {
        enabled = false;
        onDisable();
    }

    protected void onEnable() {}
    protected void onDisable() {}

    /** Вызывается каждый клиентский тик, если модуль включён. */
    public void onTick() {}

    /** Отрисовка HUD (2D). Вызывается в HudRenderCallback, если модуль включён. */
    public void onRender2D(DrawContext g, float tickDelta) {}

    /** Отрисовка в мире (3D). Вызывается в WorldRenderEvents, если модуль включён. */
    public void onRender3D(MatrixStack ms, VertexConsumerProvider consumers, Camera camera, float tickDelta) {}

    /** Скрыть из ArrayList (например для Watermark). */
    public boolean shouldList() { return true; }
}
