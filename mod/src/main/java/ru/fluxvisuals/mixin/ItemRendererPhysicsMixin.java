package ru.fluxvisuals.mixin;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.ItemPhysics;

/**
 * Кладет выброшенный предмет плоско на землю, когда включён Item Physics.
 * Пока {@link ItemEntityRendererPhysicsMixin} убирает вращение, здесь для
 * контекста {@link ItemDisplayContext#GROUND} модель поднимается на 0.47 по Y
 * и поворачивается на 90° по X.
 */
@Environment(EnvType.CLIENT)
@Mixin(ItemRenderer.class)
public abstract class ItemRendererPhysicsMixin {
   @Inject(
         method = "renderItem(Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V",
         at = @At("HEAD")
   )
   private static void fluxvisuals$groundItemPhysics(
         ItemDisplayContext context,
         MatrixStack matrices,
         VertexConsumerProvider provider,
         int light,
         int overlay,
         int[] colors,
         List<BakedQuad> quads,
         RenderLayer renderLayer,
         Glint glint,
         CallbackInfo ci
   ) {
      ItemPhysics module = ItemPhysics.getInstance();
      if (context == ItemDisplayContext.GROUND && module != null && module.enable) {
         matrices.translate(0.0F, 0.47F, 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
      }
   }
}
