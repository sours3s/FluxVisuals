package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.Remove;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.AllowMouseClick;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.AllowMouseScroll;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.BeforeMouseDrag;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.BeforeMouseRelease;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;

public final class HudEditor {
    private static final Map<String, float[]> OFFSETS = new HashMap<>();
    private static final Map<String, Float> SCALES = new HashMap<>();
    private static final Map<String, Float> ANIMATED_SCALES = new HashMap<>();
    private static final float MIN_SCALE = 0.6F;
    private static final float MAX_SCALE = 1.6F;
    private static final float SCALE_STEP = 0.05F;
    private static final float SNAP_DISTANCE = 5.5F;
    private static Hud hud;
    private static HudElement dragging;
    private static float rawOffsetX;
    private static float rawOffsetY;
    private static boolean freeDrag;
    private static boolean pendingWatermarkBossOffset;
    private static Float guideX;
    private static Float guideY;
    private static boolean installed;

    private HudEditor() {
    }

    public static void install(Hud target) {
        hud = target;
        if (!installed) {
            installed = true;
            ScreenEvents.AFTER_INIT.register((AfterInit)(client, screen, width, height) -> {
                if (screen instanceof ChatScreen) {
                    ScreenMouseEvents.allowMouseClick(screen).register((AllowMouseClick)(s, click) -> !handleChatMouseClick(click));
                    ScreenMouseEvents.allowMouseScroll(screen).register((AllowMouseScroll)(s, x, y, horizontal, vertical) -> !scaleAt(x, y, vertical));
                    ScreenMouseEvents.beforeMouseDrag(screen).register((BeforeMouseDrag)(s, click, deltaX, deltaY) -> {
                        if (click.button() == 0) {
                            drag(deltaX, deltaY);
                        }
                    });
                    ScreenMouseEvents.beforeMouseRelease(screen).register((BeforeMouseRelease)(s, click) -> {
                        if (click.button() == 0) {
                            stopDragging();
                        }
                    });
                    ScreenEvents.remove(screen).register((Remove)s -> stopDragging());
                }
            });
        }
    }

    public static boolean handleChatMouseClick(Click click) {
        return click.button() == 0 && beginDrag(click.comp_4798(), click.comp_4799());
    }

    public static boolean handleChatMouseDrag(Click click, double deltaX, double deltaY) {
        if (click.button() == 0 && dragging != null) {
            drag(deltaX, deltaY);
            return true;
        } else {
            return false;
        }
    }

    public static boolean handleChatMouseRelease(Click click) {
        if (click.button() == 0 && dragging != null) {
            stopDragging();
            return true;
        } else {
            return false;
        }
    }

    public static float[] offset(String id) {
        if (CustomHotbar.INSTANCE.id().equals(id)) {
            float[] fixed = OFFSETS.computeIfAbsent(id, key -> new float[2]);
            fixed[0] = 0.0F;
            fixed[1] = 0.0F;
            return fixed;
        } else {
            return OFFSETS.computeIfAbsent(id, key -> new float[2]);
        }
    }

    public static float scale(String id) {
        if (CustomHotbar.INSTANCE.id().equals(id)) {
            SCALES.put(id, 1.0F);
            ANIMATED_SCALES.put(id, 1.0F);
            return 1.0F;
        } else {
            float target = SCALES.getOrDefault(id, 1.0F);
            float current = ANIMATED_SCALES.getOrDefault(id, target);
            current = MathHelper.lerp(0.22F, current, target);
            ANIMATED_SCALES.put(id, current);
            return current;
        }
    }

    public static boolean isDragging(String id) {
        return dragging != null && dragging.id().equals(id);
    }

    public static boolean isDragging() {
        return dragging != null;
    }

    public static Float guideX() {
        return guideX;
    }

    public static Float guideY() {
        return guideY;
    }

    public static float[] boundsFor(HudElement element) {
        return bounds(element);
    }

    public static JsonObject saveLayout() {
        JsonObject layout = new JsonObject();

        for (HudElement element : Hud.elements()) {
            JsonObject value = new JsonObject();
            float[] elementOffset = element == CustomHotbar.INSTANCE ? new float[2] : OFFSETS.getOrDefault(element.id(), new float[2]);
            value.addProperty("x", elementOffset[0]);
            value.addProperty("y", elementOffset[1]);
            value.addProperty("scale", element == CustomHotbar.INSTANCE ? 1.0F : SCALES.getOrDefault(element.id(), 1.0F));
            layout.add(element.id(), value);
        }

        return layout;
    }

