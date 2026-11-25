package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/** Плавная рамка хотбара и его подъём при открытом чате. */
@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class BetterMinecraftHotbarMixin {
   @Unique
   private boolean fluxvisuals$hotbarLifted;

   @ModifyArg(
      method = "renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V",
         ordinal = 1
      ),
      index = 2,
      require = 0
   )
   private int fluxvisuals$smoothSelection(int x) {
      return BetterMinecraft.animateHotbarSelectionX(x);
   }

   @Inject(
      method = "renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At("HEAD")
   )
   private void fluxvisuals$liftHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      this.fluxvisuals$hotbarLifted = false;
      float offset = BetterMinecraft.chatHotbarLiftOffset();
      if (offset == 0.0F) {
         return;
      }
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(0.0F, -offset);
      this.fluxvisuals$hotbarLifted = true;
   }

   @Inject(
      method = "renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At("RETURN")
   )
   private void fluxvisuals$liftTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (this.fluxvisuals$hotbarLifted) {
         context.getMatrices().popMatrix();
         this.fluxvisuals$hotbarLifted = false;
      }
   }
}
