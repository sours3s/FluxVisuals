package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;
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
 * Target HUD — полная реализация из GodWeer (TargetHudElement)
 * <p>Голова 20px, основная панель 28px высотой,
 * предметы сверху (14.5px бокс), имя справа от головы со скроллом, HP-бар с
 * градиентом клиентского цвета, абсорбция золотом, частицы.
 * <p>Таргет только из-под прицела; при простое показывается сам игрок.
 */
@Environment(EnvType.CLIENT)
public final class TargetHUD {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   // === Настройки ===
   public static final ModeSetting targetMode = new ModeSetting("TargetHUD Mode", "Normal", "Normal", "Minimal");
   public static final ModeSetting style = new ModeSetting("TargetHUD Style", "Glass", "Dark", "Glass");
   public static final BooleanSetting showItems = new BooleanSetting("TargetHUD Show Items", true);
   public static final BooleanSetting showOnHover = new BooleanSetting("TargetHUD Show on Hover", true);
   public static final BooleanSetting particles = new BooleanSetting("TargetHUD Particles", true);
   public static final SliderSetting scale = new SliderSetting("TargetHUD Scale", 1.0F, 0.5F, 2.0F, 0.05F, false);

   // === Константы макета (как в доноре) ===
   private static final float ITEM_BOX = 14.5F;
   private static final float SPACING = 1.5F;
   private static final float MAIN_HEIGHT = 28.0F;
   private static final float HEAD_SIZE = 20.0F;
   private static final float WIDTH = 110.0F;

   // === Состояние ===
   private static final Animation animation = new Animation();
   private static final Particles2DEngine particlesEngine = new Particles2DEngine();
   private static LivingEntity prevTarget = null;
   private static LivingEntity lastHoveredEntity = null;
   private static long lastHoverTime = 0L;
   private static float healthAnim = 0.0F;
   private static float absorptionAnim = 0.0F;
   private static float healthFactorAnim = 0.0F;
   private static int particlesToSpawn = 0;
   private static long scrollDelay = 0L;
   private static int scrollDir = 1;
   private static float scrollAnim = 0.0F;

   private TargetHUD() {
   }

   /** Вызывается из AttackEvent для спавна частиц при попадании в цель. */
   public static void onAttack(LivingEntity target) {
      if (target == prevTarget && particles.get()) {
         particlesToSpawn += 10;
         healthFactorAnim = (float) (Math.random() * 20.0F);
      }
   }

   /** Жевая сущность под прицелом. */
   private static LivingEntity getHoveredEntity() {
      if (mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult ehr) {
         if (ehr.getEntity() instanceof LivingEntity le && le != mc.player && le.isAlive()) {
            return le;
         }
      }
      if (mc.targetedEntity instanceof LivingEntity le && le != mc.player && le.isAlive()) {
         return le;
      }
      return null;
   }

   public static void targetHUD(Renderer2D r2, DrawContext drawContext) {
      if (mc == null || mc.player == null || mc.world == null || mc.getWindow() == null) {
         return;
      }
      try {
         targetHUDInternal(r2, drawContext);
      } catch (Throwable t) {
         System.err.println("[TargetHUD] render error: " + t);
         t.printStackTrace();
      }
   }

