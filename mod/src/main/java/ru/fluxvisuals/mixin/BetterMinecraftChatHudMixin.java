package ru.fluxvisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Анимация появления строк чата: новые сообщения «прилетают» справа и плавно
 * проявляются. Каждой видимой строке запоминается время появления.
 */
@Environment(EnvType.CLIENT)
@Mixin(ChatHud.class)
public class BetterMinecraftChatHudMixin {
   @Unique
   private static final Map<ChatHudLine.Visible, Long> fluxvisuals$lineTimestamps = new IdentityHashMap<>();

   @Unique
   private ChatHud.Backend fluxvisuals$backend = null;

   private static BetterMinecraft bmc() {
      if (FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) return null;
      return FluxVisualsClient.get.manager.get(BetterMinecraft.class);
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at = @At("HEAD"))
   private void fluxvisuals$renderHead(ChatHud.Backend drawer, int windowHeight, int currentTick, boolean expanded, CallbackInfo ci) {
      this.fluxvisuals$backend = drawer;
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at = @At("TAIL"))
   private void fluxvisuals$renderTail(ChatHud.Backend drawer, int windowHeight, int currentTick, boolean expanded, CallbackInfo ci) {
      this.fluxvisuals$backend = null;
   }

   @WrapOperation(
      method = "forEachVisibleLine",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/ChatHud$LineConsumer;accept(Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;IF)V"
      )
   )
   private void fluxvisuals$wrapLineAccept(@Coerce Object instance, ChatHudLine.Visible line, int y1, float opacity, Operation<Void> original) {
      BetterMinecraft mod = bmc();
      if (mod == null || !mod.enable || !BetterMinecraft.chatAnim.get() || fluxvisuals$backend == null) {
         original.call(instance, line, y1, opacity);
         return;
      }

      long now = System.currentTimeMillis();
      long spawnTime = fluxvisuals$lineTimestamps.computeIfAbsent(line, k -> now);
      float duration = BetterMinecraft.animSpeed.get();
      float progress = Math.min(1.0f, (now - spawnTime) / duration);

      if (progress >= 1.0f) {
         original.call(instance, line, y1, opacity);
         return;
      }

      // Cubic Out Easing для плавного замедления в конце прилёта.
      float ease = 1.0f - (float) Math.pow(1.0f - progress, 3.0f);
      // Прилёт справа налево (начинается со сдвига +80 пикселей вправо).
      float offsetX = (1.0f - ease) * 80.0f;

      // updatePose обновляет и живую матрицу (фон), и кэшированный снимок (текст).
      fluxvisuals$backend.updatePose(pose -> pose.translate(offsetX, 0.0f));
      original.call(instance, line, y1, opacity * ease);
      fluxvisuals$backend.updatePose(pose -> pose.translate(-offsetX, 0.0f));
   }

   @Inject(method = "clear", at = @At("HEAD"))
   private void fluxvisuals$onClear(boolean clearHistory, CallbackInfo ci) {
      fluxvisuals$lineTimestamps.clear();
   }
}
