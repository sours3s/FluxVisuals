package ru.fluxvisuals.ui.gui.clickgui.externall.theme;

import com.google.gson.*;
import ru.fluxvisuals.ui.gui.clickgui.externall.colors.ColorUtil;

import java.awt.Desktop;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    private static final Path THEME_DIR = Path.of("C:/Exclusive/client1_21_11/theme");
    private static final Path ACTIVE_FILE = THEME_DIR.resolve(".active");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ThemeManager instance;

    public static final int MAX_THEMES = 32;

    private final List<Theme> themes = new ArrayList<>();
    private Theme active;

    public static ThemeManager get() {
        if (instance == null) {
            instance = new ThemeManager();
            instance.load();
        }
        return instance;
    }

    public List<Theme> getThemes() {
        return themes;
    }

    public Theme getActive() {
        if (active == null) {
            active = themes.isEmpty() ? createPreset("Фиолетовый", 0xFF8B5CF6) : themes.get(0);
            if (themes.isEmpty()) themes.add(active);
        }
        return active;
    }

    public void setActive(Theme theme) {
        if (theme == null) return;
        this.active = theme;
        saveActiveName();
    }

    public boolean canCreate() {
        return themes.size() < MAX_THEMES;
    }

    public Theme createTheme(String name) {
        if (!canCreate()) return getActive();
        name = uniqueName(name, null);
        Theme base = getActive();
        Theme created = base.copy(name);
        created.builtin = false;
        themes.add(created);
        setActive(created);
        saveTheme(created);
        return created;
    }

    public Theme duplicateTheme(Theme source) {
        if (source == null || !canCreate()) return getActive();
        String newName = uniqueName(source.name + " копия", null);
        Theme duplicated = source.copy(newName);
        duplicated.builtin = false;
        themes.add(duplicated);
        setActive(duplicated);
        saveTheme(duplicated);
        return duplicated;
    }

    public void renameTheme(Theme theme, String newName) {
        if (theme == null || theme.builtin) return;
        newName = newName == null ? "" : newName.trim();
        if (newName.isEmpty() || newName.equalsIgnoreCase(theme.name)) return;
        try {
            Files.deleteIfExists(THEME_DIR.resolve(theme.name + ".json"));
        } catch (IOException ignored) {}
        theme.name = uniqueName(newName, theme);
        saveTheme(theme);
        if (active == theme) saveActiveName();
    }

    public void deleteTheme(Theme theme) {
        if (theme == null || theme.builtin || themes.size() <= 1) return;
        themes.remove(theme);
        try {
            Files.deleteIfExists(THEME_DIR.resolve(theme.name + ".json"));
        } catch (IOException ignored) {}
        if (active == theme) {
            active = themes.get(0);
            saveActiveName();
        }
    }

    public void openFolder() {
        try {
            Files.createDirectories(THEME_DIR);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(THEME_DIR.toFile());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Theme createPreset(String name, int color) {
        Theme t = new Theme(name);
        t.builtin = true;
        t.colors[Theme.ACCENT]        = color;
        t.colors[Theme.MENU_ACCENT_1] = color;
        t.colors[Theme.MENU_ACCENT_2] = color;
        t.colors[Theme.PANEL_ACCENT]  = color;
        t.colors[Theme.PANEL_BG]      = 0xFF15141C;
        t.colors[Theme.GLASS]         = Theme.rgba(20, 19, 28, 180);
        t.colors[Theme.GLASS_OUTLINE] = Theme.rgba(255, 255, 255, 30);
        t.colors[Theme.HUD_ACCENT]    = color;
        t.colors[Theme.HUD_BG]        = 0xFF0E0D13;
        t.colors[Theme.HUD_TEXT]      = 0xFFFFFFFF;
        t.colors[Theme.HUD_OUTLINE]   = Theme.rgba(255, 255, 255, 25);
        t.colors[Theme.TEXT]          = 0xFFFFFFFF;
        t.colors[Theme.TEXT2]         = 0xFF8A8997;
        t.colors[Theme.BACKGROUND]    = 0xFF0E0E12;
        t.colors[Theme.OUTLINE]       = Theme.rgba(255, 255, 255, 30);
        t.colors[Theme.SEPARATOR]     = Theme.rgba(255, 255, 255, 20);
        t.setSlider(Theme.OPACITY, 0.9F);
        t.setSlider(Theme.BLUR, 4.0F);
        t.setBool(Theme.RAINBOW, false);
        t.setBool(Theme.SHADOW, true);
        t.setBool(Theme.DISTORTION, false);
        return t;
    }

    public void load() {
        themes.clear();

        themes.add(createPreset("Фиолетовый", 0xFF8B5CF6));
        themes.add(createPreset("Синий",       0xFF3B82F6));
        themes.add(createPreset("Голубой",     0xFF06B6D4));
        themes.add(createPreset("Бирюзовый",   0xFF14B8A6));
        themes.add(createPreset("Зеленый",     0xFF10B981));
        themes.add(createPreset("Лаймовый",    0xFF84CC16));
        themes.add(createPreset("Желтый",      0xFFEAB308));
        themes.add(createPreset("Оранжевый",   0xFFF97316));
        themes.add(createPreset("Красный",     0xFFEF4444));
        themes.add(createPreset("Малиновый",   0xFFE11D48));
        themes.add(createPreset("Розовый",     0xFFEC4899));
        themes.add(createPreset("Сиреневый",   0xFFC084FC));
        themes.add(createPreset("Лавандовый",  0xFFA855F7));
        themes.add(createPreset("Коралловый",  0xFFFF6F61));
        themes.add(createPreset("Мятный",      0xFF3EB489));
        themes.add(createPreset("Неоновый",    0xFF00F0FF));
        themes.add(createPreset("Золотой",     0xFFFFD700));
        themes.add(createPreset("Аквамарин",   0xFF7FFFD4));
        themes.add(createPreset("Индиго",      0xFF6366F1));
        themes.add(createPreset("Белый",       0xFFFFFFFF));

        try {
            Files.createDirectories(THEME_DIR);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(THEME_DIR, "*.json")) {
                for (Path path : stream) {
                    Theme t = readTheme(path);
                    if (t != null) {
                        Theme existing = themes.stream().filter(th -> th.name.equalsIgnoreCase(t.name)).findFirst().orElse(null);
                        if (existing != null) {
                            System.arraycopy(t.colors, 0, existing.colors, 0, Math.min(t.colors.length, existing.colors.length));
                            System.arraycopy(t.sliders, 0, existing.sliders, 0, Math.min(t.sliders.length, existing.sliders.length));
                            System.arraycopy(t.booleans, 0, existing.booleans, 0, Math.min(t.booleans.length, existing.booleans.length));
                        } else {
                            t.builtin = false;
                            themes.add(t);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String activeName = readActiveName();
        active = themes.stream()
                .filter(t -> t.name.equals(activeName))
                .findFirst()
                .orElse(themes.get(0));
    }

    public void saveTheme(Theme theme) {
        if (theme == null) return;
        try {
            Files.createDirectories(THEME_DIR);
            JsonObject root = new JsonObject();
            root.addProperty("name", theme.name);
            for (int i = 0; i < Theme.COLOR_COUNT; i++) {
                root.addProperty("c" + i, theme.colors[i]);
            }
            for (int i = 0; i < theme.sliders.length; i++) {
                root.addProperty("s" + i, theme.sliders[i]);
            }
            for (int i = 0; i < theme.booleans.length; i++) {
                root.addProperty("b" + i, theme.booleans[i]);
            }
            Path file = THEME_DIR.resolve(theme.name + ".json");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file.toFile()), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveActive() {
        saveTheme(getActive());
    }

    private Theme readTheme(Path path) {
        try (Reader reader = new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            String name = root.has("name") ? root.get("name").getAsString() : stripExt(path.getFileName().toString());
            Theme t = new Theme(name);
            for (int i = 0; i < Theme.COLOR_COUNT; i++) {
                if (root.has("c" + i)) t.colors[i] = root.get("c" + i).getAsInt();
            }
            for (int i = 0; i < t.sliders.length; i++) {
                if (root.has("s" + i)) t.setSlider(i, root.get("s" + i).getAsFloat());
            }
            for (int i = 0; i < t.booleans.length; i++) {
                if (root.has("b" + i)) t.setBool(i, root.get("b" + i).getAsBoolean());
            }
            return t;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveActiveName() {
        try {
            Files.createDirectories(THEME_DIR);
            Files.write(ACTIVE_FILE, getActive().name.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
    }

    private String readActiveName() {
        try {
            if (Files.exists(ACTIVE_FILE)) {
                return new String(Files.readAllBytes(ACTIVE_FILE), StandardCharsets.UTF_8).trim();
            }
        } catch (IOException ignored) {}
        return null;
    }

    private String uniqueName(String base, Theme exclude) {
        if (base == null || base.isBlank()) base = "Тема";
        base = base.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (base.isEmpty()) base = "Тема";
        String name = base;
        int n = 1;
        while (nameExists(name, exclude)) {
            name = base + " " + (++n);
        }
        return name;
    }

    private boolean nameExists(String name, Theme exclude) {
        return themes.stream().anyMatch(t -> t != exclude && t.name.equalsIgnoreCase(name));
    }

    private static String stripExt(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
