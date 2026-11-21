package ru.fluxvisuals.ui.fluxgui.client.gui.render;

import ru.fluxvisuals.ui.fluxgui.FluxVisuals;
import ru.fluxvisuals.ui.fluxgui.compat.Gui;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

public final class IconRender {
    public static final String LOGO = "logo";
    public static final String SEARCH = "search";
    public static final String LIST = "list";
    public static final String STAR = "star";
    public static final String COMMAND = "command";
    public static final String COMMAND_LARGE = "command_large";
    public static final String GRID = "grid";
    public static final String DIAMOND = "diamond";
    public static final String COMMAND_SMALL = "command_small";
    public static final String PERSON = "person";
    public static final String MONITOR = "monitor";
    public static final String PING = "ping";
    public static final String KEYBOARD = "keyboard";
    public static final String FLASK = "flask";
    public static final String CLOCK = "clock";
    static final Identifier ATLAS = Identifier.of("fluxvisuals", "icons/msdf/gui_icons.png");
    static final Identifier LAYOUT = Identifier.of("fluxvisuals", "icons/msdf/gui_icons.json");
    static final Map<String, IconRender$Icon> ICONS = new HashMap<>();
    static boolean loaded;

    IconRender() {
    }

    public static void reload() {
        ICONS.clear();
        loaded = false;
    }

    public static void draw(DrawContext context, String name, float x, float y, float size, int color) {
        ensureLoaded();
        IconRender$Icon icon = ICONS.get(name);
        if (icon != null && !(size <= 0.0F)) {
            float baseline = y + size;
            float x0 = x + icon.planeLeft * size;
            float y0 = baseline + icon.planeTop * size;
            float x1 = x + icon.planeRight * size;
            float y1 = baseline + icon.planeBottom * size;
            Gui.tex(context, ATLAS, Gui.GUI_MSDF)
                .vertex(x0, y1, icon.u0, icon.v1, color)
                .vertex(x1, y1, icon.u1, icon.v1, color)
                .vertex(x1, y0, icon.u1, icon.v0, color)
                .vertex(x0, y0, icon.u0, icon.v0, color)
                .submit(context);
        }
    }

    public static float getDesignSize(String name) {
        ensureLoaded();
        IconRender$Icon icon = ICONS.get(name);
        return icon == null ? 0.0F : icon.designSize;
    }

    static void ensureLoaded() {
        if (!loaded) {
            loaded = true;

            try {
                Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(LAYOUT);
                if (resource.isEmpty()) {
                    FluxVisuals.LOGGER.error("Missing MSDF icon layout {}", LAYOUT);
                    return;
                }

                try (BufferedReader reader = resource.get().getReader()) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject atlas = root.getAsJsonObject("atlas");
                    float atlasWidth = atlas.get("width").getAsFloat();
                    float atlasHeight = atlas.get("height").getAsFloat();

                    for (Entry<String, JsonElement> entry : root.getAsJsonObject("icons").entrySet()) {
                        JsonObject icon = entry.getValue().getAsJsonObject();
                        ICONS.put(
                            entry.getKey(),
                            new IconRender$Icon(
                                icon.get("planeLeft").getAsFloat(),
                                icon.get("planeTop").getAsFloat(),
                                icon.get("planeRight").getAsFloat(),
                                icon.get("planeBottom").getAsFloat(),
                                icon.get("atlasLeft").getAsFloat() / atlasWidth,
                                icon.get("atlasTop").getAsFloat() / atlasHeight,
                                icon.get("atlasRight").getAsFloat() / atlasWidth,
                                icon.get("atlasBottom").getAsFloat() / atlasHeight,
                                icon.get("designSize").getAsFloat()
                            )
                        );
                    }
                }
            } catch (Exception var11) {
                FluxVisuals.LOGGER.error("Failed to read the MSDF icon layout", var11);
            }
        }
    }
}
