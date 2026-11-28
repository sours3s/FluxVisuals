package ru.fluxvisuals.module.impl.visuals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventChangeWorld;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.event.render.EventRender3D;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ListSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.other.Mathf;
import ru.fluxvisuals.util.render.TextCache;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

@IModule(
   name = "NameTags",
   description = "Renders advanced and customizable information plates over players in line of sight",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class NameTags extends Module {
   private static final Comparator<PlayerEntity> DISTANCE_DESC =
         Comparator.comparingDouble((PlayerEntity p) -> p.squaredDistanceTo(mc.player)).reversed();

   private static final List<NameTags.PendingItemRender> pendingItems = new ArrayList<>();
   private static final List<NameTags.PendingHeadRender> pendingHeads = new ArrayList<>();

   private static final Map<PlayerEntity, Boolean> visibleCache = new HashMap<>();
   private static final Map<PlayerEntity, TagData> tagCache = new HashMap<>();
   private static long lastVisibleCacheTime = 0L;

   public final ModeSetting hpMode = new ModeSetting("HP mode", "Health points", "Health points", "Hearts");
   public final SliderSetting size = new SliderSetting("Size", 0.35F, 0.01F, 1.0F, 0.05F, false);
   public final BooleanSetting background = new BooleanSetting("Background", true);
   public final SliderSetting opacity = new SliderSetting("Opacity", 100.0F, 0.0F, 100.0F, 5.0F, true)
         .hidden(() -> !this.background.get());
   public final SliderSetting range = new SliderSetting("Range", 64.0F, 8.0F, 256.0F, 4.0F, false);
   public final MultiBooleanSetting targets = new MultiBooleanSetting(
         "Targets",
         new BooleanSetting("Friends", true),
         new BooleanSetting("Enemies", true),
         new BooleanSetting("Enemies in armor", false)
   );
   public final ListSetting renderParts = new ListSetting(
         "Render parts", "Prefix", "Armor", "Left hand", "Right hand", "Color HP"
   );
   public final BooleanSetting vanillaTags = new BooleanSetting("Vanilla nametags", false);
   public final BooleanSetting hideEmptySlots = new BooleanSetting("Hide empty slots", false);
   public final BooleanSetting itemCount = new BooleanSetting("Item count", true);
   public final BooleanSetting simplifyFar = new BooleanSetting("Simplify far", false);
   public final SliderSetting simplifyRange = new SliderSetting("Simplify range", 30.0F, 8.0F, 128.0F, 2.0F, false)
         .hidden(() -> !this.simplifyFar.get());

   private static final float TAG_FONT_SIZE = 25.0F;
   private static final float TAG_HEIGHT = 20.0F;
   private static final float TAG_RADIUS = 10.0F;
   private static final float TAG_PADDING_X = 7.0F;
   private static final float HEAD_SIZE = 15.0F;
   private static final float HEAD_GAP = 5.0F;
   private static final float ARMOR_TAG_HEIGHT = 16.0F;
   private static final float ARMOR_ICON_SIZE = 11.0F;
   private static final float ARMOR_ICON_SPACING = 16.0F;
   private static final float HAND_ICON_SIZE = 12.0F;
   private static final float HAND_ICON_GAP = 4.0F;
   private static final float GLASS_BLUR_STRENGTH = 18.0F;
   private static final float GLASS_BLUR_ALPHA = 0.9F;
   private static final int GLASS_ALPHA = 0x9E;
   private static final float MIN_DISTANCE_SCALE = 1.0F;
   private static final float MAX_DISTANCE_SCALE = 1.2F;
   private static final double SCALE_REFERENCE_DISTANCE = 4.0;

   private final List<PlayerEntity> visiblePlayers = new ArrayList<>();

   public NameTags() {
      this.renderParts.selected.add("Prefix");
      this.renderParts.selected.add("Armor");
      this.renderParts.selected.add("Left hand");
      this.renderParts.selected.add("Color HP");
      this.addSettings(new Setting[]{
            this.hpMode,
            this.size,
            this.background,
            this.opacity,
            this.range,
            this.targets,
            this.renderParts,
            this.vanillaTags,
            this.hideEmptySlots,
            this.itemCount,
            this.simplifyFar,
            this.simplifyRange
      });
   }

   @EventInit
   public void onWorldLoad(EventChangeWorld e) {
      visibleCache.clear();
      tagCache.clear();
      this.visiblePlayers.clear();
   }

   @EventInit
   public void onRender3D(EventRender3D event) {
      this.updatePositions(event.getTickDelta());
   }

   private void updatePositions(float tickDelta) {
      this.visiblePlayers.clear();
      if (mc.world == null || mc.player == null) {
         return;
      }

      long worldTime = mc.world.getTime();
      if (lastVisibleCacheTime != worldTime) {
         lastVisibleCacheTime = worldTime;
         visibleCache.clear();
      }

      Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
      double maxDistSq = this.range.get() * (double) this.range.get();
      boolean friends = this.targets.get("Friends");
      boolean enemies = this.targets.get("Enemies");
      boolean enemiesInArmor = this.targets.get("Enemies in armor");

      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player == mc.player || !player.isAlive()) continue;

         double dx = player.getX() - cam.x;
         double dy = player.getY() - cam.y;
         double dz = player.getZ() - cam.z;
         double distSq = dx * dx + dy * dy + dz * dz;
         if (distSq > maxDistSq) continue;

         boolean friend = FluxVisualsClient.get != null
               && FluxVisualsClient.get.friendManager != null
               && FluxVisualsClient.get.friendManager.isFriend(player.getName().getString());
         if (friend ? !friends : !enemies || enemiesInArmor && !hasArmor(player)) continue;

         // Line-of-sight: не показываем таблички сквозь стены (результат кэшируется на тик).
         boolean visible = visibleCache.computeIfAbsent(player, e -> mc.player != null && mc.player.canSee(e));
         if (!visible) continue;

         this.visiblePlayers.add(player);
      }

      this.visiblePlayers.sort(DISTANCE_DESC);
      tagCache.keySet().retainAll(this.visiblePlayers);
   }

   private static boolean hasArmor(PlayerEntity player) {
      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         if (!player.getEquippedStack(slot).isEmpty()) return true;
      }
      return false;
   }

   @EventInit
   public void onRender2D(EventScreen event) {
      if (mc.world == null || mc.player == null) {
         return;
      }
      Renderer2D renderer = event.renderer();
      DrawContext drawContext = event.drawContext();
      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      float fontScale = this.size.get() / 0.35F;
      boolean simplified = this.simplifyFar.get();
      double simplifyDistSq = this.simplifyRange.get() * (double) this.simplifyRange.get();

      for (PlayerEntity player : this.visiblePlayers) {
         Vec3d entityPos = new Vec3d(player.getX(), player.getY(), player.getZ());
         double distance = cameraPos.distanceTo(entityPos);
         float scale = (float) (SCALE_REFERENCE_DISTANCE / Math.max(distance, 1.0));
         scale = MathHelper.clamp(scale, MIN_DISTANCE_SCALE, MAX_DISTANCE_SCALE) * fontScale;

         Vec3d headScreen = Mathf.worldSpaceToScreenSpace(new Vec3d(entityPos.x,
               entityPos.y + player.getHeight() + 0.35, entityPos.z));
         Vec3d legsScreen = Mathf.worldSpaceToScreenSpace(new Vec3d(entityPos.x,
               entityPos.y - 0.35, entityPos.z));
         if (headScreen == null || legsScreen == null) continue;
         if (headScreen.z < 0.0 || headScreen.z > 1.0 || legsScreen.z < 0.0 || legsScreen.z > 1.0) continue;

         boolean near = !simplified || distance * distance <= simplifyDistSq;
         this.renderPlayerTag(renderer, drawContext, player,
               (float) headScreen.x * 2.0F, (float) headScreen.y * 2.0F,
               (float) legsScreen.x * 2.0F, (float) legsScreen.y * 2.0F, scale, near, distance);
      }

      this.renderPendingHeads(drawContext);
      this.renderPendingItems(drawContext);
   }

   private TagData tagData(Renderer2D renderer, PlayerEntity player) {
      float hp = player.getHealth();
      int hpKey = Math.round(hp * 10.0F);
      TagData data = tagCache.get(player);
      if (data == null || data.hpKey != hpKey) {
         String display = player.getDisplayName().getString();
         String plain = player.getName().getString();
         String prefix = display.equals(plain) ? "" : display.replace(plain, "").trim();
         String hpText = hpModeText(hp, player.getMaxHealth());
         float prefixWidth = TextCache.width(renderer, FontRegistry.INTER_MEDIUM, prefix, TAG_FONT_SIZE);
         float nameWidth = TextCache.width(renderer, FontRegistry.INTER_MEDIUM, plain, TAG_FONT_SIZE);
         float hpWidth = TextCache.width(renderer, FontRegistry.INTER_MEDIUM, hpText, TAG_FONT_SIZE);
         data = new TagData(plain, prefix, hpText, prefixWidth, nameWidth, hpWidth, hpKey);
         tagCache.put(player, data);
      }
      return data;
   }

   private String hpModeText(float hp, float maxHp) {
      if (this.hpMode.is("Hearts")) {
         int full = (int) Math.ceil(hp / 2.0F);
         return "❤".repeat(Math.max(0, full)) + " " + String.format("%.1f", hp).replace(',', '.');
      }
      return String.format("%.1f", hp).replace(',', '.') + "HP";
   }

   private void renderPlayerTag(Renderer2D renderer, DrawContext drawContext, PlayerEntity entity,
                                float screenX, float screenY, float legsX, float legsY,
                                float scale, boolean near, double distance) {
      TagData cached = tagData(renderer, entity);
      float hp = entity.getHealth();
      float maxHp = Math.max(entity.getMaxHealth(), 1.0F);
      boolean isFriend = FluxVisualsClient.get != null
            && FluxVisualsClient.get.friendManager != null
            && FluxVisualsClient.get.friendManager.isFriend(cached.name);

      String prefix = cached.prefix;
      if (isFriend) {
         prefix = prefix.isEmpty() ? "[F]" : "[F] " + prefix;
      }
      float prefixWidth = prefix.isEmpty() ? 0.0F
            : TextCache.width(renderer, FontRegistry.INTER_MEDIUM, prefix, TAG_FONT_SIZE);
      float prefixGap = prefix.isEmpty() ? 0.0F : 3.0F;

      float gap = 5.0F;
      float pillWidth = prefixWidth + prefixGap + cached.nameWidth + gap + cached.hpWidth
            + HEAD_SIZE + HEAD_GAP + TAG_PADDING_X * 2.0F;

      renderer.pushTranslation(screenX, screenY);
      renderer.pushScale(scale, scale);

      if (this.background.get()) {
         drawGlassTag(renderer, -pillWidth / 2.0F, -TAG_HEIGHT / 2.0F, pillWidth, TAG_HEIGHT, TAG_RADIUS);
      }
      float textY = centeredTextBaseline(renderer, cached.name, -TAG_HEIGHT / 2.0F, TAG_HEIGHT, TAG_FONT_SIZE);
      float textX = -pillWidth / 2.0F + TAG_PADDING_X + HEAD_SIZE + HEAD_GAP;
      if (!prefix.isEmpty()) {
         renderer.text(FontRegistry.INTER_MEDIUM, textX, textY, TAG_FONT_SIZE, prefix,
               isFriend ? ColorUtil.getColor(40, 255, 40, 255) : Renderer2D.ColorUtil.getTextColor(1, 1));
         textX += prefixWidth + prefixGap;
      }
      renderer.text(FontRegistry.INTER_MEDIUM, textX, textY, TAG_FONT_SIZE, cached.name,
            Renderer2D.ColorUtil.getTextColor(1, 1));
      textX += cached.nameWidth + gap;
      int hpColor = this.renderParts.isSelected("Color HP") ? getHealthColor(hp / maxHp)
            : Renderer2D.ColorUtil.getTextColor(1, 1);
      renderer.text(FontRegistry.INTER_MEDIUM, textX, textY, TAG_FONT_SIZE, cached.hpText, hpColor);

      this.queuePlayerHead(entity, screenX, screenY, scale, -pillWidth / 2.0F + TAG_PADDING_X + HEAD_SIZE / 2.0F, 0.0F);
      renderer.popScale();
      renderer.popTransform();

      if (!near) return;

      if (this.renderParts.isSelected("Armor")) {
         this.queueArmorIcons(renderer, entity, screenX, screenY, scale);
      }
      if (this.renderParts.isSelected("Right hand") || this.renderParts.isSelected("Left hand")) {
         renderer.pushTranslation(legsX, legsY);
         renderer.pushScale(scale, scale);
         this.drawPlayerHands(renderer, entity, legsX, legsY, scale);
         renderer.popScale();
         renderer.popTransform();
      }
   }

   private void queueArmorIcons(Renderer2D renderer, PlayerEntity player, float screenX, float screenY, float scale) {
      EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET};
      List<ItemStack> armorStacks = new ArrayList<>();
      for (EquipmentSlot slot : slots) {
         ItemStack stack = player.getEquippedStack(slot);
         if (!stack.isEmpty() || !this.hideEmptySlots.get()) {
            armorStacks.add(stack);
         }
      }
      if (armorStacks.isEmpty()) return;

      int slotCount = armorStacks.size();
      float totalWidth = slotCount == 1 ? ARMOR_ICON_SIZE
            : (slotCount - 1) * ARMOR_ICON_SPACING + ARMOR_ICON_SIZE;
      float firstCenterX = -totalWidth / 2.0F;
      float armorTagWidth = totalWidth + 10.0F;
      float armorTagY = -TAG_HEIGHT / 2.0F - ARMOR_TAG_HEIGHT - 1.5F;
      if (this.background.get()) {
         drawGlassTag(renderer, -armorTagWidth / 2.0F, armorTagY, armorTagWidth, ARMOR_TAG_HEIGHT, 8.0F);
      }

      for (int i = 0; i < slotCount; i++) {
         ItemStack stack = armorStacks.get(i);
         float localCenterX = firstCenterX + ARMOR_ICON_SIZE / 2.0F + i * ARMOR_ICON_SPACING;
         float localCenterY = armorTagY + ARMOR_TAG_HEIGHT / 2.0F;
         float worldX = screenX + localCenterX * scale;
         float worldY = screenY + localCenterY * scale;
         float itemPx = 16.0F * 0.5F * scale;
         pendingItems.add(new NameTags.PendingItemRender(player, stack, (worldX - itemPx) / 2.0F,
               (worldY - itemPx) / 2.0F, i, 0.5F * scale, false));
      }
   }

   private void queuePlayerHead(PlayerEntity player, float screenX, float screenY, float scale,
                                float localCenterX, float localCenterY) {
      SkinTextures skinTextures = null;
      if (mc.getNetworkHandler() != null) {
         var entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
         if (entry != null) {
            skinTextures = entry.getSkinTextures();
         }
      }
      if (skinTextures == null) {
         skinTextures = net.minecraft.client.util.DefaultSkinHelper.getSkinTextures(player.getUuid());
      }
      float worldX = screenX + localCenterX * scale;
      float worldY = screenY + localCenterY * scale;
      pendingHeads.add(new NameTags.PendingHeadRender(skinTextures,
            (worldX - HEAD_SIZE * scale / 2.0F) / 2.0F,
            (worldY - HEAD_SIZE * scale / 2.0F) / 2.0F,
            Math.max(1, Math.round(HEAD_SIZE * scale / 2.0F))));
   }

   private void renderPendingHeads(DrawContext drawContext) {
      if (!pendingHeads.isEmpty()) {
         for (NameTags.PendingHeadRender head : pendingHeads) {
            PlayerSkinDrawer.draw(drawContext, head.skinTextures, Math.round(head.x), Math.round(head.y), head.size);
         }
         pendingHeads.clear();
      }
   }

   private void renderPendingItems(DrawContext drawContext) {
      if (!pendingItems.isEmpty()) {
         for (NameTags.PendingItemRender item : pendingItems) {
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(item.x, item.y);
            drawContext.getMatrices().scale(item.scale, item.scale);
            drawContext.drawItem(item.player, item.stack, 0, 0, item.seed);
            if (item.showCount && item.stack.getCount() > 1) {
               drawContext.drawText(mc.textRenderer, "x" + item.stack.getCount(), 8, 8,
                     Renderer2D.ColorUtil.getTextColor(1, 1), false);
            }
            drawContext.getMatrices().popMatrix();
         }
         pendingItems.clear();
      }
   }

   private void drawPlayerHands(Renderer2D renderer, PlayerEntity player,
                                float screenX, float screenY, float scale) {
      List<ItemStack> items = new ArrayList<>();
      // Донор: «Левая рука» = offhand, «Правая рука» = main hand.
      if (this.renderParts.isSelected("Left hand") && !player.getOffHandStack().isEmpty()) {
         items.add(player.getOffHandStack());
      }
      if (this.renderParts.isSelected("Right hand") && !player.getMainHandStack().isEmpty()) {
         items.add(player.getMainHandStack());
      }
      if (items.isEmpty()) return;

      float y = 8.0F;
      for (int i = 0; i < items.size(); i++) {
         ItemStack stack = items.get(i);
         String text = stack.getName().getString();
         if (this.itemCount.get() && stack.getCount() > 1) {
            text = text + " x" + stack.getCount();
         }
         float textWidth = renderer.measureText(FontRegistry.INTER_MEDIUM, text, TAG_FONT_SIZE).width;
         float pillWidth = HAND_ICON_SIZE + HAND_ICON_GAP + textWidth + TAG_PADDING_X * 2.0F;
         if (this.background.get()) {
            drawGlassTag(renderer, -pillWidth / 2.0F, y, pillWidth, TAG_HEIGHT, TAG_RADIUS);
         }
         float iconCenterX = -pillWidth / 2.0F + TAG_PADDING_X + HAND_ICON_SIZE / 2.0F;
         float iconCenterY = y + TAG_HEIGHT / 2.0F;
         float worldX = screenX + iconCenterX * scale;
         float worldY = screenY + iconCenterY * scale;
         float itemPx = 16.0F * 0.5F * scale;
         pendingItems.add(new NameTags.PendingItemRender(player, stack, (worldX - itemPx) / 2.0F,
               (worldY - itemPx) / 2.0F, 20 + i, 0.5F * scale, this.itemCount.get()));
         renderer.text(FontRegistry.INTER_MEDIUM,
               -pillWidth / 2.0F + TAG_PADDING_X + HAND_ICON_SIZE + HAND_ICON_GAP,
               centeredTextBaseline(renderer, text, y, TAG_HEIGHT, TAG_FONT_SIZE),
               TAG_FONT_SIZE, text, Renderer2D.ColorUtil.getTextColor(1, 1));
         y += TAG_HEIGHT + 3.0F;
      }
   }

   private void drawGlassTag(Renderer2D renderer, float x, float y, float width, float height, float radius) {
      int alpha = Math.round(GLASS_ALPHA * (this.opacity.get() / 100.0F));
      renderer.prepareBlur(GLASS_BLUR_STRENGTH);
      renderer.blur(x, y, width, height, radius, GLASS_BLUR_ALPHA);
      renderer.rect(x, y, width, height, radius, ColorUtil.getColor(3, 3, 4, alpha));
   }

   private static int getHealthColor(float healthPercent) {
      if (healthPercent > 0.5F) return ColorUtil.getColor(35, 255, 95, 255);
      if (healthPercent > 0.2F) return ColorUtil.getColor(255, 218, 45, 255);
      return ColorUtil.getColor(255, 55, 55, 255);
   }

   private static float centeredTextBaseline(Renderer2D renderer, String text, float top, float height, float size) {
      var metrics = renderer.measureText(FontRegistry.INTER_MEDIUM, text, size);
      return top + (height - metrics.height) / 2.0F + metrics.baselineOffset;
   }

   @Environment(EnvType.CLIENT)
   private static class PendingItemRender {
      final PlayerEntity player;
      final ItemStack stack;
      final float x;
      final float y;
      final int seed;
      final float scale;
      final boolean showCount;

      PendingItemRender(PlayerEntity player, ItemStack stack, float x, float y,
                        int seed, float scale, boolean showCount) {
         this.player = player;
         this.stack = stack;
         this.x = x;
         this.y = y;
         this.seed = seed;
         this.scale = scale;
         this.showCount = showCount;
      }
   }

   private record PendingHeadRender(SkinTextures skinTextures, float x, float y, int size) {}

   /** Кэш строк наметага: имя, префикс, HP и их ширины; пересчёт по округлённому ключу здоровья. */
   private record TagData(String name, String prefix, String hpText, float prefixWidth,
                          float nameWidth, float hpWidth, int hpKey) {}
}
