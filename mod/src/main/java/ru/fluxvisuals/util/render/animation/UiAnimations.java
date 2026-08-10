package ru.fluxvisuals.util.render.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

/**
 * Лёгкие UI-анимации: плавное появление списка игроков (TAB) и «подпрыгивание»
 * чата при поступлении нового сообщения. Состояние хранится статически и
 * обновляется из миксинов {@code PlayerListHudMixin} / {@code ChatHudMixin}.
 *
 * <p>Все значения считаются в тиках {@code InGameHud.getTicks()} — тот же «часы»,
 * что использует ванильный чат для расчёта прозрачности сообщений.
 */
@Environment(EnvType.CLIENT)
public final class UiAnimations {
   private static final int TAB_SLIDE_TICKS = 10;
   private static final int CHAT_PULSE_TICKS = 12;

   private static boolean tabWasVisible = false;
   private static int tabVisibleSince = -1;
   private static int chatPulseTick = -1;

   private UiAnimations() {
   }

   /** Вызывается из {@code PlayerListHud.setVisible} каждый кадр. */
   public static void onTabVisible(boolean visible) {
      if (visible && !tabWasVisible) {
         MinecraftClient mc = MinecraftClient.getInstance();
         tabVisibleSince = mc.inGameHud != null ? mc.inGameHud.getTicks() : 0;
      }
      tabWasVisible = visible;
   }

   /**
    * Смещение по Y для списка игроков при открытии (0 = на месте).
    * Список плавно «въезжает» сверху за ~10 тиков.
    */
   public static float tabSlideOffset(float maxOffset) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (tabVisibleSince < 0 || mc.inGameHud == null) {
         return 0.0F;
      }
      float t = (float) (mc.inGameHud.getTicks() - tabVisibleSince) / (float) TAB_SLIDE_TICKS;
      if (t >= 1.0F) {
         return 0.0F;
      }
      float eased = Easings.EASE_OUT_CUBIC.ease(Math.max(0.0F, t));
      return (1.0F - eased) * maxOffset;
   }

   /** Вызывается при добавлении нового сообщения в чат. */
   public static void onChatMessage() {
      MinecraftClient mc = MinecraftClient.getInstance();
      chatPulseTick = mc.inGameHud != null ? mc.inGameHud.getTicks() : 0;
   }

   /**
    * Смещение по Y для всего чата при появлении нового сообщения.
    * Чат мягко «подпрыгивает» вверх и возвращается обратно.
    */
   public static float chatPulseOffset(float maxOffset) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (chatPulseTick < 0 || mc.inGameHud == null) {
         return 0.0F;
      }
      float age = mc.inGameHud.getTicks() - chatPulseTick;
      if (age <= 0.0F || age >= CHAT_PULSE_TICKS) {
         return 0.0F;
      }
      float p = age / (float) CHAT_PULSE_TICKS;
      // sin-волна: 0 -> 1 -> 0 (вверх и обратно)
      float wave = (float) Math.sin(p * Math.PI);
      return -wave * maxOffset;
   }
}
