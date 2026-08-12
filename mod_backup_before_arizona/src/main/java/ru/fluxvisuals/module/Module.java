package ru.fluxvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import ru.fluxvisuals.config.Setting;

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

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected MinecraftClient mc() { return MinecraftClient.getInstance(); }

    protected void addSetting(Setting setting) {
        settings.add(setting);
    }

    public List<Setting> getSettings() { return settings; }

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
