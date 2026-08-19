package ru.fluxvisuals.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;

/**
 * BetterF3 — чистый кастомный оверлей отладки вместо ванильного F3.
 * Миксин DebugHudMixin отменяет ванильный и рисует кастомный когда:
 * - модуль включен
 * - нажата клавиша F3 (или бинд) — работает только пока зажата клавиша
 */
@IModule(name = "Better F3", description = "Замена стандартного экрана отладки F3 на чистый оверлей", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class BetterF3 extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static BetterF3 INSTANCE;

   public final BooleanSetting showCoords = new BooleanSetting("Coords", true);
   public final BooleanSetting showFps = new BooleanSetting("FPS", true);
   public final BooleanSetting showDirection = new BooleanSetting("Direction", true);
   public final BooleanSetting showPing = new BooleanSetting("Ping", true);
   public final BooleanSetting showEntities = new BooleanSetting("Entities", true);
   public final BooleanSetting showMemory = new BooleanSetting("Memory", true);
   public final ModeSetting position = new ModeSetting("Position", "Left", "Left", "Right");

   public BetterF3() {
      this.addSettings(new Setting[]{showCoords, showFps, showDirection, showPing, showEntities, showMemory, position});
      INSTANCE = this;
   }

   public static BetterF3 getInstance() { return INSTANCE; }

   /** Проверяет, нажата ли клавиша F3 (или бинд) прямо сейчас. */
   public static boolean isF3Pressed() {
      if (mc.options == null) return false;
      return mc.options.debugOverlayKey.isPressed();
   }
}