   private static void targetHUDInternal(Renderer2D r2, DrawContext drawContext) {
      LivingEntity target = getHoveredEntity();
      long now = System.currentTimeMillis();
      if (target != null) {
         lastHoverTime = now;
         lastHoveredEntity = target;
      }

      boolean isInChat = mc.currentScreen instanceof ChatScreen;
      boolean isHovering = showOnHover.get() && lastHoveredEntity != null && (now - lastHoverTime < 2000L);
      boolean shown = isInChat || target != null || isHovering;

      // Обновляем prevTarget (как в доноре)
      if (target != null) {
         prevTarget = target;
      } else if (isHovering) {
         prevTarget = lastHoveredEntity;
      } else if (isInChat || prevTarget == null) {
         prevTarget = mc.player;
      }

      // Анимация появления/скрытия (как в доноре: BACKWARDS = показывать)
      animation.update();
      animation.run(shown ? 1.0 : 0.0, 0.4F, Easings.CIRC_OUT);
      float anim = 1.0F - (float) animation.getValue();
      anim = MathHelper.clamp(anim, 0.0F, 1.0F);
      if (anim < 0.01F) return;

      if (prevTarget == null) return;

      // Предметы
      List<ItemStack> items = new ArrayList<>();
      if (showItems.get()) {
         items.add(prevTarget.getMainHandStack());
         items.add(prevTarget.getEquippedStack(EquipmentSlot.HEAD));
         items.add(prevTarget.getEquippedStack(EquipmentSlot.CHEST));
         items.add(prevTarget.getEquippedStack(EquipmentSlot.LEGS));
         items.add(prevTarget.getEquippedStack(EquipmentSlot.FEET));
         items.add(prevTarget.getOffHandStack());
         items.removeIf(ItemStack::isEmpty);
      }

      float totalItemsW = items.size() * ITEM_BOX + Math.max(0, items.size() - 1) * SPACING;
      float itemStartY = 0.0F;
      float mainBoxY = showItems.get() && !items.isEmpty() ? ITEM_BOX + 2.0F : 0.0F;
      float panelH = mainBoxY + MAIN_HEIGHT + 6.0F;

      int fbW = mc.getWindow().getFramebufferWidth();
      int fbH = mc.getWindow().getFramebufferHeight();
      float defX = Math.max(0.0F, (fbW - WIDTH) / 2.0F);
      float defY = Math.max(0.0F, fbH * 0.55F);

      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag("targetHUD", defX, defY, WIDTH, panelH);
      float x = session.positionX();
      float y = session.positionY();
      float hudScale = session.scale();

      float width = WIDTH * hudScale;
      float mainHeight = MAIN_HEIGHT * hudScale;
      float headSize = HEAD_SIZE * hudScale;
      float boxY = y + mainBoxY * hudScale;
      float itemStartYScaled = y + itemStartY * hudScale;
      float totalItemsWScaled = totalItemsW * hudScale;
      float spacingScaled = SPACING * hudScale;
      float itemBoxScaled = ITEM_BOX * hudScale;

      float guiScale = (float) mc.getWindow().getScaleFactor();
      if (guiScale <= 0.0F) guiScale = 1.0F;

      drawContext.getMatrices().pushMatrix();
      try {
         drawContext.getMatrices().translate(x / guiScale, y / guiScale);
         drawContext.getMatrices().scale(hudScale, hudScale);
         drawContext.getMatrices().translate(-x / guiScale, -y / guiScale);

         r2.pushAlpha(anim);
         try {
            // Предметы сверху
            if (showItems.get() && !items.isEmpty()) {
               float itemStartX = x + (width / 2.0F) - (totalItemsWScaled / 2.0F);
               for (int i = 0; i < items.size(); i++) {
                  float ix = itemStartX + i * (itemBoxScaled + spacingScaled);
                  drawStyle(r2, ix, itemStartYScaled, itemBoxScaled, itemBoxScaled, anim);
                  if (drawContext != null) {
                     renderItem(drawContext, items.get(i), ix + 2.0F * hudScale, itemStartYScaled + 2.0F * hudScale, itemBoxScaled - 4.0F * hudScale);
                  }
               }
            }

            // Основная панель
            drawStyle(r2, x, boxY, width, mainHeight, anim);

            // Голова
            float headX = x + 4.0F * hudScale;
            float headY = boxY + 4.0F * hudScale;
            drawHead(r2, drawContext, prevTarget, headX, headY, headSize, anim);

            // Имя со скроллом
            String name = prevTarget instanceof CreeperEntity ? "Грустный крипер" : prevTarget.getName().getString();
            float nameX = headX + headSize + 5.0F * hudScale;
            float nameW = width - (headSize + 15.0F * hudScale);
            float nameSize = 7.5F * hudScale;

            // Текст метрики
            float tWidth = r2.measureText(FontRegistry.INTER_MEDIUM, name, nameSize).width;

            // Скролл анимация (как в доноре)
            if (scrollDelay == 0L) scrollDelay = System.currentTimeMillis();
            if (System.currentTimeMillis() - scrollDelay > 1000L) {
               scrollDelay = System.currentTimeMillis();
               scrollDir = -scrollDir;
            }
            float targetOffset = scrollDir > 0 ? Math.max(0.0F, tWidth - nameW) : 0.0F;
            scrollAnim = AnimationMath.animation(scrollAnim, targetOffset, 0.05F);

            r2.pushClipRect((int) nameX, (int) boxY, (int) nameW, (int) mainHeight);
            r2.text(FontRegistry.INTER_MEDIUM, nameX - scrollAnim, boxY + 4.5F * hudScale, nameSize, name, 0xFFFFFFFF);
            r2.popClipRect();

            // HP-бар
            float barX = nameX;
            float barY = boxY + mainHeight - 8.5F * hudScale;
            float barW = nameW;
            float barH = 3.5F * hudScale;

            // Фон бара
            r2.rect(barX, barY, barW, barH, 1.5F * hudScale, Renderer2D.ColorUtil.replAlpha(0xFF000000, (int) (60 * anim)));

            // HP анимация
            float maxHp = prevTarget.getMaxHealth();
            healthAnim = AnimationMath.animation(healthAnim, prevTarget.getHealth(), 0.1F);
            absorptionAnim = AnimationMath.animation(absorptionAnim, prevTarget.getAbsorptionAmount(), 0.1F);

            float hpFactor = MathHelper.clamp(healthAnim / maxHp, 0.0F, 1.0F);

            // Градиент HP (клиентский цвет)
            int c1 = Renderer2D.ColorUtil.getMainColor(1, 0);
            int c2 = Renderer2D.ColorUtil.getMainColor(1, 40);
            drawHpGradient(r2, barX, barY, barW * hpFactor, barH, c1, c2, anim);

            // Частицы у HP
            if (particles.get() && hpFactor > 0.0F) {
               spawnHealthParticles(barX + barW * hpFactor, barY + barH / 2.0F, c1);
            }

            // Абсорбция (золото, от правого края)
            float absWidth = 0.0F;
            if (absorptionAnim > 0.1F) {
               float absFactor = MathHelper.clamp(absorptionAnim / maxHp, 0.0F, 1.0F);
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

            // HP текст
            float hValue = (float) (Math.round(healthAnim * 10.0F) / 10.0F);
            float aValue = (float) (Math.round(absorptionAnim * 10.0F) / 10.0F);
            String hStr = hValue + "";
            String aStr = aValue > 0 ? " + (" + aValue + ")" : "";
            String suffix = " HP";
            float hpTextSize = 6.0F * hudScale;
            float fullTextW = r2.measureText(FontRegistry.INTER_MEDIUM, hStr + aStr + suffix, hpTextSize).width;
            float drawX = barX + barW - fullTextW;
            float drawY = barY - 7.5F * hudScale;
            r2.text(FontRegistry.INTER_MEDIUM, drawX, drawY, hpTextSize, hStr, 0xFFC8C8C8);
            float off = r2.measureText(FontRegistry.INTER_MEDIUM, hStr, hpTextSize).width;
            if (aValue > 0) {
               r2.text(FontRegistry.INTER_MEDIUM, drawX + off, drawY, hpTextSize, aStr, 0xFFFFD700);
               off += r2.measureText(FontRegistry.INTER_MEDIUM, aStr, hpTextSize).width;
            }
            r2.text(FontRegistry.INTER_MEDIUM, drawX + off, drawY, hpTextSize, suffix, Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, (int) (180 * anim)));
         } finally {
            r2.popAlpha();
         }
      } finally {
         drawContext.getMatrices().popMatrix();
      }
      DraggableManager.getInstance().endDrag(session);
   }

