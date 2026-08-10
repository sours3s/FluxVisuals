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
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.visuals.CrosshairModule;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Отменяет ванильный crosshair и рисует кастомный при включённом CrosshairModule.
 * Статичная ошибка коррекции (когда атака кулдаунится) от ванильного тоже скрывается.
 */
@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class InGameHudCrosshairMixin {

   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CrosshairModule module = null;
      if (FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null) {
         module = FluxVisualsClient.get.manager.get(CrosshairModule.class);
      }
      if (module == null || !module.enable) return;

      ci.cancel();

      // Use Renderer2D from FluxVisualsClient
      Renderer2D r2 = null;
      try {
         r2 = FluxVisualsClient.getRenderer();
      } catch (Exception ignored) {}

      if (r2 != null) {
         // Renderer2D needs a frame — render during the HUD frame
         r2.pushAlpha(0.95F);
         module.renderCrosshair(r2);
         r2.popAlpha();
      }
   }
}
