package ru.fluxvisuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.module.ModuleManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Сохранение/загрузка конфига (тема + модули + настройки) в config/fluxvisuals.json. */
public class ConfigManager {
    public static final ConfigManager INSTANCE = new ConfigManager();

    public int accent = 0xFFA855F7;      // основной акцент (пурпурный по умолчанию)
    public int accentSecond = 0xFFEC4899; // второй акцент (розовый)
    public float bgAlpha = 0.45f;        // прозрачность фоновых панелей
    public String clientName = "FluxVisuals";

    private final Map<String, Boolean> pendingEnabled = new LinkedHashMap<>();
    private final Map<String, Integer> pendingKeys = new HashMap<>();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() {}

    public Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("fluxvisuals.json");
    }

    public void load() {
        Path path = configPath();
        if (!Files.exists(path)) return;
        try {
            JsonObject root = gson.fromJson(Files.readString(path), JsonObject.class);
            if (root == null) return;
            if (root.has("accent")) accent = root.get("accent").getAsInt();
            if (root.has("accentSecond")) accentSecond = root.get("accentSecond").getAsInt();
            if (root.has("bgAlpha")) bgAlpha = root.get("bgAlpha").getAsFloat();
            if (root.has("clientName")) clientName = root.get("clientName").getAsString();
            if (root.has("modules") && root.get("modules").isJsonObject()) {
                JsonObject mods = root.getAsJsonObject("modules");
                for (Module m : ModuleManager.INSTANCE.getAll()) {
                    if (!mods.has(m.name)) continue;
                    JsonObject obj = mods.getAsJsonObject(m.name);
                    if (obj.has("enabled")) pendingEnabled.put(m.name, obj.get("enabled").getAsBoolean());
                    if (obj.has("key")) pendingKeys.put(m.name, obj.get("key").getAsInt());
                    if (obj.has("settings") && obj.get("settings").isJsonObject()) {
                        JsonObject sets = obj.getAsJsonObject("settings");
                        for (Setting s : m.getSettings()) {
                            if (sets.has(s.name)) s.load(unwrap(sets.get(s.name)));
                        }
                    }
                }
            }
            FluxVisualsClient.LOGGER.info("FluxVisuals config loaded ({} module states pending)", pendingEnabled.size());
        } catch (Exception e) {
            FluxVisualsClient.LOGGER.warn("Failed to load FluxVisuals config", e);
        }
    }

    /**
     * Применяет сохранённые состояния модулей. Вызывается на первом клиентском тике,
     * когда MinecraftClient уже существует (иначе onEnable не может трогать mc()).
     */
    public void applyPending() {
        for (Module m : ModuleManager.INSTANCE.getAll()) {
            if (pendingEnabled.containsKey(m.name)) m.setEnabled(pendingEnabled.get(m.name));
            if (pendingKeys.containsKey(m.name)) m.setKey(pendingKeys.get(m.name));
        }
        pendingEnabled.clear();
        pendingKeys.clear();
    }

    public void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("accent", accent);
            root.addProperty("accentSecond", accentSecond);
            root.addProperty("bgAlpha", bgAlpha);
            root.addProperty("clientName", clientName);

            JsonObject mods = new JsonObject();
            for (Module m : ModuleManager.INSTANCE.getAll()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("enabled", m.isEnabled());
                obj.addProperty("key", m.getKey());
                JsonObject sets = new JsonObject();
                for (Setting s : m.getSettings()) {
                    sets.add(s.name, wrap(s.getValue()));
                }
                obj.add("settings", sets);
                mods.add(m.name, obj);
            }
            root.add("modules", mods);

            Path path = configPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(root));
        } catch (IOException e) {
            FluxVisualsClient.LOGGER.warn("Failed to save FluxVisuals config", e);
        }
    }

    private JsonElement wrap(Object v) {
        if (v instanceof Boolean || v instanceof Number) return gson.toJsonTree(v);
        return gson.toJsonTree(String.valueOf(v));
    }

    private Object unwrap(JsonElement el) {
        if (el == null) return null;
        if (el.isJsonPrimitive()) {
            var p = el.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber()) return p.getAsNumber();
            return p.getAsString();
        }
        return el.getAsString();
    }
}
