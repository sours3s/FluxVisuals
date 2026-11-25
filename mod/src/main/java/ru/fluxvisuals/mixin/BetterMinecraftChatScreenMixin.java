package ru.fluxvisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/** Плавное появление панели ввода чата. */
@Environment(EnvType.CLIENT)
@Mixin(ChatScreen.class)
public abstract class BetterMinecraftChatScreenMixin {
   @WrapOperation(
      method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"),
      require = 0
   )
   private void fluxvisuals$animateBackground(
      DrawContext context, int x0, int y0, int x1, int y1, int color, Operation<Void> original
   ) {
      float displacement = BetterMinecraft.chatFieldDisplacement();
      if (displacement == 0.0F) {
         original.call(context, x0, y0, x1, y1, color);
         return;
      }
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(0.0F, displacement);
      original.call(context, x0, y0, x1, y1, color);
      context.getMatrices().popMatrix();
   }

   @WrapOperation(
      method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"
      ),
      require = 0
   )
   private void fluxvisuals$animateInput(
      ChatScreen instance, DrawContext context, int mouseX, int mouseY, float delta, Operation<Void> original
   ) {
      float displacement = BetterMinecraft.chatFieldDisplacement();
      if (displacement == 0.0F) {
         original.call(instance, context, mouseX, mouseY, delta);
         return;
      }
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(0.0F, displacement);
      original.call(instance, context, mouseX, mouseY, delta);
      context.getMatrices().popMatrix();
   }

   @Inject(method = "removed()V", at = @At("HEAD"), require = 0)
   private void fluxvisuals$onClosed(CallbackInfo ci) {
      BetterMinecraft.onChatScreenClosed();
   }
}
