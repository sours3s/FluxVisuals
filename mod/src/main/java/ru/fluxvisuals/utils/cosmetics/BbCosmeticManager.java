package ru.fluxvisuals.utils.cosmetics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModelLoader;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModelRenderer;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbTextureCache;

/** Loads and owns the active built-in .bbmodel cosmetic. */
@Environment(EnvType.CLIENT)
public final class BbCosmeticManager {
   private static final Logger LOGGER = LoggerFactory.getLogger(BbCosmeticManager.class);
   private static String appliedId = "";
   private static BbModel model;
   private static String modelKey = "";
   private static final BbTextureCache TEXTURES = new BbTextureCache();
   private static BbModelRenderer renderer;
   private static final BbModelLoader LOADER = new BbModelLoader();

   private BbCosmeticManager() {
   }

   public static boolean isActive() {
      return model != null;
   }

   public static String appliedId() {
      return appliedId;
   }

   public static boolean isApplied(CosmeticEntry entry) {
      return entry != null && entry.id().equals(appliedId);
   }

   public static BbModel activeModel() {
      return model;
   }

   public static String activeKey() {
      return modelKey;
   }

   public static BbModelRenderer renderer() {
      return renderer;
   }

   public static boolean apply(CosmeticEntry entry) {
      if (entry == null) {
         return false;
      }
      Path file = findModel(entry.folder());
      if (file == null) {
         LOGGER.warn("No .bbmodel found in {}", entry.folder());
         return false;
      }
      try {
         BbModel loaded = LOADER.load(file);
         model = loaded;
         modelKey = entry.id();
         renderer = new BbModelRenderer(TEXTURES);
         appliedId = entry.id();
         LOGGER.info("Loaded bbmodel cosmetic {} from {}", entry.id(), file.getFileName());
         return true;
      } catch (IOException | RuntimeException e) {
         LOGGER.error("Failed to load bbmodel cosmetic {}", entry.id(), e);
         return false;
      }
   }

   public static boolean clear() {
      model = null;
      modelKey = "";
      appliedId = "";
      renderer = null;
      return true;
   }

   private static Path findModel(Path folder) {
      if (folder == null || !Files.isDirectory(folder)) {
         return null;
      }
      List<Path> files;
      try (Stream<Path> stream = Files.list(folder)) {
         files = stream.filter(path -> path.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".bbmodel"))
               .sorted((left, right) -> Long.compare(size(right), size(left))).toList();
      } catch (IOException e) {
         LOGGER.warn("Unable to list models in {}", folder, e);
         return null;
      }
      return files.isEmpty() ? null : files.get(0);
   }

   private static long size(Path path) {
      try {
         return Files.size(path);
      } catch (IOException ignored) {
         return 0L;
      }
   }
}
