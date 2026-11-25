package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/** Плавное открытие контейнеров и «выезд» предметов по слотам. */
@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class BetterMinecraftHandleScreenMixin extends Screen {
   @Unique
   private boolean fluxvisuals$scaled;

   @Unique
   private boolean fluxvisuals$slotMoved;

   protected BetterMinecraftHandleScreenMixin(Text title) {
      super(title);
   }

   @Inject(method = "init", at = @At("TAIL"))
   private void fluxvisuals$markOpened(CallbackInfo ci) {
      BetterMinecraft.markInventoryOpen();
   }

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

   @Inject(method = "drawSlot", at = @At("HEAD"))
   private void fluxvisuals$slotHead(DrawContext context, Slot slot, int x, int y, CallbackInfo ci) {
      this.fluxvisuals$slotMoved = false;
      float offset = BetterMinecraft.itemSlotOffset(slot.id);
      if (offset == 0.0F) {
         return;
      }
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(0.0F, offset);
      this.fluxvisuals$slotMoved = true;
   }

   @Inject(method = "drawSlot", at = @At("RETURN"))
   private void fluxvisuals$slotTail(DrawContext context, Slot slot, int x, int y, CallbackInfo ci) {
      if (this.fluxvisuals$slotMoved) {
         context.getMatrices().popMatrix();
         this.fluxvisuals$slotMoved = false;
      }
   }
}
