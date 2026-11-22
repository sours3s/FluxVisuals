package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Set;
import net.minecraft.util.math.MathHelper;

public final class HudElementStyleManager {
    public static final List<String> ELEMENT_IDS = List.of(
        "watermark", "notifications", "target", "hotkeys", "potions", "cooldowns", "mediaPlayer", "inventory", "armorHud", "customHotbar"
    );
    public static final Set<String> ICONS = Set.of(
        "logo",
        "search",
        "list",
        "star",
        "command",
        "command_large",
        "grid",
        "diamond",
        "command_small",
        "person",
        "monitor",
        "ping",
        "keyboard",
        "flask",
        "clock"
    );
    static JsonObject styles = new JsonObject();

    HudElementStyleManager() {
    }

    public static synchronized JsonObject snapshot() {
        return styles.deepCopy();
    }

    public static synchronized void load(JsonObject stored) {
        styles = sanitize(stored, false);
    }

    public static synchronized void applyPatch(JsonObject patch) {
        if (patch != null) {
            JsonObject cleaned = sanitize(patch, true);

            for (String id : cleaned.keySet()) {
                JsonObject target = styles.has(id) && styles.get(id).isJsonObject() ? styles.getAsJsonObject(id) : new JsonObject();

                for (String key : cleaned.getAsJsonObject(id).keySet()) {
                    target.add(key, cleaned.getAsJsonObject(id).get(key));
                }

                styles.add(id, target);
            }
        }
    }

    public static synchronized HudElementStyleManager$Style style(String id) {
        JsonObject value = id != null && styles.has(id) && styles.get(id).isJsonObject() ? styles.getAsJsonObject(id) : new JsonObject();
        return new HudElementStyleManager$Style(value);
    }

    static JsonObject sanitize(JsonObject source, boolean patch) {
        JsonObject result = new JsonObject();
        if (source == null) {
            return result;
        }

        for (String id : ELEMENT_IDS) {
            if (source.has(id) && source.get(id).isJsonObject()) {
                JsonObject input = source.getAsJsonObject(id);
                JsonObject output = new JsonObject();
                copyEnum(input, output, "material", "Inherit", "Classic", "Liquid Glass");
                copyColor(input, output, "panelColor");
                copyColor(input, output, "panelColor2");
                copyInt(input, output, "panelOpacity", 0, 255);
                copyFloat(input, output, "radius", 0.0F, 28.0F);
                copyColor(input, output, "textColor");
                copyColor(input, output, "accentColor");
                copyEnum(input, output, "icon", ICONS.toArray(String[]::new));
                copyFloat(input, output, "iconScale", 0.55F, 1.8F);
                copyBoolean(input, output, "shadow");
                copyColor(input, output, "shadowColor");
                copyFloat(input, output, "shadowStrength", 0.25F, 2.5F);
                copyBoolean(input, output, "blur");
                copyFloat(input, output, "blurStrength", 0.25F, 2.5F);
                copyBoolean(input, output, "glow");
                copyColor(input, output, "glowColor");
                copyFloat(input, output, "glowStrength", 0.25F, 3.0F);
                copyBoolean(input, output, "outline");
                copyColor(input, output, "outlineColor");
                copyFloat(input, output, "outlineWidth", 0.5F, 3.0F);
                copyBoolean(input, output, "stripe");
                copyColor(input, output, "stripeColor");
                copyFloat(input, output, "stripeWidth", 1.0F, 8.0F);
                copyEnum(input, output, "stripeSide", "left", "right", "top", "bottom");
                copyString(input, output, "label", 24);
                copyColor(input, output, "labelColor");
                if (!output.isEmpty() || !patch) {
                    result.add(id, output);
                }
            }
        }

        return result;
    }

    static void copyBoolean(JsonObject from, JsonObject to, String key) {
        try {
            if (primitive(from, key)) {
                to.addProperty(key, from.get(key).getAsBoolean());
            }
        } catch (Exception var4) {
        }
    }

    static void copyInt(JsonObject from, JsonObject to, String key, int min, int max) {
        try {
            if (primitive(from, key)) {
                to.addProperty(key, MathHelper.clamp(from.get(key).getAsInt(), min, max));
            }
        } catch (Exception var6) {
        }
    }

    static void copyFloat(JsonObject from, JsonObject to, String key, float min, float max) {
        try {
            if (!primitive(from, key)) {
                return;
            }

            float value = from.get(key).getAsFloat();
            if (Float.isFinite(value)) {
                to.addProperty(key, MathHelper.clamp(value, min, max));
            }
        } catch (Exception var6) {
        }
    }

    static void copyColor(JsonObject from, JsonObject to, String key) {
        try {
            if (!primitive(from, key)) {
                return;
            }

            String value = from.get(key).getAsString().strip().toUpperCase();
            if (value.matches("#[0-9A-F]{6}")) {
                to.addProperty(key, value);
            }
        } catch (Exception var4) {
        }
    }

    static void copyEnum(JsonObject from, JsonObject to, String key, String... allowed) {
        try {
            if (!primitive(from, key)) {
                return;
            }

            String value = from.get(key).getAsString();

            for (String option : allowed) {
                if (option.equalsIgnoreCase(value)) {
                    to.addProperty(key, option);
                    return;
                }
            }
        } catch (Exception var9) {
        }
    }

    static void copyString(JsonObject from, JsonObject to, String key, int maxLength) {
        try {
            if (!primitive(from, key)) {
                return;
            }

            String value = from.get(key).getAsString().replaceAll("[\\p{Cntrl}\\r\\n\\t]", " ").replaceAll("\\s+", " ").strip();
            if (value.length() > maxLength) {
                value = value.substring(0, maxLength).strip();
            }

            to.addProperty(key, value);
        } catch (Exception var5) {
        }
    }

    static boolean primitive(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive();
    }
}
