package ru.fluxvisuals.utils.cosmetics;

import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

/** Switches cosmetic rendering between the built-in .bbmodel runtime and the legacy path. */
@Environment(EnvType.CLIENT)
public final class CosmeticBridge {
   private static final String PROPERTY = "fluxvisuals.bbmodel";
   private static final String MARKER = ".bbmodel-runtime";

   private CosmeticBridge() {
   }

   /** Enabled with -Dfluxvisuals.bbmodel=true or by dropping the marker file in the game directory. */
   public static boolean useBbModel() {
      if (Boolean.getBoolean(PROPERTY)) {
         return true;
      }
      try {
         Path gameDir = FabricLoader.getInstance().getGameDir();
         return Files.isRegularFile(gameDir.resolve(MARKER));
      } catch (Throwable ignored) {
         return false;
      }
   }

   public static boolean apply(CosmeticEntry entry) {
      return useBbModel() ? BbCosmeticManager.apply(entry) : FiguraBridge.apply(entry);
   }

   public static boolean clear() {
      return useBbModel() ? BbCosmeticManager.clear() : FiguraBridge.clear();
   }

   public static boolean isApplied(CosmeticEntry entry) {
      return useBbModel() ? BbCosmeticManager.isApplied(entry) : FiguraBridge.isApplied(entry);
   }

   public static String appliedId() {
      return useBbModel() ? BbCosmeticManager.appliedId() : FiguraBridge.appliedId();
   }

   public static boolean hasActiveAvatar() {
      if (useBbModel()) {
         return BbCosmeticManager.isActive();
      }
      MinecraftClient client = MinecraftClient.getInstance();
      if (!FiguraBridge.appliedId().isEmpty()) {
         return true;
      }
      try {
         return client != null && client.player != null
               && org.figuramc.figura.avatar.AvatarManager.getAvatarForPlayer(client.player.getUuid()) != null;
      } catch (Throwable ignored) {
         return false;
      }
   }
}
