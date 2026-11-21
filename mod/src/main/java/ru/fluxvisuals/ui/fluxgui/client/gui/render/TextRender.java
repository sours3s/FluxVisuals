package ru.fluxvisuals.ui.fluxgui.client.gui.render;

import ru.fluxvisuals.ui.fluxgui.FluxVisuals;
import ru.fluxvisuals.ui.fluxgui.compat.Gui;
import ru.fluxvisuals.ui.fluxgui.compat.Gui$Tex;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

public final class TextRender {
    public static final float TRACKING = -0.0246F;
    static final Identifier ATLAS = Identifier.of("fluxvisuals", "fonts/msdf/text.png");
    static final Identifier LAYOUT = Identifier.of("fluxvisuals", "fonts/msdf/text.json");
    static final Map<Integer, TextRender$Glyph> GLYPHS = new HashMap<>();
    static final Map<Integer, Float> KERNING = new HashMap<>();
    static final int WIDTH_CACHE_LIMIT = 2048;
    static final Map<String, Float> WIDTH_CACHE = new TextRender$1(256, 0.75F, true);
    static boolean loaded;
    static int brightTextDepth;

    TextRender() {
    }

    public static void beginBrightText() {
        brightTextDepth++;
    }

    public static void endBrightText() {
        brightTextDepth = Math.max(0, brightTextDepth - 1);
    }

    public static void reload() {
        GLYPHS.clear();
        KERNING.clear();
        WIDTH_CACHE.clear();
        loaded = false;
    }

    public static void draw(DrawContext context, String text, float x, float baselineY, float size, int color) {
        ensureLoaded();
        if (text != null && !text.isEmpty() && !(size <= 0.0F)) {
            if (brightTextDepth > 0) {
                color = color & 0xFF000000 | 16777215;
            }

            Gui$Tex batch = Gui.tex(context, ATLAS, Gui.GUI_MSDF, Math.max(4, text.length() * 4));
            List<TextRender$EmojiPlacement> emojiPlacements = null;
            float pen = x;
            int previous = -1;
            boolean any = false;
            int index = 0;

            while (index < text.length()) {
                int code = text.codePointAt(index);
                index += Character.charCount(code);
                if (!EmojiRender.isVariationSelector(code)) {
                    if (EmojiRender.contains(code)) {
                        float emojiSize = size * 0.92F;
                        if (emojiPlacements == null) {
                            emojiPlacements = new ArrayList<>(2);
                        }

                        emojiPlacements.add(new TextRender$EmojiPlacement(code, pen, baselineY - size * 0.8F, emojiSize));
                        pen += EmojiRender.advance(size);
                        previous = -1;
                    } else {
                        TextRender$Glyph glyph = GLYPHS.get(code);
                        if (glyph == null) {
                            previous = -1;
                        } else {
                            if (previous != -1) {
                                pen += kerning(previous, code) * size;
                            }

                            if (glyph.width > 0.0F) {
                                float x0 = pen + glyph.planeLeft * size;
                                float y0 = baselineY + glyph.planeTop * size;
                                float x1 = pen + glyph.planeRight * size;
                                float y1 = baselineY + glyph.planeBottom * size;
                                batch.vertex(x0, y1, glyph.u0, glyph.v1, color)
                                    .vertex(x1, y1, glyph.u1, glyph.v1, color)
                                    .vertex(x1, y0, glyph.u1, glyph.v0, color)
                                    .vertex(x0, y0, glyph.u0, glyph.v0, color);
                                any = true;
                            }

                            pen += (glyph.advance + -0.0246F) * size;
                            previous = code;
                        }
                    }
                }
            }

            if (any) {
                batch.submit(context);
            }

            if (emojiPlacements != null) {
                for (TextRender$EmojiPlacement emoji : emojiPlacements) {
                    EmojiRender.draw(context, emoji.codepoint, emoji.x, emoji.y, emoji.size, color);
                }
            }
        }
    }

    public static float width(String text, float size) {
        ensureLoaded();
        if (text != null && !text.isEmpty()) {
            Float cached = WIDTH_CACHE.get(text);
            if (cached != null) {
                return cached * size;
            }

            float pen = 0.0F;
            int previous = -1;
            int index = 0;

            while (index < text.length()) {
                int code = text.codePointAt(index);
                index += Character.charCount(code);
                if (!EmojiRender.isVariationSelector(code)) {
                    if (EmojiRender.contains(code)) {
                        pen += EmojiRender.advance(1.0F);
                        previous = -1;
                    } else {
                        TextRender$Glyph glyph = GLYPHS.get(code);
                        if (glyph == null) {
                            previous = -1;
                        } else {
                            if (previous != -1) {
                                pen += kerning(previous, code);
                            }

                            pen += glyph.advance + -0.0246F;
                            previous = code;
                        }
                    }
                }
            }

            float unitWidth = Math.max(0.0F, pen - -0.0246F);
            WIDTH_CACHE.put(text, unitWidth);
            return unitWidth * size;
        } else {
            return 0.0F;
        }
    }

    static float kerning(int first, int second) {
        Float value = KERNING.get(first << 16 | second);
        return value == null ? 0.0F : value;
    }

    static void ensureLoaded() {
        if (!loaded) {
            loaded = true;

            try {
                Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(LAYOUT);
                if (resource.isEmpty()) {
                    FluxVisuals.LOGGER.error("Missing MSDF font layout {}", LAYOUT);
                    return;
                }

                try (BufferedReader reader = resource.get().getReader()) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject atlas = root.getAsJsonObject("atlas");
                    float atlasWidth = atlas.get("width").getAsFloat();
                    float atlasHeight = atlas.get("height").getAsFloat();

                    for (JsonElement element : root.getAsJsonArray("glyphs")) {
                        JsonObject glyph = element.getAsJsonObject();
                        int code = glyph.get("unicode").getAsInt();
                        float advance = glyph.get("advance").getAsFloat();
                        if (!glyph.has("planeBounds")) {
                            GLYPHS.put(code, new TextRender$Glyph(advance, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                        } else {
                            JsonObject plane = glyph.getAsJsonObject("planeBounds");
                            JsonObject bounds = glyph.getAsJsonObject("atlasBounds");
                            GLYPHS.put(
                                code,
                                new TextRender$Glyph(
                                    advance,
                                    plane.get("left").getAsFloat(),
                                    plane.get("top").getAsFloat(),
                                    plane.get("right").getAsFloat(),
                                    plane.get("bottom").getAsFloat(),
                                    bounds.get("left").getAsFloat() / atlasWidth,
                                    bounds.get("top").getAsFloat() / atlasHeight,
                                    bounds.get("right").getAsFloat() / atlasWidth,
                                    bounds.get("bottom").getAsFloat() / atlasHeight,
                                    bounds.get("right").getAsFloat() - bounds.get("left").getAsFloat()
                                )
                            );
                        }
                    }

                    JsonArray kerning = root.getAsJsonArray("kerning");

                    for (int index = 0; index + 2 < kerning.size(); index += 3) {
                        KERNING.put(kerning.get(index).getAsInt() << 16 | kerning.get(index + 1).getAsInt(), kerning.get(index + 2).getAsFloat());
                    }
                }
            } catch (Exception var15) {
                FluxVisuals.LOGGER.error("Failed to read the MSDF font layout", var15);
            }
        }
    }
}