    public static void loadLayout(JsonObject layout) {
        if (layout != null) {
            OFFSETS.clear();
            SCALES.clear();
            ANIMATED_SCALES.clear();

            for (HudElement element : Hud.elements()) {
                if (element == CustomHotbar.INSTANCE) {
                    OFFSETS.put(element.id(), new float[2]);
                    SCALES.put(element.id(), 1.0F);
                    ANIMATED_SCALES.put(element.id(), 1.0F);
                } else {
                    JsonElement stored = layout.get(element.id());
                    if (stored != null && stored.isJsonObject()) {
                        JsonObject value = stored.getAsJsonObject();
                        float x = finite(value, "x", 0.0F);
                        float y = finite(value, "y", 0.0F);
                        float elementScale = MathHelper.clamp(finite(value, "scale", 1.0F), 0.6F, 1.6F);
                        OFFSETS.put(element.id(), new float[]{x, y});
                        SCALES.put(element.id(), elementScale);
                        ANIMATED_SCALES.put(element.id(), elementScale);
                        if (hud != null) {
                            clampToScreen(element, OFFSETS.get(element.id()));
                        }
                    }
                }
            }
        }
    }

    private static float finite(JsonObject object, String name, float fallback) {
        try {
            float value = object.has(name) ? object.get(name).getAsFloat() : fallback;
            return Float.isFinite(value) ? value : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static HudElement hovered() {
        if (hud == null) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        double screenX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double screenY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
        float designX = (float)((screenX - hud.originX) / hud.scale);
        float designY = (float)((screenY - hud.originY) / hud.scale);
        HudElement[] elements = Hud.elements();

        for (int index = elements.length - 1; index >= 0; index--) {
            float[] bounds;
            if (elements[index] != CustomHotbar.INSTANCE
                && Hud.elementEnabled(elements[index])
                && designX >= (bounds = bounds(elements[index]))[0]
                && designX <= bounds[0] + bounds[2]
                && designY >= bounds[1]
                && designY <= bounds[1] + bounds[3]) {
                return elements[index];
            }
        }

        return null;
    }

    private static boolean beginDrag(double screenX, double screenY) {
        if (hud == null) {
            return false;
        }

        float designX = (float)((screenX - hud.originX) / hud.scale);
        float designY = (float)((screenY - hud.originY) / hud.scale);
        HudElement[] elements = Hud.elements();

        for (int index = elements.length - 1; index >= 0; index--) {
            HudElement element = elements[index];
            float[] bounds;
            if (element != CustomHotbar.INSTANCE
                && Hud.elementEnabled(element)
                && designX >= (bounds = bounds(element))[0]
                && designX <= bounds[0] + bounds[2]
                && designY >= bounds[1]
                && designY <= bounds[1] + bounds[3]) {
                dragging = element;
                float[] elementOffset = offset(element.id());
                rawOffsetX = elementOffset[0];
                rawOffsetY = elementOffset[1];
                pendingWatermarkBossOffset = element == Watermark.INSTANCE;
                freeDrag = false;
                guideX = null;
                guideY = null;
                return true;
            }
        }

        return false;
    }

    private static void drag(double deltaX, double deltaY) {
        if (dragging != null && hud != null) {
            if (pendingWatermarkBossOffset) {
                float bakedOffset = Watermark.beginManualMove();
                rawOffsetY += bakedOffset;
                float[] fArray = offset(dragging.id());
                fArray[1] += bakedOffset;
                pendingWatermarkBossOffset = false;
            }

            freeDrag = freeDrag | MinecraftClient.getInstance().isCtrlPressed();
            float[] rawOffset = new float[]{rawOffsetX = rawOffsetX + (float)(deltaX / hud.scale), rawOffsetY = rawOffsetY + (float)(deltaY / hud.scale)};
            clampToScreen(dragging, rawOffset);
            rawOffsetX = rawOffset[0];
            rawOffsetY = rawOffset[1];
            float[] offset = offset(dragging.id());
            offset[0] = rawOffsetX;
            offset[1] = rawOffsetY;
            if (freeDrag) {
                guideX = null;
                guideY = null;
            } else {
                snapToGuides(offset);
            }

            clampToScreen(dragging, offset);
        }
    }

    private static boolean scaleAt(double screenX, double screenY, double amount) {
        if (hud != null && amount != 0.0) {
            float designX = (float)((screenX - hud.originX) / hud.scale);
            float designY = (float)((screenY - hud.originY) / hud.scale);
            HudElement[] elements = Hud.elements();

            for (int index = elements.length - 1; index >= 0; index--) {
                HudElement element = elements[index];
                float[] bounds;
                if (element != CustomHotbar.INSTANCE
                    && Hud.elementEnabled(element)
                    && !(designX < (bounds = bounds(element))[0])
                    && !(designX > bounds[0] + bounds[2])
                    && !(designY < bounds[1])
                    && !(designY > bounds[1] + bounds[3])) {
                    float oldScale = SCALES.getOrDefault(element.id(), 1.0F);
                    float newScale = MathHelper.clamp(oldScale + (amount > 0.0 ? 0.05F : -0.05F), 0.6F, 1.6F);
                    SCALES.put(element.id(), newScale);
                    clampToScreen(element, offset(element.id()));
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private static void snapToGuides(float[] offset) {
        guideX = null;
        guideY = null;
        float centerX = dragging.x() + offset[0] + dragging.width() / 2.0F;
        float centerY = dragging.y() + offset[1] + dragging.height() / 2.0F;
        if (Math.abs(centerX - 960.0F) <= 5.5F) {
            offset[0] += 960.0F - centerX;
            guideX = 960.0F;
        }

        if (Math.abs(centerY - 540.0F) <= 5.5F) {
            offset[1] += 540.0F - centerY;
            guideY = 540.0F;
        }

        float draggedScale = SCALES.getOrDefault(dragging.id(), 1.0F);
        float draggedHeight = dragging.height() * draggedScale;
        float draggedTop = dragging.y() + offset[1] + (dragging.height() - draggedHeight) / 2.0F;
        float bestDistance = 6.5F;
        float bestDelta = 0.0F;
        float bestGuide = 0.0F;

        for (HudElement other : Hud.elements()) {
            float[] otherBounds;
            float distance;
            if (other != dragging
                && Hud.elementEnabled(other)
                && (distance = Math.abs(draggedTop - (otherBounds = bounds(other))[1])) <= 5.5F
                && distance < bestDistance) {
                bestDistance = distance;
                bestDelta = otherBounds[1] - draggedTop;
                bestGuide = otherBounds[1];
            }
        }

        if (bestDistance <= 5.5F && guideY == null) {
            offset[1] += bestDelta;
            guideY = bestGuide;
        }
    }

    private static float[] bounds(HudElement element) {
        float[] offset = offset(element.id());
        float elementScale = ANIMATED_SCALES.getOrDefault(element.id(), SCALES.getOrDefault(element.id(), 1.0F));
        float width = element.width() * elementScale;
        float height = element.height() * elementScale;
        return new float[]{
            element.x() + offset[0] + (element.width() - width) / 2.0F,
            element.y() + offset[1] + Hud.automaticYOffset(element) + (element.height() - height) / 2.0F,
            width,
            height
        };
    }

    private static void clampToScreen(HudElement element, float[] offset) {
        float elementScale = SCALES.getOrDefault(element.id(), 1.0F);
        float scaledWidth = element.width() * elementScale;
        float scaledHeight = element.height() * elementScale;
        float insetX = (element.width() - scaledWidth) / 2.0F;
        float insetY = (element.height() - scaledHeight) / 2.0F;
        offset[0] = MathHelper.clamp(offset[0], -element.x() - insetX, 1920.0F - element.x() - insetX - scaledWidth);
        offset[1] = MathHelper.clamp(offset[1], -element.y() - insetY, 1080.0F - element.y() - insetY - scaledHeight);
    }

    private static void stopDragging() {
        dragging = null;
        freeDrag = false;
        pendingWatermarkBossOffset = false;
        guideX = null;
        guideY = null;
    }
}
