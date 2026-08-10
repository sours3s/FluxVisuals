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
import ru.fluxvisuals.util.render.animation.UiAnimations;

/**
 * Плавное «въезжание» списка игроков (TAB) сверху: пока открыт список,
 * его матрица сдвигается по Y по ease-out-кривой.
 */
@Environment(EnvType.CLIENT)
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
   @Unique
   private static boolean fluxvisuals$translated = false;

   @Inject(method = "setVisible(Z)V", at = @At("HEAD"))
   private void fluxvisuals$trackTabVisibility(boolean visible, CallbackInfo ci) {
      UiAnimations.onTabVisible(visible);
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"))
   private void fluxvisuals$tabSlidePush(DrawContext context, int x, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
      float offset = UiAnimations.tabSlideOffset(60.0F);
      if (offset != 0.0F) {
         context.getMatrices().pushMatrix();
         context.getMatrices().translate(0.0F, offset);
         fluxvisuals$translated = true;
      }
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("RETURN"))
   private void fluxvisuals$tabSlidePop(DrawContext context, int x, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
      if (fluxvisuals$translated) {
         context.getMatrices().popMatrix();
         fluxvisuals$translated = false;
      }
   }
}
