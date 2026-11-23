package ru.fluxvisuals.utils.cosmetics;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class FiguraBridge {
   private static final Logger LOGGER = LoggerFactory.getLogger(FiguraBridge.class);
   private static final String AVATAR_MANAGER = "org.figuramc.figura.avatar.AvatarManager";
   private static Boolean available;
   private static Method loadLocalAvatar;
   private static Method clearAvatars;
   private static String appliedId = "";

   private FiguraBridge() {
   }

   public static boolean isAvailable() {
      if (available == null) {
         available = resolve();
      }
      return available;
   }

   private static boolean resolve() {
      if (!FabricLoader.getInstance().isModLoaded("figura")) {
         return false;
      }
      try {
         Class.forName("org.figuramc.figura.FiguraMod");
         Class<?> manager = Class.forName(AVATAR_MANAGER);
         loadLocalAvatar = manager.getMethod("loadLocalAvatar", Path.class);
         clearAvatars = manager.getMethod("clearAvatars", UUID.class);
         LOGGER.info("Figura bridge resolved");
         return true;
      } catch (Exception e) {
         LOGGER.warn("Figura present but bridge failed to resolve", e);
         loadLocalAvatar = null;
         clearAvatars = null;
         return false;
      }
   }

   public static String appliedId() {
      return appliedId;
   }

   public static boolean isApplied(CosmeticEntry entry) {
      return entry != null && entry.id().equals(appliedId);
   }

   public static boolean apply(CosmeticEntry entry) {
      if (entry == null || !isAvailable()) {
         return false;
      }
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.player == null) {
         LOGGER.warn("Cannot apply: player not available");
         return false;
      }
      try {
         LOGGER.info("Loading avatar: {}", entry.id());
         // Клик по другой карточке не вызывает clear(), поэтому убираем наш
         // hide-скрипт и старый блок из предыдущего аватара вручную.
         repairPrevious();
         // Figura не прячет модель от владельца в первом лице сама: без этого
         // камера оказывается внутри головы аватара. Скрипт ставим до загрузки.
         CosmeticFirstPerson.installHide(entry.folder());
         // Без clearAvatars старый аватар остаётся активным и его camera/head-хуки
         // продолжают работать вместе с новым.
         clearAvatars.invoke(null, mc.player.getUuid());
         loadLocalAvatar.invoke(null, entry.folder());
         appliedId = entry.id();
         LOGGER.info("Avatar loaded: {}", entry.id());
         return true;
      } catch (Exception e) {
         LOGGER.error("Failed to apply cosmetic {}", entry.id(), e);
         return false;
      }
   }

   private static void repairPrevious() {
      if (appliedId.isEmpty()) {
         return;
      }
      CosmeticEntry previous = CosmeticsRepository.byId(appliedId);
      if (previous != null) {
         CosmeticFirstPerson.repair(previous.folder());
      }
   }

   public static boolean clear() {
      if (!isAvailable()) {
         appliedId = "";
         return false;
      }
      MinecraftClient mc = MinecraftClient.getInstance();
      UUID uuid = mc != null && mc.player != null ? mc.player.getUuid() : null;
      if (uuid == null) {
         appliedId = "";
         return false;
      }
      try {
         clearAvatars.invoke(null, uuid);
         repairPrevious();
         appliedId = "";
         LOGGER.info("Cleared cosmetic");
         return true;
      } catch (Exception e) {
         LOGGER.error("Failed to clear cosmetic", e);
         appliedId = "";
         return false;
      }
   }
}
