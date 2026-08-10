package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.crosshair.CrosshairSettings;
import ru.fluxvisuals.crosshair.RenderCrosshairDrawer;

/**
 * Всегда рисует кастомный прицел вместо ванильного на основе настроек
 * «Мастерской прицелов» (CrosshairSettings). Тумблер-модуля нет.
 */
@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class InGameHudCrosshairMixin {

   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      CrosshairSettings settings = CrosshairSettings.getInstance();
      if (!settings.enabled) return;
      // Скрываем в спектаторе (как ванильный)
      if (mc.player != null && mc.player.isSpectator()) return;

      ci.cancel();
      RenderCrosshairDrawer.draw(context, settings);
   }
}
