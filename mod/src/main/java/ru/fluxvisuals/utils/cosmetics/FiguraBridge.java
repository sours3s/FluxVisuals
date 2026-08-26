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
         Class<?> manager = Class.forName(AVATAR_MANAGER);
         loadLocalAvatar = manager.getMethod("loadLocalAvatar", Path.class);
         clearAvatars = manager.getMethod("clearAvatars", UUID.class);
         return true;
      } catch (Exception var1) {
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
      if (entry != null && isAvailable()) {
         try {
            CosmeticFirstPerson.repair(entry.folder());
            if (entry.kind() != CosmeticEntry.Kind.WEAPON) {
               CosmeticFirstPerson.installHide(entry.folder());
            }
            loadLocalAvatar.invoke(null, entry.folder());
            appliedId = entry.id();
            return true;
         } catch (Exception var2) {
            LOGGER.error("Failed to apply cosmetic {}", entry.id(), var2);
            return false;
         }
      }
      return false;
   }

   public static boolean clear() {
      appliedId = "";
      if (isAvailable()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc != null && mc.player != null) {
            try {
               clearAvatars.invoke(null, mc.player.getUuid());
               return true;
            } catch (Exception var1) {
               LOGGER.error("Failed to clear the applied cosmetic", var1);
               return false;
            }
         }
      }
      return false;
   }
}
