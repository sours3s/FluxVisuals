package ru.fluxvisuals.utils.cosmetics.bbmodel;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/** Registers embedded .bbmodel textures with Minecraft's texture manager. */
@Environment(EnvType.CLIENT)
public final class BbTextureCache {
   private static final Identifier FALLBACK = Identifier.of("fluxvisuals", "bb/fallback_white");
   private final Map<Identifier, NativeImageBackedTexture> owned = new HashMap<>();
   private boolean fallbackRegistered;

   public Identifier texture(String modelKey, int index, BbModel.Texture texture) {
      Identifier id = Identifier.of("fluxvisuals", "bb/" + sanitize(modelKey) + "/" + index + ".png");
      if (owned.containsKey(id)) {
         return id;
      }
      if (texture.png().length == 0) {
         registerFallback();
         return FALLBACK;
      }
      try (ByteArrayInputStream stream = new ByteArrayInputStream(texture.png())) {
         NativeImage image = NativeImage.read(stream);
         NativeImageBackedTexture backed = new NativeImageBackedTexture(() -> id.toString(), image);
         MinecraftClient.getInstance().getTextureManager().registerTexture(id, backed);
         owned.put(id, backed);
         return id;
      } catch (Exception ignored) {
         registerFallback();
         return FALLBACK;
      }
   }

   public Identifier fallback() {
      registerFallback();
      return FALLBACK;
   }

   /** Releases GPU textures owned by this cache. Call when switching cosmetics. */
   public void clear() {
      for (NativeImageBackedTexture texture : owned.values()) {
         texture.close();
      }
      owned.clear();
   }

   private void registerFallback() {
      if (fallbackRegistered) {
         return;
      }
      NativeImage image = new NativeImage(1, 1, true);
      image.setColor(0, 0, 0xFFFFFFFF);
      MinecraftClient.getInstance().getTextureManager()
         .registerTexture(FALLBACK, new NativeImageBackedTexture(() -> FALLBACK.toString(), image));
      fallbackRegistered = true;
   }

   private static String sanitize(String raw) {
      StringBuilder out = new StringBuilder(raw.length());
      for (char c : raw.toLowerCase(Locale.ROOT).toCharArray()) {
         boolean allowed = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/';
         out.append(allowed ? c : '_');
      }
      return out.length() == 0 ? "model" : out.toString();
   }
}
