package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.util.render.HudRenderBridge;

@Environment(EnvType.CLIENT)
@Mixin({InGameHud.class})
public class InGameHudMixin {
   private static boolean isHudModuleEnabled() {
      return FluxVisualsClient.isModInitialized() && FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null;
   }

   private static boolean isCustomPotionsActive() {
      if (!isHudModuleEnabled()) return false;
      try {
         return Hud.isPotionListActive();
      } catch (Throwable t) {
         return false;
      }
   }

   private static boolean isCustomHotbarActive() {
      if (!isHudModuleEnabled()) return false;
      try {
         return Hud.isHotbarBindsActive();
      } catch (Throwable t) {
         return false;
      }
   }
   @Inject(
      method = {"renderStatusEffectOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      // Ванильные иконки эффектов убраны всегда — есть элемент Potions.
      ci.cancel();
   }

   @Inject(
      method = {"renderHotbar"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (isCustomHotbarActive()) {
         ci.cancel(); // Отменяем ванильный рендер, свой рисуем в Hud.onRender (EventScreen)
      }
   }

   @Inject(
      method = {"renderHealthBar"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderHealthBar(
      DrawContext context,
      PlayerEntity player,
      int x,
      int y,
      int lines,
      int regeneratingHeartIndex,
      float maxHealth,
      int lastHealth,
      int health,
      int absorption,
      boolean blinking,
      CallbackInfo ci
   ) {
      if (isCustomHotbarActive()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderFood"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderFood(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
      if (isCustomHotbarActive()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderArmor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void onRenderArmor(DrawContext context, PlayerEntity player, int i, int j, int k, int x, CallbackInfo ci) {
      if (isCustomHotbarActive()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderAirBubbles"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderAir(DrawContext context, PlayerEntity player, int heartCount, int top, int left, CallbackInfo ci) {
      if (isCustomHotbarActive()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderMountHealth"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderMountHealth(DrawContext context, CallbackInfo ci) {
      if (isCustomHotbarActive()) {
         ci.cancel();
      }
   }

   @Redirect(
      method = {"renderMainHud"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/bar/Bar;drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V"
      )
   )
   private void redirectDrawExperienceLevel(DrawContext context, TextRenderer textRenderer, int level) {
      if (!isCustomHotbarActive()) {
         Bar.drawExperienceLevel(context, textRenderer, level);
      }
   }

   @Redirect(
      method = {"renderMainHud"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/bar/Bar;renderBar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
         ordinal = 0
      )
   )
   private void redirectRenderBar(Bar bar, DrawContext context, RenderTickCounter tickCounter) {
      if (!isCustomHotbarActive()) {
         bar.renderBar(context, tickCounter);
      }
   }

   @Redirect(
      method = {"renderMainHud"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/bar/Bar;renderAddons(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
         ordinal = 0
      )
   )
   private void redirectRenderAddons(Bar bar, DrawContext context, RenderTickCounter tickCounter) {
      if (!isCustomHotbarActive()) {
         bar.renderAddons(context, tickCounter);
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void onRenderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      // Когда открыт чат — HUD-элементы рисуем в ChatScreenMixin ПОВЕРХ чата, а не под ним
      // (иначе они прячутся за тёмным фоном панели чата). Здесь пропускаем.
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.currentScreen instanceof ChatScreen) {
         return;
      }
      HudRenderBridge.renderHudOverlay(context, tickCounter);
   }
}
