package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.utils.ChatSounds;
import ru.fluxvisuals.util.ChatCleanUtil;
import ru.fluxvisuals.util.render.animation.UiAnimations;

@Environment(EnvType.CLIENT)
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
   // Всегда вырезаем маркер голов сервера («[unknown player head]»/«[Ник head]») —
   // в чате остаются только ники и текст.
   @ModifyVariable(
      method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private Text fluxvisuals$cleanHeadMarkers(Text message) {
      return ChatCleanUtil.clean(message);
   }

   @Inject(
      method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
      at = @At("RETURN")
   )
   private void fluxvisuals$chatSounds(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
      ChatSounds.onChatMessage(message);
      // Анимация «подпрыгивания» чата при новом сообщении.
      UiAnimations.onChatMessage();
   }

   @Unique
   private static boolean fluxvisuals$chatTranslated = false;

   @Inject(
      method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
      at = @At("HEAD")
   )
   private void fluxvisuals$chatPulsePush(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int width, int height, int tickCounter, boolean focused, boolean chatScreenOpen, CallbackInfo ci) {
      float offset = UiAnimations.chatPulseOffset(6.0F);
      if (offset != 0.0F) {
         context.getMatrices().pushMatrix();
         context.getMatrices().translate(0.0F, offset);
         fluxvisuals$chatTranslated = true;
      }
   }

   @Inject(
      method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
      at = @At("RETURN")
   )
   private void fluxvisuals$chatPulsePop(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int width, int height, int tickCounter, boolean focused, boolean chatScreenOpen, CallbackInfo ci) {
      if (fluxvisuals$chatTranslated) {
         context.getMatrices().popMatrix();
         fluxvisuals$chatTranslated = false;
      }
   }
}
