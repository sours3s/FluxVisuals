package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/** Анимация личного инвентаря: масштаб панели и выезд модели игрока. */
@Environment(EnvType.CLIENT)
@Mixin(InventoryScreen.class)
public abstract class BetterMinecraftInventoryScreenMixin {
   @Unique
   private boolean fluxvisuals$scaled;

   @Inject(method = "render", at = @At("HEAD"))
   private void fluxvisuals$scaleHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      this.fluxvisuals$scaled = false;
      if (!BetterMinecraft.inventoryAnimationEnabled()) {
         return;
      }
      float progress = BetterMinecraft.inventoryProgress();
      if (progress >= 1.0F) {
         return;
      }
      float scale = 0.92F + 0.08F * progress;
      float cx = context.getScaledWindowWidth() / 2.0F;
      float cy = context.getScaledWindowHeight() / 2.0F;
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(cx, cy);
      context.getMatrices().scale(scale, scale);
      context.getMatrices().translate(-cx, -cy);
      this.fluxvisuals$scaled = true;
   }

   @Inject(method = "render", at = @At("RETURN"))
   private void fluxvisuals$scaleTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.fluxvisuals$scaled) {
         context.getMatrices().popMatrix();
         this.fluxvisuals$scaled = false;
      }
   }

   @Redirect(
      method = "drawBackground",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIFFFLnet/minecraft/entity/LivingEntity;)V"
      ),
      require = 0
   )
   private void fluxvisuals$slideEntity(
      DrawContext context, int x1, int y1, int x2, int y2, int size,
      float f, float mouseX, float mouseY, LivingEntity entity
   ) {
      int offset = Math.round(BetterMinecraft.inventorySlideOffset());
      InventoryScreen.drawEntity(context, x1, y1 + offset, x2, y2 + offset, size, f, mouseX, mouseY, entity);
   }
}
