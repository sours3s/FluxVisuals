package ru.fluxvisuals.ui.gui.clickgui.util.type;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ItemSpriteTextures;

import net.minecraft.item.ItemStack;

/**
 * Stub: the donor ItemSpriteManager referenced the pre-1.21.11 item-model API
 * (BakedModel, ModelTransformationMode, client-side SkinTextures) which was removed/renamed
 * in 1.21.11. DrawEngine.itemStack() only needs getSpriteTextures(ItemStack); returning null
 * makes DrawEngine treat the item sprite as unavailable (no-op render).
 */
public enum ItemSpriteManager {
    INSTANCE;

    public ItemSpriteTextures getSpriteTextures(ItemStack itemStack) {
        return null;
    }
}