package ru.fluxvisuals.utils.cosmetics;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fluxvisuals.client.FluxVisualsClient;

@Environment(EnvType.CLIENT)
public final class CosmeticsRepository {
   private static final Logger LOGGER = LoggerFactory.getLogger(CosmeticsRepository.class);
   private static final String FOLDER = "cosmetics";
   private static final String AVATAR_FILE = "avatar.json";
   private static final String PREVIEW_FILE = "avatar.png";
   private static final String HEAD_PREFIX = "head-";
   private static final String WEAPON_PREFIX = "weapon-";
   private static final String BUNDLED_ROOT = "/fluxvisuals/cosmetics/";
   private static final String BUNDLED_INDEX = "/fluxvisuals/cosmetics.index";
   private static final int MAX_DEPTH = 4;
   private static final List<CosmeticEntry> ENTRIES = new ArrayList<>();
   private static boolean scanned;
   private static boolean extracted;
   private static final Map<String, Integer> GL_IDS = new HashMap<>();

   private CosmeticsRepository() {
   }

   public static Path directory() {
      if (FluxVisualsClient.get != null && FluxVisualsClient.get.root != null) {
         return FluxVisualsClient.get.root.toPath().resolve(FOLDER);
      }
      return Path.of(System.getProperty("user.home"), ".fluxvisuals", FOLDER);
   }

   public static List<CosmeticEntry> all() {
      if (!scanned) {
         rescan();
      }
      return ENTRIES;
   }

   public static List<CosmeticEntry> of(CosmeticEntry.Kind kind) {
      List<CosmeticEntry> filtered = new ArrayList<>();
      for (CosmeticEntry entry : all()) {
         if (entry.kind() == kind) {
            filtered.add(entry);
         }
      }
      return filtered;
   }

   public static CosmeticEntry byId(String id) {
      if (id != null && !id.isBlank()) {
         for (CosmeticEntry entry : all()) {
            if (entry.id().equals(id)) {
               return entry;
            }
         }
      }
      return null;
   }

   public static void rescan() {
      scanned = true;
      ENTRIES.clear();
      Path root = directory();
      try {
         Files.createDirectories(root);
      } catch (Exception var2) {
         LOGGER.warn("Could not create the cosmetics folder at {}", root, var2);
         return;
      }
      ensureExtracted(root);
      collect(root, root, 0);
      ENTRIES.sort(Comparator.<CosmeticEntry>comparingInt(entry -> entry.kind().ordinal())
            .thenComparing(entry -> entry.displayName().toLowerCase(Locale.ROOT)));
   }

   private static void collect(Path root, Path dir, int depth) {
      if (depth <= MAX_DEPTH && Files.isDirectory(dir) && !isHidden(dir)) {
         if (Files.isRegularFile(dir.resolve(AVATAR_FILE))) {
            if (!dir.equals(root)) {
               ENTRIES.add(toEntry(root, dir));
            }
         } else {
            try (Stream<Path> stream = Files.list(dir)) {
               for (Path child : stream.filter(Files::isDirectory).toList()) {
                  collect(root, child, depth + 1);
               }
            } catch (Exception var9) {
               LOGGER.debug("Skipping unreadable cosmetics folder {}", dir, var9);
            }
         }
      }
   }

   private static boolean isHidden(Path dir) {
      Path name = dir.getFileName();
      return name != null && name.toString().startsWith(".");
   }

   private static CosmeticEntry toEntry(Path root, Path dir) {
      String id = root.relativize(dir).toString().replace('\\', '/');
      int slash = id.indexOf('/');
      String top = slash < 0 ? id : id.substring(0, slash);
      String lower = top.toLowerCase(Locale.ROOT);
      CosmeticEntry.Kind kind;
      if (lower.startsWith(HEAD_PREFIX)) {
         kind = CosmeticEntry.Kind.HEAD;
      } else if (lower.startsWith(WEAPON_PREFIX)) {
         kind = CosmeticEntry.Kind.WEAPON;
      } else {
         kind = CosmeticEntry.Kind.MODEL;
      }
      return new CosmeticEntry(dir, id, label(dir.getFileName().toString()), kind);
   }

