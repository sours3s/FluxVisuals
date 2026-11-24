package ru.fluxvisuals.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the first item layer transform for the built-in weapon renderer. */
@Mixin(ItemRenderState.class)
public interface ItemRenderStateAccessor {
   @Accessor("layers")
   ItemRenderState.LayerRenderState[] fluxvisuals$layers();
}
