package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/**
 * Масштабная анимация списка игроков: при открытии он «вырастает» на месте,
 * при закрытии — сжимается. Управляется модулем Better Minecraft.
 */
@Environment(EnvType.CLIENT)
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
   @Unique
   private boolean fluxvisuals$scaled;

   @Inject(method = "setVisible(Z)V", at = @At("HEAD"))
   private void fluxvisuals$trackVisibility(boolean visible, CallbackInfo ci) {
      BetterMinecraft.onTabVisibility(visible);
   }

   @Inject(
      method = "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
      at = @At("HEAD")
   )
   private void fluxvisuals$tabScaleHead(
      DrawContext context, int windowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci
   ) {
      this.fluxvisuals$scaled = false;
      if (!BetterMinecraft.tabAnimationEnabled()) {
         return;
      }
      float scale = BetterMinecraft.tabScale();
      if (scale >= 0.999F && scale <= 1.001F) {
         return;
      }
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(windowWidth / 2.0F, 10.0F);
      context.getMatrices().scale(scale, scale);
      context.getMatrices().translate(-windowWidth / 2.0F, -10.0F);
      this.fluxvisuals$scaled = true;
   }

   @Inject(
      method = "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
      at = @At("RETURN")
   )
   private void fluxvisuals$tabScaleTail(
      DrawContext context, int windowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci
   ) {
      if (this.fluxvisuals$scaled) {
         context.getMatrices().popMatrix();
         this.fluxvisuals$scaled = false;
      }
   }
}
