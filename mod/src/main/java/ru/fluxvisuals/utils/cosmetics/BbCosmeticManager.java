package ru.fluxvisuals.utils.cosmetics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbAnimationPlayer;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModelLoader;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModelRenderer;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbPoseState;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbTextureCache;

/** Loads and owns the active built-in .bbmodel cosmetic. */
@Environment(EnvType.CLIENT)
public final class BbCosmeticManager {
   private static final Logger LOGGER = LoggerFactory.getLogger(BbCosmeticManager.class);
   private static String appliedId = "";
   private static CosmeticEntry.Kind appliedKind;
   private static BbModel model;
   private static String modelKey = "";
   private static final BbTextureCache TEXTURES = new BbTextureCache();
   private static BbModelRenderer renderer;
   private static BbAnimationPlayer animation;
   private static final BbModelLoader LOADER = new BbModelLoader();
   private static long lastAnimationNanos;

   private BbCosmeticManager() {
   }

   public static boolean isActive() {
      return model != null;
   }

   public static boolean isAvatarActive() {
      return isActive() && appliedKind != CosmeticEntry.Kind.WEAPON;
   }

   public static boolean isWeaponActive() {
      return isActive() && appliedKind == CosmeticEntry.Kind.WEAPON;
   }

   public static CosmeticEntry.Kind appliedKind() {
      return appliedKind;
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
         TEXTURES.clear();
         model = loaded;
         modelKey = entry.id();
         appliedKind = entry.kind();
         renderer = new BbModelRenderer(TEXTURES);
         animation = new BbAnimationPlayer(loaded);
         appliedId = entry.id();
         lastAnimationNanos = 0L;
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
      appliedKind = null;
      renderer = null;
      animation = null;
      lastAnimationNanos = 0L;
      TEXTURES.clear();
      return true;
   }

   /** Renders the equipped avatar in place of the vanilla body. */
   public static void renderAvatar(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay) {
      if (!isAvatarActive() || renderer == null) {
         return;
      }
      tickAnimation();
      renderer.render(model, modelKey, matrices, queue, light, overlay);
   }

   /** Renders the equipped weapon model in the hand, using the vanilla item transform. */
   public static boolean renderWeapon(ItemDisplayContext context, MatrixStack matrices,
         OrderedRenderCommandQueue queue, int light, int overlay, Transformation transform) {
      if (!isWeaponActive() || renderer == null) {
         return false;
      }
      tickAnimation();
      matrices.push();
      if (transform != null) {
         transform.apply(context.isLeftHand(), matrices.peek());
      }
      renderer.render(model, modelKey, matrices, queue, light, overlay);
      matrices.pop();
      return true;
   }

   private static void tickAnimation() {
      if (animation == null || model == null) {
         return;
      }
      long now = System.nanoTime();
      float delta = lastAnimationNanos == 0L ? 0.0F : (now - lastAnimationNanos) / 1_000_000_000.0F;
      lastAnimationNanos = now;
      if (delta > 0.25F) {
         delta = 0.25F;
      }
      animation.tick(poseState(), delta);
   }

   private static BbPoseState poseState() {
      MinecraftClient client = MinecraftClient.getInstance();
      ClientPlayerEntity player = client == null ? null : client.player;
      if (player == null) {
         return BbPoseState.IDLE;
      }
      float speed = (float) player.getVelocity().horizontalLength();
      boolean moving = speed > 0.08F || player.forwardSpeed != 0.0F || player.sidewaysSpeed != 0.0F;
      long millis = System.currentTimeMillis();
      boolean blinking = millis % 4700L < 140L;
      return new BbPoseState(
            moving,
            player.isSprinting() && moving,
            player.isSneaking(),
            player.isGliding(),
            player.getAbilities().flying,
            player.isSwimming(),
            player.isUsingItem(),
            player.getHandSwingProgress(0.0F) > 0.0F,
            blinking,
            speed);
   }

   private static Path findModel(Path folder) {
      if (folder == null || !Files.isDirectory(folder)) {
         return null;
      }
      List<Path> files;
      try (Stream<Path> stream = Files.list(folder)) {
         files = stream.filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".bbmodel"))
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
