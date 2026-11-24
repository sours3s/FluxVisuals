package ru.fluxvisuals.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.json.Transformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the layer transform for the built-in weapon renderer. */
@Mixin(ItemRenderState.LayerRenderState.class)
public interface ItemRenderStateLayerAccessor {
   @Accessor("transform")
   Transformation fluxvisuals$transform();
}
