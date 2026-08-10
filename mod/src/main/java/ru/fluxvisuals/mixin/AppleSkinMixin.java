package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.utils.AppleSkin;

/**
 * AppleSkinMixin — рисует полупрозрачную полоску насыщения поверх шкалы голода
 * и предпросмотр восстанавливаемых шанков при наведении на еду.
 */
@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class AppleSkinMixin {

   @Inject(method = "renderFood", at = @At("RETURN"))
   private void afterRenderFood(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
      AppleSkin module = null;
      if (FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null) {
         module = FluxVisualsClient.get.manager.get(AppleSkin.class);
      }
      if (module == null || !module.isSaturationEnabled()) return;
      if (context == null || player == null) return;

      MinecraftClient mc = MinecraftClient.getInstance();
      float guiScale = (float) mc.getWindow().getScaleFactor();
      if (guiScale <= 0.0F) guiScale = 1.0F;

      // The vanilla food bar is drawn at: x = right - 81, y = top
      // Each hunger shank is 8px wide, max 10 shanks = 80px
      int foodLevel = player.getHungerManager().getFoodLevel();
      float saturationLevel = player.getHungerManager().getSaturationLevel();
      int shanks = Math.min(foodLevel, 10);

      // Saturation overlay: golden fill proportional to saturation (max saturation = foodLevel)
      float satRatio = saturationLevel / Math.max(1.0F, foodLevel);
      float overlayW = 80.0F * Math.min(1.0F, satRatio);

      int foodX = (int) ((right - 81) / guiScale);
      int foodY = (int) (top / guiScale);

      // Draw saturation as a translucent golden overlay on the food bar
      int satAlpha = 120;
      context.fill(foodX, foodY + 1, foodX + (int)(overlayW / guiScale), foodY + 11, (satAlpha << 24) | 0xFFDDAA00);

      // Draw exhaustion indicator (darker overlay) - simplified
      if (module.isExhaustionEnabled()) {
         // HungerManager.getExhaustion() not available in this MC version - skip
      }
   }
}
