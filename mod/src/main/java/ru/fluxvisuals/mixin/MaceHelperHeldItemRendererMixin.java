package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.MaceHelper;
import ru.fluxvisuals.util.render.MaceHelperRenderState;

@Environment(EnvType.CLIENT)
@Mixin(HeldItemRenderer.class)
public abstract class MaceHelperHeldItemRendererMixin {
   @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
   private void fluxvisuals$maceHandHead(
      AbstractClientPlayerEntity player,
      float tickProgress,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      MatrixStack matrices,
      OrderedRenderCommandQueue queue,
      int light,
      CallbackInfo ci
   ) {
      MaceHelperRenderState.renderingHand = true;
      MaceHelperRenderState.renderingMace = MaceHelper.isMace(stack);
   }

   @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
   private void fluxvisuals$maceHandReturn(
      AbstractClientPlayerEntity player,
      float tickProgress,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      MatrixStack matrices,
      OrderedRenderCommandQueue queue,
      int light,
      CallbackInfo ci
   ) {
      MaceHelperRenderState.renderingHand = false;
      MaceHelperRenderState.renderingMace = false;
   }
}
