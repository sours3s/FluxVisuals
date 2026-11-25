package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/** Скрывает служебные иконки у сообщений чата, если включено в настройках. */
@Environment(EnvType.CLIENT)
@Mixin(ChatHudLine.Visible.class)
public abstract class BetterMinecraftMessageMixin {
   @Inject(
      method = "indicator()Lnet/minecraft/client/gui/hud/MessageIndicator;",
      at = @At("HEAD"),
      cancellable = true,
      require = 0
   )
   private void fluxvisuals$hideIndicator(CallbackInfoReturnable<MessageIndicator> cir) {
      if (BetterMinecraft.hideMessageIndicatorEnabled()) {
         cir.setReturnValue(null);
      }
   }
}
