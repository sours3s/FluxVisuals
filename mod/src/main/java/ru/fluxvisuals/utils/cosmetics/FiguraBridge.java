package ru.fluxvisuals.utils.cosmetics;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
   private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "FiguraBridge-Worker");
      t.setDaemon(true);
      return t;
   });
   private static Boolean available;
   private static Method loadLocalAvatar;
   private static Method clearAvatars;
   private static volatile String appliedId = "";

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
         LOGGER.warn("Cannot apply cosmetic: player not available");
         return false;
      }
      appliedId = entry.id();
      EXECUTOR.submit(() -> {
         try {
            CosmeticFirstPerson.repair(entry.folder());
            if (entry.kind() != CosmeticEntry.Kind.WEAPON) {
               CosmeticFirstPerson.installHide(entry.folder());
            }
            loadLocalAvatar.invoke(null, entry.folder());
            LOGGER.info("Applied cosmetic: {}", entry.id());
         } catch (Exception e) {
            LOGGER.error("Failed to apply cosmetic {}", entry.id(), e);
            appliedId = "";
         }
      });
      return true;
   }

   public static boolean clear() {
      String prevId = appliedId;
      appliedId = "";
      if (!isAvailable()) {
         return false;
      }
      MinecraftClient mc = MinecraftClient.getInstance();
      UUID uuid = mc != null && mc.player != null ? mc.player.getUuid() : null;
      if (uuid == null) {
         return false;
      }
      EXECUTOR.submit(() -> {
         try {
            clearAvatars.invoke(null, uuid);
            LOGGER.info("Cleared cosmetic");
         } catch (Exception e) {
            LOGGER.error("Failed to clear cosmetic", e);
         }
      });
      return true;
   }
}
