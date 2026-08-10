package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.render.animation.util.Animation;
import ru.fluxvisuals.util.render.animation.util.Easings;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.animation.AnimationMath;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Target HUD — полный перенос GodWeer TargetHudElement.
 *
 * <p>Компактный макет GodWeer: голова 20px, основная панель 28px высотой,
 * предметы сверху (14.5px бокс), имя справа от головы со скроллом, HP-бар с
 * градиентом клиентского цвета {@code getMainColor(1, 0)} → {@code getMainColor(1, 40)},
 * абсорбция золотом, частицы {@code Particles2DEngine} (dir/roll, sin/cos, затухание).
 *
 * <p>Голова рисуется плоским {@code PlayerSkinDrawer.draw(5-arg)} в логических координатах
 * GUI (без 3D-рендера и без матричных трансформаций) — это устраняет краш при F5 и
 * «голову за пределами панели».
 *
 * <p>Таргет только из-под прицела (не сквозь стены); при простое показывается сам игрок.
 */
@Environment(EnvType.CLIENT)
public final class TargetHUD {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   // === Настройки (регистрируются в Hud.java) ===
   public static final ModeSetting style = new ModeSetting("TargetHUD Style", "Glass", "Dark", "Glass");
   public static final BooleanSetting showItems = new BooleanSetting("TargetHUD Items", true);
   public static final BooleanSetting showOnHover = new BooleanSetting("TargetHUD Show on hover", true);
   public static final BooleanSetting particles = new BooleanSetting("TargetHUD Particles", true);
   public static final SliderSetting scale = new SliderSetting("TargetHUD Scale", 1.0F, 0.5F, 2.0F, 0.05F, false);

   // === Advantage indicator ===
   public static final BooleanSetting advantage = new BooleanSetting("Combat Advantage", true);
   public static final ModeSetting advantageMode = new ModeSetting("Advantage Mode", "Bar", "Bar", "Badge");
   public static final SliderSetting advHealthWeight = new SliderSetting("Health Weight", 0.5F, 0.0F, 1.0F, 0.05F, false);
   public static final SliderSetting advArmorWeight = new SliderSetting("Armor Weight", 0.25F, 0.0F, 1.0F, 0.05F, false);
   public static final SliderSetting advDamageWeight = new SliderSetting("Damage Weight", 0.25F, 0.0F, 1.0F, 0.05F, false);

   // === Константы макета (GodWeer TargetHudElement) ===
   private static final float ITEM_BOX = 14.5F;
   private static final float SPACING = 1.5F;
   private static final float MAIN_HEIGHT = 28.0F;   // высота основной панели
   private static final float HEAD_SIZE = 20.0F;     // размер головы
   private static final float WIDTH = 104.0F;        // ширина панели

   // === Состояние ===
   private static final Animation openAnimation = new Animation();
   private static final Particles2DEngine particlesEngine = new Particles2DEngine();
   private static LivingEntity prevTarget = null;
   private static LivingEntity lastHoveredEntity = null;
   private static long lastHoverTime = 0L;
   private static float healthAnim = 0.0F;
   private static float absorptionAnim = 0.0F;
   private static long scrollDelay = 0L;
   private static int scrollDir = 1;
   private static float scrollAnim = 0.0F;

   private static final int HP_TEXT_COLOR = 0xFFC8C8C8;
   private static final int ABSORPTION_COLOR = 0xFFFFD700;

   private TargetHUD() {
   }

   /** Живая сущность под прицелом. Только crosshair-hit, без «сквозь стены»; игрок исключён. */
   private static LivingEntity getHoveredEntity() {
      Entity raw = null;
      if (mc.crosshairTarget instanceof EntityHitResult ehr) {
         raw = ehr.getEntity();
      }
      if (raw == null && mc.targetedEntity != null && mc.targetedEntity != mc.player) {
         raw = mc.targetedEntity;
      }
      if (raw instanceof LivingEntity le && le != mc.player) {
         return le;
      }
      return null;
   }

