package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;

public final class HudItemFilter {
    private static final Set<ItemGuiElementRenderState> HUD_ITEMS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<ItemGuiElementRenderState, Float> HUD_ITEM_OPACITY = new IdentityHashMap<>();
    private static boolean capturing;
    private static boolean activeIsHudItem;
    private static float activeOpacity = 1.0F;

    private HudItemFilter() {
    }

    public static void beginCapture() {
        capturing = true;
    }

    public static void endCapture() {
        capturing = false;
    }

    public static void clearFrame() {
        HUD_ITEMS.clear();
        HUD_ITEM_OPACITY.clear();
        activeIsHudItem = false;
        activeOpacity = 1.0F;
    }

    public static void capture(ItemGuiElementRenderState state) {
        if (capturing) {
            HUD_ITEMS.add(state);
            HUD_ITEM_OPACITY.put(state, Hud.animatedOpacity());
        }
    }

    public static void beginPreparing(ItemGuiElementRenderState state) {
        activeIsHudItem = HUD_ITEMS.remove(state);
        if (!activeIsHudItem) {
            activeOpacity = 1.0F;
            return;
        }
        activeOpacity = HUD_ITEM_OPACITY.remove(state);
        if (Float.isNaN(activeOpacity)) {
            activeOpacity = 1.0F;
        }
    }

    public static void endPreparing() {
        activeIsHudItem = false;
        activeOpacity = 1.0F;
    }

    public static int preparedColor(int original) {
        if (!activeIsHudItem || activeOpacity >= 0.999F) {
            return original;
        }

        int alpha = Math.max(0, Math.min(255, Math.round(activeOpacity * 255.0F)));
        return alpha << 24 | alpha << 16 | alpha << 8 | alpha;
    }
}
