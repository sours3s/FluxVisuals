package ru.fluxvisuals.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Настройки стилей оформления (вкладка Styles в ClickGUI).
 * Персистятся в gui.cfg через GuiManager.
 */
@Environment(EnvType.CLIENT)
public final class StyleConfig {
   public static boolean watermarkLogo = true;
   public static boolean watermarkGlow = true;
   public static boolean watermarkName = true;
   public static boolean clickGuiLogo = true;
   /** Ник клиента (ватермарка) из лаунчера: у обычного юзера = его ник, у админа — свой. */
   public static String clientName = "Flux Visuals";

   private StyleConfig() {
   }
}