   public static void targetHUD(Renderer2D r2, DrawContext drawContext) {
      if (mc == null || mc.player == null || mc.world == null || mc.getWindow() == null) {
         return;
      }
      float s = scale.get();
      float width = WIDTH * s;
      float mainHeight = MAIN_HEIGHT * s;
      float headSize = HEAD_SIZE * s;

      boolean isInChat = mc.currentScreen instanceof ChatScreen;
      LivingEntity hovered = getHoveredEntity();
      long now = System.currentTimeMillis();
      if (hovered != null) {
         lastHoverTime = now;
         lastHoveredEntity = hovered;
      }

      boolean isHovering = showOnHover.get() && lastHoveredEntity != null && (now - lastHoverTime < 2000L);
      boolean shown = isInChat || hovered != null || isHovering;

      if (hovered != null) prevTarget = hovered;
      else if (isHovering) prevTarget = lastHoveredEntity;
      else if (isInChat || prevTarget == null) prevTarget = mc.player;

      openAnimation.update();
      openAnimation.run(shown ? 1.0 : 0.0, 0.4F, Easings.CIRC_OUT);
      float anim = (float) openAnimation.get();
      if (anim < 0.01F) return;

      // Предметы цели (GodWeer: main, head, chest, legs, feet, offhand)
      List<ItemStack> items = new ArrayList<>();
      if (showItems.get() && prevTarget != null) {
         items.add(prevTarget.getMainHandStack());
         items.add(prevTarget.getEquippedStack(EquipmentSlot.HEAD));
         items.add(prevTarget.getEquippedStack(EquipmentSlot.CHEST));
         items.add(prevTarget.getEquippedStack(EquipmentSlot.LEGS));
         items.add(prevTarget.getEquippedStack(EquipmentSlot.FEET));
         items.add(prevTarget.getOffHandStack());
         items.removeIf(ItemStack::isEmpty);
      }

      float totalItemsW = items.size() * ITEM_BOX * s + Math.max(0, items.size() - 1) * SPACING * s;
      float itemStartY = 0.0F;
      float mainBoxY = showItems.get() && !items.isEmpty() ? ITEM_BOX * s + 2.0F * s : 0.0F;

      // Drag-область: по умолчанию чуть ниже середины экрана (под прицелом).
      float panelH = mainBoxY + mainHeight + 6.0F * s;
      int fbW = mc.getWindow().getFramebufferWidth();
      int fbH = mc.getWindow().getFramebufferHeight();
      float defX = Math.max(0.0F, (fbW - width) / 2.0F);
      float defY = Math.max(0.0F, fbH * 0.55F);
      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag("targetHUD", defX, defY, width, panelH);
      float x = session.positionX();
      float y = session.positionY();
      float boxY = y + mainBoxY;

      // Apply HUD scale matrix (like PotionsHUD)
      float guiScale = (float) mc.getWindow().getScaleFactor();
      if (guiScale <= 0.0F) guiScale = 1.0F;
      float hudScale = session.scale();

      drawContext.getMatrices().pushMatrix();
      drawContext.getMatrices().translate(x / guiScale, y / guiScale);
      drawContext.getMatrices().scale(hudScale, hudScale);
      drawContext.getMatrices().translate(-x / guiScale, -y / guiScale);

      r2.pushAlpha(anim);

      // Предметы сверху
      if (showItems.get() && !items.isEmpty()) {
         float itemStartX = x + (width / 2.0F) - (totalItemsW / 2.0F);
         for (int i = 0; i < items.size(); i++) {
            float ix = itemStartX + i * (ITEM_BOX * s + SPACING * s);
            drawStyle(r2, ix, y + itemStartY, ITEM_BOX * s, ITEM_BOX * s, anim);
            if (drawContext != null) {
               renderItem(drawContext, items.get(i), ix + 2.0F * s, y + itemStartY + 2.0F * s, ITEM_BOX * s - 4.0F * s);
            }
         }
      }

      // Основная панель
      drawStyle(r2, x, boxY, width, mainHeight, anim);

      // Голова (плоский скин, логические GUI-координаты — безопасно при F5)
      float headX = x + 4.0F * s;
      float headY = boxY + 4.0F * s;
      drawHead(r2, drawContext, prevTarget, headX, headY, headSize, anim);

      // Имя со скроллом (GodWeer: baseline mainBoxY + 4.5)
      String name = prevTarget instanceof CreeperEntity ? "Грустный крипер" : prevTarget.getName().getString();
      float nameX = headX + headSize + 5.0F * s;
      float nameW = width - (headSize + 15.0F * s);
      float nameSize = 14.0F * s;
      float tWidth = r2.measureText(FontRegistry.INTER_MEDIUM, name, nameSize).width;

      if (scrollDelay == 0L) scrollDelay = System.currentTimeMillis();
      if (System.currentTimeMillis() - scrollDelay > 1000L) {
         scrollDelay = System.currentTimeMillis();
         scrollDir = -scrollDir;
      }
      float targetOffset = scrollDir > 0 ? Math.max(0.0F, tWidth - nameW) : 0.0F;
      scrollAnim = AnimationMath.animation(scrollAnim, targetOffset, 0.05F);

      r2.pushClipRect((int) nameX, (int) boxY, (int) nameW, (int) mainHeight);
      r2.text(FontRegistry.INTER_MEDIUM, nameX - scrollAnim, boxY + 4.5F * s, nameSize, name, -1);
      r2.popClipRect();

      // HP-бар (GodWeer: barY = boxY + mainHeight - 8.5, barH = 3.5)
      float barX = nameX;
      float barY = boxY + mainHeight - 8.5F * s;
      float barW = nameW;
      float barH = 3.5F * s;

      float maxHp = prevTarget.getMaxHealth();
      float hpFactor = Math.min(Math.max(healthAnim / maxHp, 0.0F), 1.0F);

      // Фон бара
      r2.rect(barX, barY, barW, barH, 4.0F * s, Renderer2D.ColorUtil.replAlpha(0xFF000000, (int) (60 * anim)));

      // Градиент клиентского цвета: getColor(0) → getColor(40) как в GodWeer
      int c1 = Renderer2D.ColorUtil.getMainColor(1, 0);
      int c2 = Renderer2D.ColorUtil.getMainColor(1, 40);
      drawHpGradient(r2, barX, barY, barW * hpFactor, barH, c1, c2, anim);
      if (particles.get() && hpFactor > 0.0F) {
         spawnHealthParticles(barX + barW * hpFactor, barY + barH / 2.0F, c1);
      }

      // Абсорбция (золото, от правого края)
      float absWidth = 0.0F;
      if (absorptionAnim > 0.1F) {
         float absFactor = Math.min(Math.max(absorptionAnim / maxHp, 0.0F), 1.0F);
         absWidth = barW * absFactor;
         int gold1 = Renderer2D.ColorUtil.replAlpha(0xFFFFD700, (int) (200 * anim));
         int gold2 = Renderer2D.ColorUtil.replAlpha(0xFFFFA500, (int) (180 * anim));
         drawHpGradient(r2, barX + barW - absWidth, barY, absWidth, barH, gold1, gold2, anim);
         if (particles.get()) {
            spawnHealthParticles(barX + barW - absWidth, barY + barH / 2.0F, gold1);
         }
      }

      // Частицы
      if (particles.get()) {
         particlesEngine.render(r2);
      }

      // HP-текст (компактно, под баром внутри панели)
      float hValue = (float) (Math.round(healthAnim * 10.0F) / 10.0F);
      float aValue = (float) (Math.round(absorptionAnim * 10.0F) / 10.0F);
      String hStr = hValue + "";
      String aStr = aValue > 0 ? " + (" + aValue + ")" : "";
      String suffix = " HP";
      float hpTextSize = 9.0F * s;
      float fullTextW = r2.measureText(FontRegistry.INTER_MEDIUM, hStr + aStr + suffix, hpTextSize).width;
      float drawX = barX + barW - fullTextW;
      float drawY = barY + barH + 2.0F * s;
      r2.text(FontRegistry.INTER_MEDIUM, drawX, drawY, hpTextSize, hStr, HP_TEXT_COLOR);
      float off = r2.measureText(FontRegistry.INTER_MEDIUM, hStr, hpTextSize).width;
      if (aValue > 0) {
         r2.text(FontRegistry.INTER_MEDIUM, drawX + off, drawY, hpTextSize, aStr, ABSORPTION_COLOR);
         off += r2.measureText(FontRegistry.INTER_MEDIUM, aStr, hpTextSize).width;
      }
      r2.text(FontRegistry.INTER_MEDIUM, drawX + off, drawY, hpTextSize, suffix,
            Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, (int) (180 * anim)));

