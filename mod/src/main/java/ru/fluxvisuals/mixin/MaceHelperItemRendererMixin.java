package ru.fluxvisuals.mixin;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.visuals.MaceHelper;
import ru.fluxvisuals.util.render.MaceHelperHandConsumer;
import ru.fluxvisuals.util.render.MaceHelperRenderState;

@Environment(EnvType.CLIENT)
@Mixin(ItemRenderer.class)
public abstract class MaceHelperItemRendererMixin {
   private static MaceHelper fluxvisuals$maceHelper() {
      return FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null
         ? FluxVisualsClient.get.manager.get(MaceHelper.class)
         : null;
   }

   @Inject(
      method = "renderItem(Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V",
      at = @At("HEAD")
   )
   private static void fluxvisuals$maceRenderHead(
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
      if (context.isFirstPerson()) {
         MaceHelperRenderState.renderingHand = true;
      }
   }

   @Inject(
      method = "renderItem(Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V",
      at = @At("TAIL")
   )
   private static void fluxvisuals$maceRenderTail(
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
      MaceHelperRenderState.renderingHand = false;
   }

   @ModifyVariable(method = "renderBakedItemQuads", at = @At("HEAD"), argsOnly = true, ordinal = 0, index = 1)
   private static VertexConsumer fluxvisuals$maceVertexConsumer(VertexConsumer original) {
      MaceHelper maceHelper = fluxvisuals$maceHelper();
      if ((MaceHelperRenderState.renderingMace || MaceHelperRenderState.renderingHand)
         && maceHelper != null
         && maceHelper.enable
         && maceHelper.handHighlight.get()
         && MaceHelper.isHoldingMace()) {
         return new MaceHelperHandConsumer(original, MaceHelper.getChargeColor());
      }

      return original;
   }
}
