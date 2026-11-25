package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.utils.AppleSkin;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/** Иконки насыщения поверх ванильной шкалы еды. */
@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class BetterMinecraftSaturationMixin {
   @Inject(method = "renderFood(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;II)V", at = @At("RETURN"))
   private void fluxvisuals$renderSaturation(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
      // AppleSkin уже рисует свою полосу насыщения — не дублируем её.
      if (FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null) {
         AppleSkin appleSkin = FluxVisualsClient.get.manager.get(AppleSkin.class);
         if (appleSkin != null && appleSkin.enable && appleSkin.showSaturation.get()) {
            return;
         }
      }
      BetterMinecraft.renderSaturation(context, player, top, right);
   }
}
