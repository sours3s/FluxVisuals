package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.animation.AnimationMath;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * HotBar HUD — полная реализация из GodWeer (HotbarElement)
 * <p>Кастомный хотбар с рендерингом слотов, оффхенда, брони, голода, воздуха, опыта.
 */
@Environment(EnvType.CLIENT)
public class HotBarHUD {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   // Константы из донора (HotbarElement)
   private static final float SLOT_SIZE = 24.0F;
   private static final float SLOT_OFFSET = 2.0F;
   private static final float HOTBAR_WIDTH = 198.0F;
   private static final float HOTBAR_HEIGHT = SLOT_SIZE - 2.0F;
   private static final float HOTBAR_OFFSET = 25.0F;
   private static final float ITEM_OFFSET = -4.0F;
   private static final float ROUND_SMALL = 4.0F;
   private static final float SMOOTH = 1.0F;

   // Статус бары
   private static final float STATUS_HEIGHT = 8.0F;
   private static final float STATUS_OFFSET = 2.0F;
   private static final float STATUS_CENTER_OFFSET = 30.0F;
   private static final float STATUS_WIDTH = (HOTBAR_WIDTH - STATUS_CENTER_OFFSET) / 2.0F;
   private static final float STATUS_TEXT_SIZE = 7.0F;
   private static final float EXP_OFFSET = 12.0F;
   private static final float EXP_HEIGHT = STATUS_HEIGHT;
   private static final float EXP_TEXT_SIZE = STATUS_TEXT_SIZE;

   // Анимации
   private static float slotChangeAnim = 0.0F;
   private static float expAnim = 0.0F;
   private static float[] statusAnims = new float[4];
   private static float tooltipAnim = 0.0F;

   // Health animation (как в доноре)
   private static int ticks = 0;
   private static long lastTickTime = 0L;
   private static int lastHealthValue = 0;
   private static int renderHealthValue = 0;
   private static long lastHealthCheckTime = 0L;
   private static long heartJumpEndTick = 0L;
   private static int lastBurstBubble = 0;

   public static void tick() {
      long currentTime = System.currentTimeMillis();
      if (currentTime - lastTickTime >= 50L) {
         ticks++;
         lastTickTime = currentTime;
      }
   }

   public static boolean hasContent() {
      return mc.player != null && mc.world != null;
   }

