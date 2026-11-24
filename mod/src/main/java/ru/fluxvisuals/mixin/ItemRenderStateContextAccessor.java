package ru.fluxvisuals.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the display context for the built-in weapon renderer. */
@Mixin(ItemRenderState.class)
public interface ItemRenderStateContextAccessor {
   @Accessor("displayContext")
   ItemDisplayContext fluxvisuals$displayContext();
}
