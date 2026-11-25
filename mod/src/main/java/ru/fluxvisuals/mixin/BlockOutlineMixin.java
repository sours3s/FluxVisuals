package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.BlockOutline;

/** Отменяет ванильный контур блока — модуль рисует свой. */
@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public abstract class BlockOutlineMixin {
   @Inject(
         method = "drawBlockOutline(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;DDDLnet/minecraft/client/render/state/OutlineRenderState;IF)V",
         at = @At("HEAD"),
         cancellable = true
   )
   private void fluxvisuals$cancelVanillaOutline(
         MatrixStack matrices,
         VertexConsumer vertexConsumer,
         double x,
         double y,
         double z,
         OutlineRenderState state,
         int light,
         float alpha,
         CallbackInfo ci
   ) {
      BlockOutline module = BlockOutline.getInstance();
      if (module != null && module.enable) {
         ci.cancel();
      }
   }
}