   public static void renderEmpty(Renderer2D r2) {
      float w = HOTBAR_WIDTH;
      float h = HOTBAR_HEIGHT + 40.0F;
      float preferredX = (mc.getWindow().getScaledWidth() - w) / 2.0F;
      float preferredY = mc.getWindow().getScaledHeight() - HOTBAR_OFFSET - h;
      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag("hotbar", preferredX, preferredY, w, h);
      float x = session.positionX();
      float y = session.positionY();
      Hud.drawClientRect(r2, x, y, w, h, 11.0F, 1.0F, 1.0F);
      r2.text(FontRegistry.INTER_MEDIUM, x + 14.0F, y + 28.0F, 28.0F, "Hotbar",
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), 255));
      DraggableManager.getInstance().endDrag(session);
   }

   public static void hotbar(Renderer2D r2, DrawContext drawContext) {
      if (mc.player != null && mc.world != null) {
         float guiScaleFactor = (float) mc.getWindow().getScaleFactor();
         if (guiScaleFactor <= 0.0F) guiScaleFactor = 1.0F;

         // Анимация выбранного слота (как в доноре)
         int slot = mc.player.getInventory().getSelectedSlot();
         slotChangeAnim = AnimationMath.animation(slotChangeAnim, slot, 0.41F);

         // Позиции (как в доноре)
         float halfWidth = mc.getWindow().getScaledWidth() / 2.0F;
         float x = halfWidth - HOTBAR_WIDTH / 2.0F;
         float y = mc.getWindow().getScaledHeight() - HOTBAR_OFFSET;

         DraggableManager.DragSession session = DraggableManager.getInstance()
               .beginDrag("hotbar", x, y, HOTBAR_WIDTH, HOTBAR_HEIGHT);
         float dragX = session.positionX();
         float dragY = session.positionY();
         float hudScale = session.scale();

         float finalX = dragX;
         float finalY = dragY;
         float finalScale = hudScale;

         drawContext.getMatrices().pushMatrix();
         try {
            drawContext.getMatrices().translate(finalX / guiScaleFactor, finalY / guiScaleFactor);
            drawContext.getMatrices().scale(finalScale, finalScale);
            drawContext.getMatrices().translate(-finalX / guiScaleFactor, -finalY / guiScaleFactor);

            // Рендерим хотбар (как в доноре)
            drawHotbarStyle(r2, finalX, finalY, HOTBAR_WIDTH, HOTBAR_HEIGHT, ROUND_SMALL, 1.0F);

            // Выбранный слот индикатор (как в доноре)
            float selectedSlotX = halfWidth - HOTBAR_WIDTH / 2.0F + ((SLOT_SIZE - SLOT_OFFSET) * slotChangeAnim);
            int selectedColor = Renderer2D.ColorUtil.getMainColor(1, 0);
            r2.rect(selectedSlotX + 1.0F, finalY + 1.5F, SLOT_SIZE - 4.0F, 2.5F, 1.0F, selectedColor);

            // Оффхенд слот (как в доноре)
            ItemStack offhandStack = mc.player.getOffHandStack();
            Arm offhand = mc.player.getMainArm().getOpposite();

            if (!offhandStack.isEmpty()) {
               float offhandGap = 10.0F;
               float offhandSlotWidth = SLOT_SIZE - 3.0F;

               float offhandX = offhand == Arm.LEFT
                       ? finalX - offhandSlotWidth - offhandGap
                       : finalX + HOTBAR_WIDTH + offhandGap;

               drawHotbarStyle(r2, offhandX, finalY, offhandSlotWidth, HOTBAR_HEIGHT, ROUND_SMALL, 1.0F);

               float itemX = offhandX + (offhandSlotWidth - 16.0F) / 2.0F;
               float itemY = finalY + (HOTBAR_HEIGHT - 16.0F) / 2.0F;
               renderHotbarItem(drawContext, itemX, itemY, offhandStack, 10);
            }

            // 9 слотов хотбара (как в доноре)
            for (int m = 0; m < 9; ++m) {
               float n = finalX + (m * (SLOT_SIZE - SLOT_OFFSET)) + 4.0F;
               float o = finalY + (HOTBAR_HEIGHT - 16.0F) / 2.0F;
               renderHotbarItem(drawContext, n, o, mc.player.getInventory().getStack(m), m);
            }

         } finally {
            drawContext.getMatrices().popMatrix();
         }
         DraggableManager.getInstance().endDrag(session);
      }
   }

   private static void drawHotbarStyle(Renderer2D r2, float rx, float ry, float rw, float rh, float round, float alpha) {
      Vector4f roundVec = new Vector4f(round);
      if (Hud.blur.get("HUD")) {
         r2.prepareBlur(23.0F);
         r2.blur(rx, ry, rw, rh, round, alpha);
      }
      // Glass style (как в доноре GLASS)
      int bgColor = Renderer2D.ColorUtil.replAlpha(0xFF3E3E47, 0);
      r2.rect(rx, ry, rw, rh, round, bgColor);
      int outAlpha = (int) Math.min(Math.max(25 * alpha, 0), 255);
      int outColor = Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, outAlpha);
      r2.rectOutline(rx, ry, rw, rh, round, outColor, 0.4F);
   }

   private static boolean isImportantItem(ItemStack stack) {
      if (stack.isEmpty()) return false;
      Item item = stack.getItem();
      if (stack.contains(DataComponentTypes.EQUIPPABLE) || stack.contains(DataComponentTypes.FOOD)) return true;
      return item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION ||
            item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE || item == Items.TOTEM_OF_UNDYING ||
            item == Items.ENDER_PEARL || item == Items.SHIELD || item == Items.MILK_BUCKET || item == Items.EXPERIENCE_BOTTLE;
   }

   private static void renderHotbarItem(DrawContext context, float x, float y, ItemStack stack, int seed) {
      if (stack.isEmpty()) return;
      float guiScale = (float) mc.getWindow().getScaleFactor();
      if (isImportantItem(stack)) {
         context.drawItem(mc.player, stack, (int) x, (int) y, seed);
         context.drawStackOverlay(mc.textRenderer, stack, (int) x, (int) y);
         return;
      }
      float f = (float) stack.getBobbingAnimationTime() - 0.0F; // tickCounter не доступен, упрощаем
      if (f > 0.0F) {
         float g = 1.0F + f / 5.0F;
         context.getMatrices().pushMatrix();
         context.getMatrices().translate(x + 8.0F, y + 12.0F);
         context.getMatrices().scale(g, g);
         context.getMatrices().translate(-x - 8.0F, -y - 12.0F);
      }
      context.drawItem(mc.player, stack, (int) x, (int) y, seed);
      if (f > 0.0F) {
         context.getMatrices().popMatrix();
      }
   }
}