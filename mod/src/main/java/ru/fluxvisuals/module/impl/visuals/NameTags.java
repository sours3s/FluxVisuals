package ru.fluxvisuals.module.impl.visuals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
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
import ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting;
import ru.fluxvisuals.util.other.Mathf;
import ru.fluxvisuals.util.render.TextCache;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

@IModule(
   name = "NameTags",
   description = "Renders advanced and customizable information plates over entities in line of sight",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class NameTags extends Module {
   public static Map<Entity, double[]> entityPositions = new HashMap<>();
   private static final List<NameTags.PendingItemRender> pendingItems = new ArrayList<>();
   private static final List<NameTags.PendingHeadRender> pendingHeads = new ArrayList<>();

   // ===== Кэши для оптимизации горячего цикла (Feature 5) =====
   private static final double MAX_RENDER_DIST_SQ = 96.0 * 96.0;
   /** Результаты canSee (raycast) кэшируются на один тик — raycast дорогой, не нужно его гонять каждый кадр. */
   private static final Map<Entity, Boolean> visibleCache = new HashMap<>();
   /** Строки имени/HP и их ширины кэшируются на сущность; пересчёт только при изменении HP. */
   private static final Map<Entity, NameTags.CachedTag> tagCache = new HashMap<>();
   private static long lastVisibleCacheTime = 0L;

   public MultiBooleanSetting entityType = new MultiBooleanSetting(
      "Targets", new BooleanSetting("Player", true), new BooleanSetting("Mobs", false), new BooleanSetting("Item", true)
   );
   public BooleanSetting armor = new BooleanSetting("Render Armor", true).hidden(() -> !this.entityType.get("Player"));
   public BooleanSetting showMainHand = new BooleanSetting("Main Hand", true).hidden(() -> !this.entityType.get("Player"));
   public BooleanSetting showOffHand = new BooleanSetting("Offhand", true).hidden(() -> !this.entityType.get("Player"));

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
   private static final int GLASS_COLOR = 0x9E030304;
   private static final float MIN_DISTANCE_SCALE = 1.0F;
   private static final float MAX_DISTANCE_SCALE = 1.2F;
   private static final double SCALE_REFERENCE_DISTANCE = 4.0;

   public NameTags() {
      this.addSettings(new Setting[]{this.entityType, this.armor, this.showMainHand, this.showOffHand});
   }

   @EventInit
   public void onWorldLoad(EventChangeWorld e) {
      entityPositions.clear();
      visibleCache.clear();
      tagCache.clear();
   }

   @EventInit
   public void onRender3D(EventRender3D event) {
      this.updatePositions(event.getTickDelta());
   }

   private void updatePositions(float tickDelta) {
      entityPositions.clear();
      if (mc.world == null || mc.player == null) {
         return;
      }

      boolean trackPlayers = this.entityType.get("Player");
      boolean trackMobs = this.entityType.get("Mobs");
      boolean trackItems = this.entityType.get("Item");

      // Raycast-кэш (canSee) живёт один тик — очищаем по смене времени мира.
      long worldTime = mc.world.getTime();
      if (lastVisibleCacheTime != worldTime) {
         lastVisibleCacheTime = worldTime;
         visibleCache.clear();
      }

      Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

      for (Entity entity : mc.world.getEntities()) {
         if (entity == mc.player) continue;
         if (!entity.isAlive() && !(entity instanceof ItemEntity)) continue;

         boolean accept;
         if (entity instanceof PlayerEntity) {
            accept = trackPlayers;
         } else if (entity instanceof ItemEntity) {
            accept = trackItems;
         } else if (entity instanceof LivingEntity) {
            accept = trackMobs;
         } else {
            continue;
         }
         if (!accept) continue;

         // Быстрый отсев по дистанции: вне радиуса не считаем луч и проекцию.
         double dx = entity.getX() - cam.x;
         double dy = entity.getY() - cam.y;
         double dz = entity.getZ() - cam.z;
         if (dx * dx + dy * dy + dz * dz > MAX_RENDER_DIST_SQ) continue;

         // Line-of-sight: не показываем таблички сквозь стены (результат кэшируется на тик).
         boolean visible = visibleCache.computeIfAbsent(entity,
               e -> mc.player != null && mc.player.canSee(e));
         if (!visible) continue;

         double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
         double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
         double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
         double topY = entity instanceof ItemEntity ? y + 0.6 : y + entity.getHeight() + 0.35;
         double bottomY = y - 0.35;
         Vec3d headScreen = Mathf.worldSpaceToScreenSpace(new Vec3d(x, topY, z));
         Vec3d legsScreen = Mathf.worldSpaceToScreenSpace(new Vec3d(x, bottomY, z));
         if (headScreen == null || legsScreen == null) continue;
         if (headScreen.z < 0.0 || headScreen.z > 1.0) continue;
         if (legsScreen.z < 0.0 || legsScreen.z > 1.0) continue;

         entityPositions.put(entity, new double[]{
               headScreen.x, headScreen.y, headScreen.z,
               legsScreen.x, legsScreen.y, legsScreen.z
         });
      }
   }

   @EventInit
   public void onRender2D(EventScreen event) {
      if (mc.world == null || mc.player == null) {
         return;
      }
      Renderer2D renderer = event.renderer();
      DrawContext drawContext = event.drawContext();
      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();

      for (Map.Entry<Entity, double[]> e : entityPositions.entrySet()) {
         Entity entity = e.getKey();
         double[] pos = e.getValue();
         if (pos == null) continue;
         if (pos[2] < 0.0 || pos[2] > 1.0) continue;

         Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
         double distance = cameraPos.distanceTo(entityPos);
         float scale = (float)(SCALE_REFERENCE_DISTANCE / Math.max(distance, 1.0));
         scale = MathHelper.clamp(scale, MIN_DISTANCE_SCALE, MAX_DISTANCE_SCALE);

         float screenX = (float) pos[0] * 2.0F;
         float screenY = (float) pos[1] * 2.0F;

         if (entity instanceof PlayerEntity player) {
            this.renderPlayerTag(renderer, drawContext, player, screenX, screenY, scale, pos);
         } else if (entity instanceof ItemEntity itemEntity) {
            this.renderItemTag(renderer, itemEntity, screenX, screenY, scale);
         } else if (entity instanceof LivingEntity living) {
            this.renderMobTag(renderer, living, screenX, screenY, scale);
         }
      }

      this.renderPendingHeads(drawContext);
      this.renderPendingItems(drawContext);

      // Ограничиваем кэш строк только теми сущностями, что реально отрисованы в этом кадре.
      tagCache.keySet().retainAll(entityPositions.keySet());
   }

   /**
    * Возвращает кэшированные строки имени/HP и их ширины для сущности.
    * Пересчёт только при изменении HP (по округлённому ключу) — экономит String.format и измерение
    * текста каждый кадр в горячем цикле наметагов.
    */
   private static NameTags.CachedTag cachedTag(Renderer2D renderer, LivingEntity entity) {
      float hp = entity.getHealth();
      int hpKey = Math.round(hp * 10.0F);
      NameTags.CachedTag c = tagCache.get(entity);
      if (c == null || c.hpKey != hpKey) {
         String name = entity.getName().getString();
         String hpText = String.format("%.1f", hp).replace(',', '.') + "HP";
         float nameWidth = TextCache.width(renderer, FontRegistry.INTER_MEDIUM, name, TAG_FONT_SIZE);
         float hpWidth = TextCache.width(renderer, FontRegistry.INTER_MEDIUM, hpText, TAG_FONT_SIZE);
         c = new NameTags.CachedTag(name, hpText, nameWidth, hpWidth, hpKey);
         tagCache.put(entity, c);
      }
      return c;
   }

   private void renderPlayerTag(Renderer2D renderer, DrawContext drawContext, PlayerEntity entity,
                                float screenX, float screenY, float scale, double[] pos) {
      NameTags.CachedTag cached = cachedTag(renderer, entity);
      String name = cached.name;
      String hpText = cached.hpText;
      float hp = entity.getHealth();
      float maxHp = Math.max(entity.getMaxHealth(), 1.0F);
      boolean isFriend = FluxVisualsClient.get.friendManager.isFriend(name);
      String friendTag = isFriend ? "[F]" : "";

      float friendTagWidth = isFriend ? TextCache.width(renderer, FontRegistry.INTER_MEDIUM, friendTag, TAG_FONT_SIZE) : 0.0F;
      float friendGap = isFriend ? TextCache.width(renderer, FontRegistry.INTER_MEDIUM, " ", TAG_FONT_SIZE) : 0.0F;
      float nameWidth = cached.nameWidth;
      float gap = 5.0F;
      float hpWidth = cached.hpWidth;
      float contentWidth = friendTagWidth + friendGap + nameWidth + gap + hpWidth;
      float pillWidth = contentWidth + HEAD_SIZE + HEAD_GAP + TAG_PADDING_X * 2.0F;

      renderer.pushTranslation(screenX, screenY);
      renderer.pushScale(scale, scale);

      drawGlassTag(renderer, -pillWidth / 2.0F, -TAG_HEIGHT / 2.0F, pillWidth, TAG_HEIGHT, TAG_RADIUS);
      float textY = centeredTextBaseline(renderer, name, -TAG_HEIGHT / 2.0F, TAG_HEIGHT, TAG_FONT_SIZE);
      float textX = -pillWidth / 2.0F + TAG_PADDING_X + HEAD_SIZE + HEAD_GAP;
      if (isFriend) {
         renderer.text(FontRegistry.INTER_MEDIUM, textX, textY, TAG_FONT_SIZE, friendTag,
               Renderer2D.ColorUtil.getColor(40, 255, 40, 255));
         textX += friendTagWidth + friendGap;
      }
      renderer.text(FontRegistry.INTER_MEDIUM, textX, textY, TAG_FONT_SIZE, name, Renderer2D.ColorUtil.getTextColor(1, 1));
      textX += nameWidth + gap;
      renderer.text(FontRegistry.INTER_MEDIUM, textX, textY, TAG_FONT_SIZE, hpText, getHealthColor(hp / maxHp));

      this.queuePlayerHead(entity, screenX, screenY, scale, -pillWidth / 2.0F + TAG_PADDING_X + HEAD_SIZE / 2.0F, 0.0F);

      if (this.armor.get()) {
         this.queueArmorIcons(renderer, entity, screenX, screenY, scale);
      }

      renderer.popScale();
      renderer.popTransform();

      if (this.showMainHand.get() || this.showOffHand.get()) {
         float legsX = (float) pos[3] * 2.0F;
         float legsY = (float) pos[4] * 2.0F;
         renderer.pushTranslation(legsX, legsY);
         renderer.pushScale(scale, scale);
         this.drawPlayerHands(renderer, entity, legsX, legsY, scale);
         renderer.popScale();
         renderer.popTransform();
      }
   }

   private void renderMobTag(Renderer2D renderer, LivingEntity entity, float screenX, float screenY, float scale) {
      NameTags.CachedTag cached = cachedTag(renderer, entity);
      String name = cached.name;
      String hpText = cached.hpText;
      float nameWidth = cached.nameWidth;
      float gap = 5.0F;
      float hpWidth = cached.hpWidth;
      float pillWidth = nameWidth + gap + hpWidth + TAG_PADDING_X * 2.0F;

      renderer.pushTranslation(screenX, screenY);
      renderer.pushScale(scale, scale);
      drawGlassTag(renderer, -pillWidth / 2.0F, -TAG_HEIGHT / 2.0F, pillWidth, TAG_HEIGHT, TAG_RADIUS);
      float textY = centeredTextBaseline(renderer, name, -TAG_HEIGHT / 2.0F, TAG_HEIGHT, TAG_FONT_SIZE);
      float textX = -pillWidth / 2.0F + TAG_PADDING_X;
      renderer.text(FontRegistry.INTER_MEDIUM, textX, textY, TAG_FONT_SIZE, name, Renderer2D.ColorUtil.getTextColor(1, 1));
      textX += nameWidth + gap;
      renderer.text(FontRegistry.INTER_MEDIUM, textX, textY, TAG_FONT_SIZE, hpText, getHealthColor(entity.getHealth() / Math.max(entity.getMaxHealth(), 1.0F)));
      renderer.popScale();
      renderer.popTransform();
   }

   private void renderItemTag(Renderer2D renderer, ItemEntity itemEntity, float screenX, float screenY, float scale) {
      ItemStack stack = itemEntity.getStack();
      if (stack.isEmpty()) return;

      String name = stack.getName().getString();
      int count = stack.getCount();
      String countText = count > 1 ? " x" + count : "";
      float nameWidth = renderer.measureText(FontRegistry.INTER_MEDIUM, name, TAG_FONT_SIZE).width;
      float countWidth = countText.isEmpty() ? 0.0F
            : renderer.measureText(FontRegistry.INTER_MEDIUM, countText, TAG_FONT_SIZE).width;
      float pillWidth = nameWidth + countWidth + TAG_PADDING_X * 2.0F;

      renderer.pushTranslation(screenX, screenY);
      renderer.pushScale(scale, scale);
      drawGlassTag(renderer, -pillWidth / 2.0F, -TAG_HEIGHT / 2.0F, pillWidth, TAG_HEIGHT, TAG_RADIUS);
      float textY = centeredTextBaseline(renderer, name, -TAG_HEIGHT / 2.0F, TAG_HEIGHT, TAG_FONT_SIZE);
      float textX = -pillWidth / 2.0F + TAG_PADDING_X;
      renderer.text(FontRegistry.INTER_MEDIUM, textX, textY, TAG_FONT_SIZE, name, Renderer2D.ColorUtil.getTextColor(1, 1));
      if (!countText.isEmpty()) {
         renderer.text(FontRegistry.INTER_MEDIUM, textX + nameWidth, textY, TAG_FONT_SIZE, countText,
               Renderer2D.ColorUtil.getMainColor(1, 1));
      }
      renderer.popScale();
      renderer.popTransform();
   }

   private void queueArmorIcons(Renderer2D renderer, PlayerEntity player, float screenX, float screenY, float scale) {
      if (player.getArmor() == 0) return;
      EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
      List<ItemStack> armorStacks = new ArrayList<>();
      for (EquipmentSlot slot : slots) {
         ItemStack stack = player.getEquippedStack(slot);
         if (!stack.isEmpty()) {
            armorStacks.add(stack);
         }
      }
      if (armorStacks.isEmpty()) return;

      int slotCount = armorStacks.size();
      float totalWidth = slotCount == 1 ? ARMOR_ICON_SIZE : (slotCount - 1) * ARMOR_ICON_SPACING + ARMOR_ICON_SIZE;
      float firstCenterX = -totalWidth / 2.0F;
      float armorTagWidth = totalWidth + 10.0F;
      float armorTagY = -TAG_HEIGHT / 2.0F - ARMOR_TAG_HEIGHT - 1.5F;
      drawGlassTag(renderer, -armorTagWidth / 2.0F, armorTagY, armorTagWidth, ARMOR_TAG_HEIGHT, 8.0F);

      for (int i = 0; i < slotCount; i++) {
         ItemStack stack = armorStacks.get(i);
         float localCenterX = firstCenterX + ARMOR_ICON_SIZE / 2.0F + i * ARMOR_ICON_SPACING;
         float localCenterY = armorTagY + ARMOR_TAG_HEIGHT / 2.0F;
         float worldX = screenX + localCenterX * scale;
         float worldY = screenY + localCenterY * scale;
         float itemPx = 16.0F * 0.5F * scale;
         float drawX = (worldX - itemPx) / 2.0F;
         float drawY = (worldY - itemPx) / 2.0F;
         pendingItems.add(new NameTags.PendingItemRender(player, stack, drawX, drawY, i, 0.5F * scale));
      }
   }

   private void queuePlayerHead(PlayerEntity player, float screenX, float screenY, float scale, float localCenterX, float localCenterY) {
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
      pendingHeads.add(new NameTags.PendingHeadRender(skinTextures, (worldX - HEAD_SIZE * scale / 2.0F) / 2.0F, (worldY - HEAD_SIZE * scale / 2.0F) / 2.0F, Math.max(1, Math.round(HEAD_SIZE * scale / 2.0F))));
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
            drawContext.getMatrices().popMatrix();
         }
         pendingItems.clear();
      }
   }

   private void drawPlayerHands(Renderer2D renderer, PlayerEntity player, float screenX, float screenY, float scale) {
      List<ItemStack> items = new ArrayList<>();
      if (this.showMainHand.get() && !player.getMainHandStack().isEmpty()) {
         items.add(player.getMainHandStack());
      }
      if (this.showOffHand.get() && !player.getOffHandStack().isEmpty()) {
         items.add(player.getOffHandStack());
      }
      if (items.isEmpty()) return;

      float y = 8.0F;
      for (int i = 0; i < items.size(); i++) {
         ItemStack stack = items.get(i);
         String text = stack.getName().getString();
         float textWidth = renderer.measureText(FontRegistry.INTER_MEDIUM, text, TAG_FONT_SIZE).width;
         float pillWidth = HAND_ICON_SIZE + HAND_ICON_GAP + textWidth + TAG_PADDING_X * 2.0F;
         drawGlassTag(renderer, -pillWidth / 2.0F, y, pillWidth, TAG_HEIGHT, TAG_RADIUS);
         float iconCenterX = -pillWidth / 2.0F + TAG_PADDING_X + HAND_ICON_SIZE / 2.0F;
         float iconCenterY = y + TAG_HEIGHT / 2.0F;
         float worldX = screenX + iconCenterX * scale;
         float worldY = screenY + iconCenterY * scale;
         float itemPx = 16.0F * 0.5F * scale;
         pendingItems.add(new NameTags.PendingItemRender(player, stack, (worldX - itemPx) / 2.0F, (worldY - itemPx) / 2.0F, 20 + i, 0.5F * scale));
         renderer.text(FontRegistry.INTER_MEDIUM, -pillWidth / 2.0F + TAG_PADDING_X + HAND_ICON_SIZE + HAND_ICON_GAP, centeredTextBaseline(renderer, text, y, TAG_HEIGHT, TAG_FONT_SIZE),
               TAG_FONT_SIZE, text, Renderer2D.ColorUtil.getTextColor(1, 1));
         y += TAG_HEIGHT + 3.0F;
      }
   }

   private static void drawGlassTag(Renderer2D renderer, float x, float y, float width, float height, float radius) {
      renderer.prepareBlur(GLASS_BLUR_STRENGTH);
      renderer.blur(x, y, width, height, radius, GLASS_BLUR_ALPHA);
      renderer.rect(x, y, width, height, radius, GLASS_COLOR);
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
      PlayerEntity player;
      ItemStack stack;
      float x;
      float y;
      int seed;
      float scale;

      PendingItemRender(PlayerEntity player, ItemStack stack, float x, float y, int seed, float scale) {
         this.player = player;
         this.stack = stack;
         this.x = x;
         this.y = y;
         this.seed = seed;
         this.scale = scale;
      }
   }

   private record PendingHeadRender(SkinTextures skinTextures, float x, float y, int size) {}

   /** Кэш строк наметага: имя, HP-текст и их ширины; пересчёт по округлённому ключу здоровья. */
   private record CachedTag(String name, String hpText, float nameWidth, float hpWidth, int hpKey) {}
}
