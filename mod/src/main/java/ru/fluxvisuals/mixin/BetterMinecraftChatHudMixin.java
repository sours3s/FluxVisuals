package ru.fluxvisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/**
 * Новые сообщения появляются чуть ниже и плавно поднимаются на своё место.
 * Прокрученный чат не анимируется, чтобы не дёргать историю.
 */
@Environment(EnvType.CLIENT)
@Mixin(ChatHud.class)
public abstract class BetterMinecraftChatHudMixin {
   @Shadow
   private int scrolledLines;

   @Shadow
   public abstract int getLineHeight();

   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("TAIL"), require = 0)
   private void fluxvisuals$onMessage(Text text, CallbackInfo ci) {
      BetterMinecraft.onChatMessage();
   }

   @Inject(
      method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
      at = @At("TAIL"),
      require = 0
   )
   private void fluxvisuals$onSignedMessage(
      Text text, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci
   ) {
      BetterMinecraft.onChatMessage();
   }

   @WrapOperation(
      method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V"
      ),
      require = 0
   )
   private void fluxvisuals$animateNewMessage(
      ChatHud instance, ChatHud.Backend drawer, int width, int height, boolean focused,
      Operation<Void> original, @Local(argsOnly = true) DrawContext context
   ) {
      float displacement = BetterMinecraft.chatDisplacement(this.getLineHeight(), this.scrolledLines != 0);
      if (displacement != 0.0F) {
         context.getMatrices().pushMatrix();
         context.getMatrices().translate(0.0F, displacement);
      }
      original.call(instance, drawer, width, height, focused);
      if (displacement != 0.0F) {
         context.getMatrices().popMatrix();
      }
   }
}
