package ru.fluxvisuals.ui.fluxgui.config;

import java.nio.file.Path;
import java.util.List;

/** Конфигурации GUI FluxVisuals. */
public final class ConfigManager {
    public static final ConfigManager INSTANCE = new ConfigManager();

    private ConfigManager() {
    }

    public List<String> listConfigs() {
        return List.of("default");
    }

    public String getActiveConfigName() {
        return "default";
    }

    public Path getConfigDirectory() {
        return net.minecraft.client.MinecraftClient.getInstance().runDirectory.toPath().resolve("fluxvisuals");
    }

    public boolean deleteConfig(String name) {
        return false;
    }

    public void saveConfig(String name) {
    }

    public void loadConfig(String name) {
    }

    public boolean importFromCode(String code) {
        return false;
    }
}
