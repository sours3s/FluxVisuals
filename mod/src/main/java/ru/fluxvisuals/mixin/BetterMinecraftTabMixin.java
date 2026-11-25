package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/** Дорисовывает список игроков во время его анимации закрытия. */
@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class BetterMinecraftTabMixin {
   @Shadow
   private MinecraftClient client;

   @Shadow
   private PlayerListHud playerListHud;

   @Inject(
      method = "renderPlayerList(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At("RETURN")
   )
   private void fluxvisuals$renderClosingTab(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (!BetterMinecraft.shouldRenderClosingTab()) {
         return;
      }
      if (this.client == null || this.client.world == null || this.client.options.playerListKey.isPressed()) {
         return;
      }
      Scoreboard scoreboard = this.client.world.getScoreboard();
      ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
      this.playerListHud.render(context, context.getScaledWindowWidth(), scoreboard, objective);
   }
}
