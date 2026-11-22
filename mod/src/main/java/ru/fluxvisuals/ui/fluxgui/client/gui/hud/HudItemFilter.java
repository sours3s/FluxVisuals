package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import ru.fluxvisuals.mixin.accessor.AbstractTextureAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Atlases;

public final class HudItemFilter {
    private static final Set<ItemGuiElementRenderState> HUD_ITEMS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<ItemGuiElementRenderState, Float> HUD_ITEM_OPACITY = new IdentityHashMap<>();
    private static boolean capturing;
    private static AbstractTexture activeAtlas;
    private static GpuSampler previousSampler;
    private static AbstractTexture cachedItemAtlas;
    private static GpuSampler cachedLinearSampler;
    private static float activeOpacity = 1.0F;

    private HudItemFilter() {
    }

    public static void beginCapture() {
        capturing = true;
    }

    public static void endCapture() {
        capturing = false;
    }

    public static void capture(ItemGuiElementRenderState state) {
        if (capturing) {
            HUD_ITEMS.add(state);
            HUD_ITEM_OPACITY.put(state, Hud.animatedOpacity());
        }
    }

    public static void beginPreparing(ItemGuiElementRenderState state) {
        if (!HUD_ITEMS.remove(state)) {
            activeOpacity = 1.0F;
        } else {
            activeOpacity = HUD_ITEM_OPACITY.remove(state);
            if (cachedItemAtlas == null) {
                cachedItemAtlas = MinecraftClient.getInstance().getAtlasManager().getAtlasTexture(Atlases.ITEMS);
            }

            if (cachedLinearSampler == null) {
                cachedLinearSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            }

            if ((previousSampler = (activeAtlas = cachedItemAtlas).getSampler()) != cachedLinearSampler) {
                ((AbstractTextureAccessor)activeAtlas).fluxvisuals$setSampler(cachedLinearSampler);
            }
        }
    }

    public static void endPreparing() {
        if (activeAtlas == null) {
            activeOpacity = 1.0F;
        } else {
            if (previousSampler != cachedLinearSampler) {
                ((AbstractTextureAccessor)activeAtlas).fluxvisuals$setSampler(previousSampler);
            }

            activeAtlas = null;
            previousSampler = null;
            activeOpacity = 1.0F;
        }
    }

    public static int preparedColor(int original) {
        if (activeOpacity >= 0.999F) {
            return original;
        }

        int alpha = Math.max(0, Math.min(255, Math.round(activeOpacity * 255.0F)));
        return alpha << 24 | alpha << 16 | alpha << 8 | alpha;
    }
}