      // Advantage indicator (под панелью)
      if (advantage.get() && prevTarget != mc.player) {
         renderAdvantage(r2, x, y, width, mainHeight, anim, prevTarget);
      }

      // Плавное движение значений к реальным
      healthAnim = AnimationMath.animation(healthAnim, prevTarget.getHealth(), 0.1F);
      absorptionAnim = AnimationMath.animation(absorptionAnim, prevTarget.getAbsorptionAmount(), 0.1F);

      drawContext.getMatrices().popMatrix();
      r2.popAlpha();
      DraggableManager.getInstance().endDrag(session);
   }

   /** Плоский скин-аватар в логических координатах GUI (без 3D, без матричных трюков). */
   private static void drawHead(Renderer2D r2, DrawContext drawContext, LivingEntity target, float headX, float headY, float headSize, float anim) {
      if (target == null) return;
      if (drawContext == null) {
         // нет DrawContext — рисуем серый квадрат через r2
         r2.rect(headX, headY, headSize, headSize, 4.0F, Renderer2D.ColorUtil.replAlpha(0xFF333333, (int) (anim * 200.0F)));
         return;
      }
      try {
         if (target instanceof PlayerEntity player) {
            var skinSupplier = mc.getSkinProvider().supplySkinTextures(player.getGameProfile(), true);
            if (skinSupplier != null) {
               SkinTextures textures = skinSupplier.get();
               if (textures == null) return;
               float guiScale = mc.getWindow().getScaleFactor();
               int hx = Math.round(headX / guiScale);
               int hy = Math.round(headY / guiScale);
               int hs = Math.max(1, Math.round(headSize / guiScale));
               int color = Renderer2D.ColorUtil.replAlpha(-1, (int) (anim * 255.0F));
               PlayerSkinDrawer.draw(drawContext, textures, hx, hy, hs, color);
            }
         } else {
            r2.rect(headX, headY, headSize, headSize, 4.0F, Renderer2D.ColorUtil.replAlpha(0xFF333333, (int) (anim * 200.0F)));
         }
      } catch (Exception ignored) {
         // Никогда не роняем игру (F5, отвалившийся скин и т.п.)
      }
   }

   /** Стиль панели: blur + Dark (плотный фон) или Glass (прозрачный + обводка). */
   private static void drawStyle(Renderer2D r2, float rx, float ry, float rw, float rh, float alpha) {
      float round = 5.0F;
      if (Hud.blur.get("HUD")) {
         r2.prepareBlur(23.0F);
         r2.blur(rx, ry, rw, rh, round, alpha);
      }
      if (style.is("Dark")) {
         int bg = Renderer2D.ColorUtil.replAlpha(0xFF141419, (int) (180 * alpha));
         r2.rect(rx, ry, rw, rh, round, bg);
         int out = Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, (int) (15 * alpha));
         r2.rectOutline(rx, ry, rw, rh, round, out, 0.4F);
      } else {
         int bg = Renderer2D.ColorUtil.replAlpha(0xFF3E3E47, 0);
         r2.rect(rx, ry, rw, rh, round, bg);
         int outAlpha = (int) Math.min(Math.max(25 * alpha, 0), 255);
         int out = Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, outAlpha);
         r2.rectOutline(rx, ry, rw, rh, round, out, 0.4F);
      }
   }

   /** Градиентный бар rect(c1, c1, c2, c2): левая часть c1, правая c2. */
   private static void drawHpGradient(Renderer2D r2, float x, float y, float w, float h, int c1, int c2, float anim) {
      if (w <= 0.0F || h <= 0.0F) return;
      float mid = w / 2.0F;
      r2.rect(x, y, mid, h, 4.0F, Renderer2D.ColorUtil.replAlpha(c1, (int) (255 * anim)));
      r2.rect(x + mid, y, w - mid, h, 4.0F, Renderer2D.ColorUtil.replAlpha(c2, (int) (255 * anim)));
   }

   private static void renderItem(DrawContext drawContext, ItemStack stack, float ix, float iy, float size) {
      if (drawContext == null) return;
      drawContext.getMatrices().pushMatrix();
      drawContext.getMatrices().translate(ix, iy);
      float scale2 = size / 16.0F;
      drawContext.getMatrices().scale(scale2, scale2);
      drawContext.drawItem(stack, 0, 0);
      drawContext.getMatrices().popMatrix();
   }

   /** Спавн частиц у края HP-бара (GodWeer spawnHealthParticles). */
   private static void spawnHealthParticles(float x, float y, int color) {
      if (Math.random() > 0.8) {
         float dir = (float) (Math.random() * 360.0F);
         float roll = (float) (Math.random() - 0.5F) * 5.0F;
         long alive = 500L + (long) (Math.random() * 500L);
         particlesEngine.add(
               x, y,
               (float) (Math.random() - 0.5F) * 0.5F,
               (float) (Math.random() - 0.2F) * 0.5F,
               dir, roll, alive, color);
      }
   }

   /** Движок частиц (порт GodWeer Particles2DEngine). */
   @Environment(EnvType.CLIENT)
   private static final class Particles2DEngine {
      private final List<Particle2D> particles = new ArrayList<>();

      void add(float x, float y, float vx, float vy, float dir, float roll, long alive, int color) {
         particles.add(new Particle2D(x, y, vx, vy, dir, roll, alive, color));
      }

      void clear() {
         particles.clear();
      }

      void render(Renderer2D r2) {
         for (Particle2D p : particles) {
            p.render(r2);
         }
         particles.removeIf(Particle2D::removed);
      }
   }

   /** Частица (порт GodWeer Particle2D: dir/roll, движение по sin/cos, затухание, цвет клиента). */
   @Environment(EnvType.CLIENT)
   private static final class Particle2D {
      private float x;
      private float y;
      private final float vx;
      private final float vy;
      private float dir;
      private final float roll;
      private final long deleteIn;
      private final int color;

      Particle2D(float x, float y, float vx, float vy, float dir, float roll, long alive, int color) {
         this.x = x;
         this.y = y;
         this.vx = vx;
         this.vy = vy;
         this.dir = dir;
         this.roll = roll;
         this.deleteIn = System.currentTimeMillis() + alive;
         this.color = color;
      }

      boolean removed() {
         return System.currentTimeMillis() > deleteIn;
      }

      void render(Renderer2D r2) {
         dir += roll;
         x += (float) (Math.sin(dir * Math.PI / 180.0) * vx);
         y += (float) (Math.cos(dir * Math.PI / 180.0) * vy);
         float f = Math.min(255.0F, Math.max(0, deleteIn - System.currentTimeMillis())) / 255.0F;
         int col = Renderer2D.ColorUtil.replAlpha(color, (int) (f * 255.0F));
         // мягкое свечение (аналог ghost-glow.png)
         r2.circle(x, y, 2.5F, 0.0F, 1.0F, col);
         r2.circle(x, y, 1.5F, 0.0F, 1.0F, Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, (int) (f * 200.0F)));
      }
   }

   /** Рендер индикатора преимущества в бою. */
   private static void renderAdvantage(Renderer2D r2, float x, float y, float width, float mainHeight, float anim, LivingEntity target) {
      float playerHpRatio = mc.player.getHealth() / Math.max(mc.player.getMaxHealth(), 1.0F);
      float targetHpRatio = target.getHealth() / Math.max(target.getMaxHealth(), 1.0F);
      float hpDiff = playerHpRatio - targetHpRatio;

      float playerDamage = getDamageScore(mc.player);
      float targetDamage = getDamageScore(target);
      float damageDiff = 0.0F;
      if (playerDamage > 0 && targetDamage > 0) {
         damageDiff = (playerDamage - targetDamage) / Math.max(playerDamage, targetDamage);
      }

      float playerArmor = getArmorScore(mc.player);
      float targetArmor = getArmorScore(target);
      float armorDiff = 0.0F;
      if (playerArmor > 0 && targetArmor > 0) {
         armorDiff = (playerArmor - targetArmor) / Math.max(playerArmor, targetArmor);
      }

      float wHealth = advHealthWeight.get();
      float wArmor = advArmorWeight.get();
      float wDamage = advDamageWeight.get();
      float sum = wHealth + wArmor + wDamage;
      if (sum <= 0.0F) return;

      float score = (hpDiff * wHealth + armorDiff * wArmor + damageDiff * wDamage) / sum; // -1..1
      score = Math.max(-1.0F, Math.min(1.0F, score));

      // Бар под HP-баром цели
      float barY = y + mainHeight + 6.0F;
      float barW = width;
      float barH = 4.0F;
      float centerX = x + barW / 2.0F;
      float fillW = Math.abs(score) * barW / 2.0F;

      int good = Renderer2D.ColorUtil.getColor(40, 255, 40, Math.round(255 * anim));
      int bad = Renderer2D.ColorUtil.getColor(255, 55, 55, Math.round(255 * anim));
      int mid = Renderer2D.ColorUtil.getColor(255, 218, 45, Math.round(255 * anim));

      // Фон бара
      r2.rect(x, barY, barW, barH, 3.0F, Renderer2D.ColorUtil.replAlpha(0xFF000000, Math.round(80 * anim)));

      if (score > 0.05F) { // преимущество игрока — зелёная отрез слева направо
         drawHpGradient(r2, centerX, barY, fillW, barH, good, good, anim);
      } else if (score < -0.05F) { // преимущество цели — красная отрез справа налево
         drawHpGradient(r2, centerX - fillW, barY, fillW, barH, bad, bad, anim);
      } else {
         drawHpGradient(r2, centerX - fillW, barY, fillW, barH, mid, mid, anim);
      }

      // Надпись
      if (advantageMode.is("Badge")) {
         String label;
         int labelColor;
         if (score > 0.15F) {
            label = "ПРЕИМУЩЕСТВО";
            labelColor = good;
         } else if (score < -0.15F) {
            label = "ОПАСНОСТЬ";
            labelColor = bad;
         } else {
            label = "РАВНО";
            labelColor = mid;
         }
         float labelX = centerX - r2.measureText(FontRegistry.INTER_SEMIBOLD, label, 12.0F).width / 2.0F;
         float labelY = barY - 18.0F;
         r2.text(FontRegistry.INTER_SEMIBOLD, labelX, labelY, 12.0F, label, labelColor);
      }
   }

   private static float getDamageScore(LivingEntity entity) {
      float attr = (float) entity.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
      ItemStack stack = entity.getMainHandStack();
      float weapon = stack != null && !stack.isEmpty() ? (float) stack.getDamage() : 0.0F;
      return attr + weapon;
   }

   private static float getArmorScore(LivingEntity entity) {
      float armor = (float) entity.getAttributeValue(EntityAttributes.ARMOR);
      float toughness = (float) entity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
      float total = 0.0F;
      for (EquipmentSlot slot : EquipmentSlot.values()) {
         if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            ItemStack s = entity.getEquippedStack(slot);
            if (s != null && !s.isEmpty()) {
               total += (float) s.getDamage();
            }
         }
      }
      return armor + toughness + total;
   }
}
