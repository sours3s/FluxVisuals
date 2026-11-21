package ru.fluxvisuals.ui.fluxgui.client.gui.font.icon;

import ru.fluxvisuals.ui.fluxgui.FluxVisuals;
import java.awt.Font;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

public final class TtfIconFonts {
    private static final AtomicInteger GLYPH_TEX_SEQ = new AtomicInteger();
    private static final Map<String, HelsingStyleTtfFont> CACHE = new HashMap<>();
    private static Font baseIcons;
    private static Font baseIcon2;
    private static Font baseCategory;
    private static Font baseGui;
    private static boolean initialized = false;
    private static boolean lazyAttempted = false;

    private TtfIconFonts() {
    }

    public static void init() {
        CACHE.clear();
        baseIcons = load("/assets/fluxvisuals/fonts/icons.ttf", "fonts/icons.ttf");
        baseIcon2 = load("/assets/fluxvisuals/fonts/icon2.ttf", "fonts/icon2.ttf");
        baseCategory = load("/assets/fluxvisuals/fonts/categoryicons.ttf", "fonts/categoryicons.ttf");
        baseGui = load("/assets/fluxvisuals/fonts/guiicons.ttf", "fonts/guiicons.ttf");
        if (baseIcons == null && baseIcon2 == null && baseCategory == null && baseGui == null) {
            FluxVisuals.LOGGER.info("TtfIconFonts: no icon TTFs loaded at init — will retry lazily via ResourceManager.");
        } else {
            initialized = true;
        }
    }

    public static void lazyInit() {
        if (!initialized && !lazyAttempted) {
            lazyAttempted = true;
            FluxVisuals.LOGGER.info("TtfIconFonts: attempting lazy re-init via ResourceManager...");
            if (baseIcons == null) {
                baseIcons = load("/assets/fluxvisuals/fonts/icons.ttf", "fonts/icons.ttf");
            }

            if (baseIcon2 == null) {
                baseIcon2 = load("/assets/fluxvisuals/fonts/icon2.ttf", "fonts/icon2.ttf");
            }

            if (baseCategory == null) {
                baseCategory = load("/assets/fluxvisuals/fonts/categoryicons.ttf", "fonts/categoryicons.ttf");
            }

            if (baseGui == null) {
                baseGui = load("/assets/fluxvisuals/fonts/guiicons.ttf", "fonts/guiicons.ttf");
            }

            if (baseIcons == null && baseIcon2 == null && baseCategory == null && baseGui == null) {
                FluxVisuals.LOGGER.warn("TtfIconFonts: lazy re-init also failed — icons will not render.");
            } else {
                initialized = true;
                FluxVisuals.LOGGER.info("TtfIconFonts: lazy re-init succeeded.");
            }
        }
    }

    private static Font load(String classpathPath, String mcResourcePath) {
        Font font = loadViaClasspath(TtfIconFonts.class.getClassLoader(), classpathPath, "Class classloader");
        if (font != null) {
            return font;
        }

        ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
        if (ctxLoader != null && (font = loadViaClasspath(ctxLoader, classpathPath, "Thread context classloader")) != null) {
            return font;
        }

        font = loadViaClasspath(ClassLoader.getSystemClassLoader(), classpathPath, "System classloader");
        if (font != null) {
            return font;
        }

        font = loadViaMinecraftResourceManager(mcResourcePath);
        if (font != null) {
            return font;
        }

        FluxVisuals.LOGGER.warn("TtfIconFonts: all strategies failed for {}", classpathPath);
        return null;
    }

    private static Font loadViaClasspath(ClassLoader loader, String path, String strategyName) {
        String lookupPath = path.startsWith("/") ? path.substring(1) : path;
        InputStream in = loader.getResourceAsStream(lookupPath);
        if (in == null) {
            return null;
        }

        InputStream inputStream = in;

        try (in) {
            Font f = Font.createFont(0, in);
            FluxVisuals.LOGGER.info("TtfIconFonts: loaded {} via {}", path, strategyName);
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    private static Font loadViaMinecraftResourceManager(String mcResourcePath) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getResourceManager() != null) {
            Identifier id = Identifier.of("fluxvisuals", mcResourcePath);
            Optional res = mc.getResourceManager().getResource(id);
            if (res.isEmpty()) {
                return null;
            }

            try (InputStream in = ((Resource)res.get()).getInputStream()) {
                Font f = Font.createFont(0, in);
                FluxVisuals.LOGGER.info("TtfIconFonts: loaded {} via Minecraft ResourceManager", mcResourcePath);
                return f;
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static Identifier nextGlyphTextureId() {
        return Identifier.of("fluxvisuals", "ttf_glyphs/page_" + GLYPH_TEX_SEQ.incrementAndGet());
    }

    public static boolean hasIcons() {
        return baseIcons != null;
    }

    public static boolean hasIcon2() {
        return baseIcon2 != null;
    }

    public static boolean hasCategory() {
        return baseCategory != null;
    }

    public static boolean hasGui() {
        return baseGui != null;
    }

    public static HelsingStyleTtfFont icons(int logicalSize) {
        return get(baseIcons, logicalSize, "icons");
    }

    public static HelsingStyleTtfFont icon2(int logicalSize) {
        return get(baseIcon2, logicalSize, "icon2");
    }

    public static HelsingStyleTtfFont category(int logicalSize) {
        return get(baseCategory, logicalSize, "category");
    }

    public static HelsingStyleTtfFont gui(int logicalSize) {
        return get(baseGui, logicalSize, "gui");
    }

    private static HelsingStyleTtfFont get(Font base, int logicalSize, String kind) {
        if (base == null) {
            return null;
        }

        int quantizedSize = Math.max(8, Math.min(120, logicalSize + 1 & -2));
        String key = kind + ":" + quantizedSize;
        return CACHE.computeIfAbsent(key, k -> new HelsingStyleTtfFont(base, quantizedSize));
    }
}