   /** Плоский скин-аватар в логических координатах GUI (без 3D). */
   private static void drawHead(Renderer2D r2, DrawContext drawContext, LivingEntity target, float headX, float headY, float headSize, float anim) {
      if (target == null) return;
      if (drawContext == null) {
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

   /** Градиентный бар (левая часть c1, правая c2). */
   private static void drawHpGradient(Renderer2D r2, float x, float y, float w, float h, int c1, int c2, float anim) {
      if (w <= 0.0F || h <= 0.0F) return;
      float mid = w / 2.0F;
      r2.rect(x, y, mid, h, 1.5F, Renderer2D.ColorUtil.replAlpha(c1, (int) (255 * anim)));
      r2.rect(x + mid, y, w - mid, h, 1.5F, Renderer2D.ColorUtil.replAlpha(c2, (int) (255 * anim)));
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

   /** Спавн частиц у края HP-бара. */
   private static void spawnHealthParticles(float x, float y, int color) {
      if (Math.random() > 0.8) {
         float dir = (float) (Math.random() * 360.0F);
         float roll = (float) (Math.random() - 0.5F) * 5.0F;
         long alive = 500L + (long) (Math.random() * 500L);
         particlesEngine.add(x, y,
               (float) (Math.random() - 0.5F) * 0.5F,
               (float) (Math.random() - 0.2F) * 0.5F,
               dir, roll, alive, color);
      }
   }

   /** Движок частиц (2D). */
   @Environment(EnvType.CLIENT)
   private static final class Particles2DEngine {
      private final List<Particle2D> particles = new ArrayList<>();

      void add(float x, float y, float vx, float vy, float dir, float roll, long alive, int color) {
         particles.add(new Particle2D(x, y, vx, vy, dir, roll, alive, color));
      }

      void render(Renderer2D r2) {
         for (Particle2D p : particles) {
            p.render(r2);
         }
         particles.removeIf(Particle2D::removed);
      }
   }

   /** Частица (dir/roll, движение по sin/cos, затухание). */
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
         r2.circle(x, y, 2.5F, 0.0F, 1.0F, col);
         r2.circle(x, y, 1.5F, 0.0F, 1.0F, Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, (int) (f * 200.0F)));
      }
   }
}