package ru.fluxvisuals.utils.cosmetics.bbmodel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Parsed representation of a Blockbench .bbmodel file. */
@Environment(EnvType.CLIENT)
public final class BbModel {
   public record Vec2(float x, float y) {
      public static Vec2 of(float x, float y) {
         return new Vec2(x, y);
      }
   }

   public record Vec3(float x, float y, float z) {
      public static final Vec3 ZERO = new Vec3(0.0F, 0.0F, 0.0F);
      public static final Vec3 ONE = new Vec3(1.0F, 1.0F, 1.0F);

      public static Vec3 of(float x, float y, float z) {
         return new Vec3(x, y, z);
      }
   }

   /** A decoded embedded texture. The bytes are a PNG file. */
   public record Texture(int width, int height, byte[] png) {
   }

   public record Keyframe(String channel, float time, Vec3 value, String interpolation) {
   }

   public record Animator(UUID nodeUuid, List<Keyframe> keyframes) {
      public Animator {
         keyframes = List.copyOf(keyframes);
      }
   }

   public record Animation(String name, float length, boolean loop, Map<UUID, Animator> animators) {
      public Animation {
         animators = Map.copyOf(animators);
      }
   }

   private final String id;
   private final Vec2 resolution;
   private final List<Texture> textures;
   private final Map<String, Animation> animations;
   private final List<BbNode> roots;
   private final Map<UUID, BbNode> nodesByUuid;

   public BbModel(String id, Vec2 resolution, List<Texture> textures,
         List<Animation> animations, List<BbNode> roots, Map<UUID, BbNode> nodesByUuid) {
      this.id = id;
      this.resolution = resolution;
      this.textures = List.copyOf(textures);
      Map<String, Animation> byName = new LinkedHashMap<>();
      for (Animation animation : animations) {
         byName.put(normalizeName(animation.name()), animation);
      }
      this.animations = Collections.unmodifiableMap(byName);
      this.roots = List.copyOf(roots);
      this.nodesByUuid = Map.copyOf(nodesByUuid);
   }

   private static String normalizeName(String name) {
      return name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT);
   }

   public String id() {
      return id;
   }

   public Vec2 resolution() {
      return resolution;
   }

   public List<Texture> textures() {
      return textures;
   }

   public Animation animation(String name) {
      return animations.get(normalizeName(name));
   }

   public Map<String, Animation> animations() {
      return animations;
   }

   public List<BbNode> roots() {
      return roots;
   }

   public BbNode node(UUID uuid) {
      return nodesByUuid.get(uuid);
   }
}
