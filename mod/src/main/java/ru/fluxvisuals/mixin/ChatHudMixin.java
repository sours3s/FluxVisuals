package ru.fluxvisuals.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
    private void onAddMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        // Получаем экземпляр ChatHud
        ChatHud chatHud = (ChatHud)(Object)this;

        // Здесь можно добавить вашу логику обработки сообщений в чате
        // Например, фильтрация или модификация сообщений
    }

    // @Inject(method = "tick", at = @At("HEAD"))
    // private void onTick(CallbackInfo ci) {
    //    // Отключено: метод tick отсутствует в ChatHud 1.21.11
    // }
}