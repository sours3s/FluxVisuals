package ru.fluxvisuals.ui.fluxgui.cosmetics;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import ru.fluxvisuals.utils.cosmetics.CosmeticEntry;

/** GUI cosmetic item backed by a bundled .bbmodel folder. */
public final class CosmeticItem {
   private static final Identifier FALLBACK_PREVIEW =
      Identifier.of("fluxvisuals", "textures/gui/fluxvisuals_logo.png");

   private final String name;
   private final String category;
   private final CosmeticEntry entry;
   private Identifier previewId;

   public CosmeticItem(String name, String category) {
      this(name, category, null);
   }

   public CosmeticItem(String name, String category, CosmeticEntry entry) {
      this.name = name;
      this.category = category;
      this.entry = entry;
   }

   public String getName() {
      return name;
   }

   public String getCategory() {
      return category;
   }

   public CosmeticEntry entry() {
      return entry;
   }

   public String getSlug() {
      return name.toLowerCase(Locale.ROOT).replace(' ', '_');
   }

   /** Превью не рисуется как 3D-модель: используем avatar.png из папки аватара. */
   public BedrockModel getModel() {
      return null;
   }

   public Identifier getPreviewId() {
      if (previewId != null) {
         return previewId;
      }
      if (entry == null) {
         return FALLBACK_PREVIEW;
      }
      Path file = entry.folder().resolve("avatar.png");
      if (!Files.isRegularFile(file)) {
         return FALLBACK_PREVIEW;
      }
      Identifier id = Identifier.of("fluxvisuals", "cosmetic_preview/" + sanitize(entry.id()) + ".png");
      try (InputStream in = Files.newInputStream(file)) {
         NativeImage image = NativeImage.read(in);
         MinecraftClient.getInstance().getTextureManager()
            .registerTexture(id, new NativeImageBackedTexture(() -> id.toString(), image));
         previewId = id;
      } catch (Exception ignored) {
         previewId = FALLBACK_PREVIEW;
      }
      return previewId;
   }

   public Identifier getTextureId() {
      return getPreviewId();
   }

   public Object getAnimation() {
      return null;
   }

   private static String sanitize(String raw) {
      StringBuilder out = new StringBuilder(raw.length());
      for (char c : raw.toLowerCase(Locale.ROOT).toCharArray()) {
         boolean allowed = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/';
         out.append(allowed ? c : '_');
      }
      return out.length() == 0 ? "avatar" : out.toString();
   }
}