   private static String label(String folderName) {
      String label = folderName;
      String lower = folderName.toLowerCase(Locale.ROOT);
      if (lower.startsWith(HEAD_PREFIX)) {
         label = folderName.substring(HEAD_PREFIX.length());
      } else if (lower.startsWith(WEAPON_PREFIX)) {
         label = folderName.substring(WEAPON_PREFIX.length());
      }
      int dash = label.indexOf(" - ");
      if (dash > 0) {
         label = label.substring(0, dash);
      }
      return label.replace('_', ' ').trim();
   }

   private static void ensureExtracted(Path target) {
      if (!extracted) {
         extracted = true;
         try (Stream<Path> existing = Files.list(target)) {
            if (existing.findAny().isPresent()) {
               return;
            }
         } catch (Exception var7) {
            return;
         }
         List<String> index = readIndex();
         if (!index.isEmpty()) {
            int written = 0;
            for (String relative : index) {
               if (copyBundled(relative, target)) {
                  written++;
               }
            }
            LOGGER.info("Unpacked {} of {} bundled cosmetic files into {}", written, index.size(), target);
         }
      }
   }

   private static List<String> readIndex() {
      try (InputStream in = CosmeticsRepository.class.getResourceAsStream(BUNDLED_INDEX)) {
         if (in == null) {
            LOGGER.warn("No bundled cosmetics index on the classpath at {}", BUNDLED_INDEX);
            return List.of();
         }
         List<String> lines = new ArrayList<>();
         for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) {
               lines.add(trimmed);
            }
         }
         return lines;
      } catch (Exception var9) {
         LOGGER.error("Failed to read the bundled cosmetics index", var9);
         return List.of();
      }
   }

   private static boolean copyBundled(String relative, Path target) {
      Path file = resolveChild(target, relative);
      if (file == null) {
         return false;
      }
      try (InputStream in = CosmeticsRepository.class.getResourceAsStream(BUNDLED_ROOT + relative)) {
         if (in == null) {
            return false;
         }
         Files.createDirectories(file.getParent());
         Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
         return true;
      } catch (Exception var8) {
         LOGGER.debug("Skipped bundled cosmetic file {}", relative, var8);
         return false;
      }
   }

   private static Path resolveChild(Path target, String relative) {
      if (relative.isEmpty()) {
         return null;
      }
      Path resolved = target.resolve(relative).normalize();
      return resolved.startsWith(target.normalize()) ? resolved : null;
   }

   public static int loadPreview(CosmeticEntry entry) {
      if (entry == null) {
         return -1;
      }
      if (entry.previewGlId() >= 0) {
         return entry.previewGlId();
      }
      Integer cached = GL_IDS.get(entry.id());
      if (cached != null) {
         entry.setPreviewGlId(cached);
         return cached;
      }
      Path file = entry.folder().resolve(PREVIEW_FILE);
      if (!Files.isRegularFile(file)) {
         return -1;
      }
      try {
         byte[] fileBytes = Files.readAllBytes(file);
         java.nio.ByteBuffer imageBuffer = java.nio.ByteBuffer.allocateDirect(fileBytes.length);
         imageBuffer.put(fileBytes).flip();
         org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush();
         try {
            java.nio.IntBuffer w = stack.mallocInt(1);
            java.nio.IntBuffer h = stack.mallocInt(1);
            java.nio.IntBuffer comp = stack.mallocInt(1);
            java.nio.ByteBuffer image = org.lwjgl.stb.STBImage.stbi_load_from_memory(imageBuffer, w, h, comp, 4);
            if (image == null) {
               return -1;
            }
            int width = w.get(0);
            int height = h.get(0);
            int glId = org.lwjgl.opengl.GL11.glGenTextures();
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, glId);
            org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);
            org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);
            org.lwjgl.opengl.GL11.glTexImage2D(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_RGBA, width, height, 0, org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, image);
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0);
            org.lwjgl.stb.STBImage.stbi_image_free(image);
            GL_IDS.put(entry.id(), glId);
            entry.setPreviewGlId(glId);
            return glId;
         } finally {
            stack.close();
         }
      } catch (Exception var9) {
         LOGGER.debug("No usable preview for cosmetic {}", entry.id(), var9);
         return -1;
      }
   }

   private static String sanitize(String raw) {
      StringBuilder out = new StringBuilder(raw.length());
      for (char c : raw.toLowerCase(Locale.ROOT).toCharArray()) {
         out.append((c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '_' && c != '.' && c != '-' ? '_' : c);
      }
      return out.toString();
   }
}
