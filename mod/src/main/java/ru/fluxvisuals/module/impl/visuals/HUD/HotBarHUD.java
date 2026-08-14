package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Arm;
import net.minecraft.util.Util;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.random.Random;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public class HotBarHUD {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Random random = Random.create();
   private static int ticks = 0;
   private static long lastTickTime = 0L;
   private static int lastHealthValue = 0;
   private static int renderHealthValue = 0;
   private static long lastHealthCheckTime = 0L;
   private static long heartJumpEndTick = 0L;
   private static int lastBurstBubble = 0;
   private static final Identifier ARMOR_EMPTY_TEXTURE = Identifier.ofVanilla("hud/armor_empty");
   private static final Identifier ARMOR_HALF_TEXTURE = Identifier.ofVanilla("hud/armor_half");
   private static final Identifier ARMOR_FULL_TEXTURE = Identifier.ofVanilla("hud/armor_full");
   private static final Identifier FOOD_EMPTY_TEXTURE = Identifier.ofVanilla("hud/food_empty");
   private static final Identifier FOOD_HALF_TEXTURE = Identifier.ofVanilla("hud/food_half");
   private static final Identifier FOOD_FULL_TEXTURE = Identifier.ofVanilla("hud/food_full");
   private static final Identifier FOOD_EMPTY_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_empty_hunger");
   private static final Identifier FOOD_HALF_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_half_hunger");
   private static final Identifier FOOD_FULL_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_full_hunger");
   private static final Identifier AIR_TEXTURE = Identifier.ofVanilla("hud/air");
   private static final Identifier AIR_BURSTING_TEXTURE = Identifier.ofVanilla("hud/air_bursting");
   private static final Identifier AIR_EMPTY_TEXTURE = Identifier.ofVanilla("hud/air_empty");
   private static final List<HotBarHUD.PendingItemRender> pendingItems = new ArrayList<>();

   public static void tick() {
      long currentTime = System.currentTimeMillis();
      if (currentTime - lastTickTime >= 50L) {
         ticks++;
         lastTickTime = currentTime;
      }
   }

   /** Always has content in-game (health, armor, food, etc.), but in chat we still show empty placeholder for positioning. */
   public static boolean hasContent() {
      return mc.player != null && mc.world != null;
   }

   /** Empty placeholder for chat screen positioning - shows "Hotbar" header like other HUD elements. */
   public static void renderEmpty(Renderer2D r2) {
      boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
      float w = 200.0F;
      float h = 50.0F;
      float preferredX = (mc.getWindow().getWidth() - w) / 2.0F;
      float preferredY = mc.getWindow().getHeight() - 80.0F;
      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag("hotbar", preferredX, preferredY, w, h);
      float x = session.positionX();
      float y = session.positionY();
      Hud.drawClientRect(r2, x, y, w, h, 11.0F, 1.0F, 1.0F);
      r2.text(ru.fluxvisuals.util.render.text.FontRegistry.INTER_MEDIUM, x + 14.0F, y + 28.0F, 28.0F, "Hotbar",
            ru.fluxvisuals.util.color.ColorUtil.replAlpha(ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.getTextColor(1, 1), 1.0F));
      DraggableManager.getInstance().endDrag(session);
   }

   public static void hotbar(Renderer2D r2, DrawContext drawContext) {
      if (mc.player != null && mc.world != null) {
         float guiScale = (float)mc.getWindow().getScaleFactor();
         if (guiScale <= 0.0F) {
            guiScale = 1.0F;
         }

         // Width must accommodate the vanilla 182-px-wide health/food row in gui-scale
         // pixels (= 182 * guiScale framebuffer pixels) plus padding, otherwise the
         // hearts and food icons spill out of the pill on higher GUI scales.
         float minWidth = 182.0F * guiScale + 24.0F * guiScale;
         float hotbarWidth = Math.max(332.0F, minWidth);
         float hotbarHeight = 36.0F;
         float screenWidth = mc.getWindow().getWidth();
         float screenHeight = mc.getWindow().getHeight();
         float preferredX = (screenWidth - hotbarWidth) / 2.0F;
         // Match vanilla hotbar position: (guiHeight - 22) * guiScale in framebuffer pixels
         // guiHeight = screenHeight / guiScale
         // vanilla Y = (screenHeight / guiScale - 22) * guiScale = screenHeight - 22 * guiScale
         float preferredY = screenHeight - 22.0F * guiScale;
         DraggableManager.DragSession session = DraggableManager.getInstance()
               .beginDrag("hotbar", preferredX, preferredY, hotbarWidth, hotbarHeight);
         float x = session.positionX();
         float y = session.positionY();
         float hudScale = session.scale();

         try {
            Hud.drawClientRect(r2, x, y, hotbarWidth, hotbarHeight, 11.0F, 1.0F, 1.0F);
            int slots = 9;
            float cellWidth = hotbarWidth / slots;
            float normalSlotSize = 24.0F;
            float selectedSlotSize = 30.0F;
            int selectedSlot = mc.player.getInventory().getSelectedSlot();

            for (int i = 0; i < slots; i++) {
               float cellLeft = x + i * cellWidth;
               float slotCenterX = cellLeft + cellWidth / 2.0F;
               boolean isSelected = i == selectedSlot;
               float currentSlotSize = isSelected ? selectedSlotSize : normalSlotSize;
               float slotX = slotCenterX - currentSlotSize / 2.0F;
               float slotY = y + (hotbarHeight - currentSlotSize) / 2.0F;
               if (isSelected) {
                  // Выделение выбранного слота: яркая обводка + заполнение основным цветом
                  r2.rectOutline(
                     slotX, slotY, currentSlotSize, currentSlotSize, 11.0F, 2, Renderer2D.ColorUtil.getMainColor(1, 1)
                  );
                  r2.rect(
                     slotX, slotY, currentSlotSize, currentSlotSize, 11.0F, Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 40)
                  );
               }

               ItemStack stack = mc.player.getInventory().getStack(i);
               if (!stack.isEmpty()) {
                  float itemRenderSize = 16.0F;
                  float itemX = (slotCenterX - itemRenderSize / 2.0F) / guiScale;
                  float itemY = (y + (hotbarHeight - itemRenderSize) / 2.0F) / guiScale;
                  pendingItems.add(new HotBarHUD.PendingItemRender(stack, itemX, itemY, i, 1.0F));
               }
            }

            renderOffhandSlot(r2, drawContext, x, y, hotbarWidth, hotbarHeight, selectedSlotSize, guiScale);

            // Apply the user HUD scale to every DrawContext-driven element (items, hearts,
            // armor, food, air, experience level) by transforming the matrix around the
            // HUD top-left, mirroring what r2.pushScale already does for renderer2D draws.
            drawContext.getMatrices().pushMatrix();
            try {
               drawContext.getMatrices().translate(x / guiScale, y / guiScale);
               drawContext.getMatrices().scale(hudScale, hudScale);
               drawContext.getMatrices().translate(-x / guiScale, -y / guiScale);

               float scaledHotbarX = x / guiScale;
               float scaledHotbarY = y / guiScale;
               float scaledHotbarWidth = hotbarWidth / guiScale;
               renderExperienceLevel(drawContext, scaledHotbarY);
               if (mc.interactionManager != null && mc.interactionManager.hasStatusBars()) {
                  renderStatusBars(drawContext, scaledHotbarX, scaledHotbarY, scaledHotbarWidth);
               }
               renderPendingItems(drawContext);
            } finally {
               drawContext.getMatrices().popMatrix();
            }
         } finally {
            DraggableManager.getInstance().endDrag(session);
         }
      }
   }

   private static void renderExperienceLevel(DrawContext drawContext, float hotbarY) {
      if (mc.player != null) {
         int experienceLevel = mc.player.experienceLevel;
         if (experienceLevel > 0) {
            drawContext.getMatrices().pushMatrix();
            int vanillaY = drawContext.getScaledWindowHeight() - 31;
            int targetY = Math.round(hotbarY - 13.0F);
            drawContext.getMatrices().translate(0.0F, (float)(targetY - vanillaY));
            Bar.drawExperienceLevel(drawContext, mc.textRenderer, experienceLevel);
            drawContext.getMatrices().popMatrix();
         }
      }
   }

   private static void renderStatusBars(DrawContext context, float hotbarX, float hotbarY, float hotbarWidth) {
      PlayerEntity player = mc.player;
      if (player != null) {
         int i = MathHelper.ceil(player.getHealth());
         boolean bl = heartJumpEndTick > ticks && (heartJumpEndTick - ticks) / 3L % 2L == 1L;
         long l = Util.getMeasuringTimeMs();
         if (i < lastHealthValue && player.timeUntilRegen > 0) {
            lastHealthCheckTime = l;
            heartJumpEndTick = ticks + 20;
         } else if (i > lastHealthValue && player.timeUntilRegen > 0) {
            lastHealthCheckTime = l;
            heartJumpEndTick = ticks + 10;
         }

         if (l - lastHealthCheckTime > 1000L) {
            renderHealthValue = i;
            lastHealthCheckTime = l;
         }

         lastHealthValue = i;
         int j = renderHealthValue;
         random.setSeed(ticks * 312871L);
         int k = Math.round(hotbarX + (hotbarWidth - 182.0F) / 2.0F);
         int m = k + 182;
         int n = Math.round(hotbarY - 14.0F);
         float f = Math.max((float)player.getAttributeValue(EntityAttributes.MAX_HEALTH), (float)Math.max(j, i));
         int o = MathHelper.ceil(player.getAbsorptionAmount());
         int p = MathHelper.ceil((f + o) / 2.0F / 10.0F);
         int q = Math.max(10 - (p - 2), 3);
         int r = n - 10;
         int s = -1;
         if (player.hasStatusEffect(StatusEffects.REGENERATION)) {
            s = ticks % MathHelper.ceil(f + 5.0F);
         }

         renderArmor(context, player, n, p, q + 2, k - 22);
         renderHealthBar(context, player, k - 22, n - 2, q, s, f, i, j, o, bl);
         renderFood(context, player, n - 2, m + 22);
         renderAirBubbles(context, player, 0, r - 2, m + 22);
      }
   }

   private static void renderArmor(DrawContext context, PlayerEntity player, int i, int j, int k, int x) {
      int l = player.getArmor();
      if (l > 0) {
         int m = i - (j - 1) * k - 10;

         for (int n = 0; n < 10; n++) {
            int o = x + n * 8;
            if (n * 2 + 1 < l) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ARMOR_FULL_TEXTURE, o, m, 9, 9);
            }

            if (n * 2 + 1 == l) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ARMOR_HALF_TEXTURE, o, m, 9, 9);
            }

            if (n * 2 + 1 > l) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ARMOR_EMPTY_TEXTURE, o, m, 9, 9);
            }
         }
      }
   }

   private static void renderHealthBar(
      DrawContext context,
      PlayerEntity player,
      int x,
      int y,
      int lines,
      int regeneratingHeartIndex,
      float maxHealth,
      int lastHealth,
      int health,
      int absorption,
      boolean blinking
   ) {
      HotBarHUD.HeartType heartType = HotBarHUD.HeartType.fromPlayerState(player);
      boolean hardcore = MinecraftClient.getInstance().world.getLevelProperties().isHardcore();
      int i = MathHelper.ceil(maxHealth / 2.0);
      int j = MathHelper.ceil(absorption / 2.0);
      int k = i * 2;

      for (int l = i + j - 1; l >= 0; l--) {
         int m = l / 10;
         int n = l % 10;
         int o = x + n * 8;
         int p = y - m * lines;
         if (lastHealth + absorption <= 4) {
            p += random.nextInt(2);
         }

         if (l < i && l == regeneratingHeartIndex) {
            p -= 2;
         }

         drawHeart(context, HotBarHUD.HeartType.CONTAINER, o, p, hardcore, blinking, false);
         int q = l * 2;
         boolean bl2 = l >= i;
         if (bl2) {
            int r = q - k;
            if (r < absorption) {
               boolean bl3 = r + 1 == absorption;
               drawHeart(context, heartType == HotBarHUD.HeartType.WITHERED ? heartType : HotBarHUD.HeartType.ABSORBING, o, p, hardcore, false, bl3);
            }
         }

         if (blinking && q < health) {
            boolean bl4 = q + 1 == health;
            drawHeart(context, heartType, o, p, hardcore, true, bl4);
         }

         if (q < lastHealth) {
            boolean bl4 = q + 1 == lastHealth;
            drawHeart(context, heartType, o, p, hardcore, false, bl4);
         }
      }
   }

   private static void drawHeart(DrawContext context, HotBarHUD.HeartType type, int x, int y, boolean hardcore, boolean blinking, boolean half) {
      context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, type.getTexture(hardcore, half, blinking), x, y, 9, 9);
   }

   private static void renderFood(DrawContext context, PlayerEntity player, int top, int right) {
      int i = player.getHungerManager().getFoodLevel();

      for (int j = 0; j < 10; j++) {
         int k = top;
         Identifier emptyTexture;
         Identifier halfTexture;
         Identifier fullTexture;
         if (player.hasStatusEffect(StatusEffects.HUNGER)) {
            emptyTexture = FOOD_EMPTY_HUNGER_TEXTURE;
            halfTexture = FOOD_HALF_HUNGER_TEXTURE;
            fullTexture = FOOD_FULL_HUNGER_TEXTURE;
         } else {
            emptyTexture = FOOD_EMPTY_TEXTURE;
            halfTexture = FOOD_HALF_TEXTURE;
            fullTexture = FOOD_FULL_TEXTURE;
         }

         if (player.getHungerManager().getSaturationLevel() <= 0.0F && ticks % (i * 3 + 1) == 0) {
            k = top + (random.nextInt(3) - 1);
         }

         int l = right - j * 8 - 9;
         context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, emptyTexture, l, k, 9, 9);
         if (j * 2 + 1 < i) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, fullTexture, l, k, 9, 9);
         }

         if (j * 2 + 1 == i) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, halfTexture, l, k, 9, 9);
         }
      }
   }

   private static void renderAirBubbles(DrawContext context, PlayerEntity player, int heartCount, int top, int left) {
      int maxAir = player.getMaxAir();
      int air = (int) Math.clamp((long)player.getAir(), 0L, (long)maxAir);
      boolean submergedInWater = player.isSubmergedIn(FluidTags.WATER);
      if (submergedInWater || air < maxAir) {
         int k = getAirBubbles(air, maxAir, -2);
         int l = getAirBubbles(air, maxAir, 0);
         int m = 10 - getAirBubbles(air, maxAir, getAirBubbleDelay(air, submergedInWater));
         boolean bl2 = k != l;
         if (!submergedInWater) {
            lastBurstBubble = 0;
         }

         for (int n = 1; n <= 10; n++) {
            int o = left - (n - 1) * 8 - 9;
            if (n <= k) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, AIR_TEXTURE, o, top, 9, 9);
            } else if (bl2 && n == l && submergedInWater) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, AIR_BURSTING_TEXTURE, o, top, 9, 9);
            } else if (n > 10 - m) {
               int p = m == 10 && ticks % 2 == 0 ? random.nextInt(2) : 0;
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, AIR_EMPTY_TEXTURE, o, top + p, 9, 9);
            }
         }
      }
   }

   private static int getAirBubbles(int air, int maxAir, int delay) {
      return MathHelper.ceil((float)((air + delay) * 10) / maxAir);
   }

   private static int getAirBubbleDelay(int air, boolean submergedInWater) {
      return air != 0 && submergedInWater ? 1 : 0;
   }

   private static void renderOffhandSlot(
      Renderer2D r2, DrawContext drawContext, float hotbarX, float hotbarY, float hotbarWidth, float hotbarHeight, float slotSize, float guiScale
   ) {
      if (mc.player != null) {
         ItemStack offhandStack = mc.player.getOffHandStack();
         if (!offhandStack.isEmpty()) {
            Arm mainArm = (Arm)mc.options.getMainArm().getValue();
            boolean isRightHanded = mainArm == Arm.RIGHT;
            float offhandGap = 8.0F;
            float offhandSlotX;
            if (isRightHanded) {
               offhandSlotX = hotbarX - slotSize - offhandGap;
            } else {
               offhandSlotX = hotbarX + hotbarWidth + offhandGap;
            }

            Hud.drawClientRect(r2, offhandSlotX, hotbarY, slotSize, slotSize, 11.0F, 1.0F, 1.0F);
            float itemRenderSize = 16.0F;
            float itemX = (offhandSlotX + (slotSize - itemRenderSize) / 2.0F) / guiScale;
            float itemY = (hotbarY + (slotSize - itemRenderSize) / 2.0F) / guiScale;
            pendingItems.add(new HotBarHUD.PendingItemRender(offhandStack, itemX, itemY, 999, 1.0F));
         }
      }
   }

   public static void renderPendingItems(DrawContext drawContext) {
      if (!pendingItems.isEmpty()) {
         for (HotBarHUD.PendingItemRender item : pendingItems) {
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(item.x, item.y);
            drawContext.getMatrices().scale(item.scale, item.scale);
            drawContext.drawItem(item.stack, 0, 0, item.seed);
            drawContext.drawStackOverlay(mc.textRenderer, item.stack, 0, 0);
            drawContext.getMatrices().popMatrix();
         }

         pendingItems.clear();
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum HeartType {
      CONTAINER(
         Identifier.ofVanilla("hud/heart/container"),
         Identifier.ofVanilla("hud/heart/container_blinking"),
         Identifier.ofVanilla("hud/heart/container"),
         Identifier.ofVanilla("hud/heart/container_blinking"),
         Identifier.ofVanilla("hud/heart/container_hardcore"),
         Identifier.ofVanilla("hud/heart/container_hardcore_blinking"),
         Identifier.ofVanilla("hud/heart/container_hardcore"),
         Identifier.ofVanilla("hud/heart/container_hardcore_blinking")
      ),
      NORMAL(
         Identifier.ofVanilla("hud/heart/full"),
         Identifier.ofVanilla("hud/heart/full_blinking"),
         Identifier.ofVanilla("hud/heart/half"),
         Identifier.ofVanilla("hud/heart/half_blinking"),
         Identifier.ofVanilla("hud/heart/hardcore_full"),
         Identifier.ofVanilla("hud/heart/hardcore_full_blinking"),
         Identifier.ofVanilla("hud/heart/hardcore_half"),
         Identifier.ofVanilla("hud/heart/hardcore_half_blinking")
      ),
      POISONED(
         Identifier.ofVanilla("hud/heart/poisoned_full"),
         Identifier.ofVanilla("hud/heart/poisoned_full_blinking"),
         Identifier.ofVanilla("hud/heart/poisoned_half"),
         Identifier.ofVanilla("hud/heart/poisoned_half_blinking"),
         Identifier.ofVanilla("hud/heart/poisoned_hardcore_full"),
         Identifier.ofVanilla("hud/heart/poisoned_hardcore_full_blinking"),
         Identifier.ofVanilla("hud/heart/poisoned_hardcore_half"),
         Identifier.ofVanilla("hud/heart/poisoned_hardcore_half_blinking")
      ),
      WITHERED(
         Identifier.ofVanilla("hud/heart/withered_full"),
         Identifier.ofVanilla("hud/heart/withered_full_blinking"),
         Identifier.ofVanilla("hud/heart/withered_half"),
         Identifier.ofVanilla("hud/heart/withered_half_blinking"),
         Identifier.ofVanilla("hud/heart/withered_hardcore_full"),
         Identifier.ofVanilla("hud/heart/withered_hardcore_full_blinking"),
         Identifier.ofVanilla("hud/heart/withered_hardcore_half"),
         Identifier.ofVanilla("hud/heart/withered_hardcore_half_blinking")
      ),
      ABSORBING(
         Identifier.ofVanilla("hud/heart/absorbing_full"),
         Identifier.ofVanilla("hud/heart/absorbing_full_blinking"),
         Identifier.ofVanilla("hud/heart/absorbing_half"),
         Identifier.ofVanilla("hud/heart/absorbing_half_blinking"),
         Identifier.ofVanilla("hud/heart/absorbing_hardcore_full"),
         Identifier.ofVanilla("hud/heart/absorbing_hardcore_full_blinking"),
         Identifier.ofVanilla("hud/heart/absorbing_hardcore_half"),
         Identifier.ofVanilla("hud/heart/absorbing_hardcore_half_blinking")
      ),
      FROZEN(
         Identifier.ofVanilla("hud/heart/frozen_full"),
         Identifier.ofVanilla("hud/heart/frozen_full_blinking"),
         Identifier.ofVanilla("hud/heart/frozen_half"),
         Identifier.ofVanilla("hud/heart/frozen_half_blinking"),
         Identifier.ofVanilla("hud/heart/frozen_hardcore_full"),
         Identifier.ofVanilla("hud/heart/frozen_hardcore_full_blinking"),
         Identifier.ofVanilla("hud/heart/frozen_hardcore_half"),
         Identifier.ofVanilla("hud/heart/frozen_hardcore_half_blinking")
      );

      private final Identifier fullTexture;
      private final Identifier fullBlinkingTexture;
      private final Identifier halfTexture;
      private final Identifier halfBlinkingTexture;
      private final Identifier hardcoreFullTexture;
      private final Identifier hardcoreFullBlinkingTexture;
      private final Identifier hardcoreHalfTexture;
      private final Identifier hardcoreHalfBlinkingTexture;

      private HeartType(
         Identifier fullTexture,
         Identifier fullBlinkingTexture,
         Identifier halfTexture,
         Identifier halfBlinkingTexture,
         Identifier hardcoreFullTexture,
         Identifier hardcoreFullBlinkingTexture,
         Identifier hardcoreHalfTexture,
         Identifier hardcoreHalfBlinkingTexture
      ) {
         this.fullTexture = fullTexture;
         this.fullBlinkingTexture = fullBlinkingTexture;
         this.halfTexture = halfTexture;
         this.halfBlinkingTexture = halfBlinkingTexture;
         this.hardcoreFullTexture = hardcoreFullTexture;
         this.hardcoreFullBlinkingTexture = hardcoreFullBlinkingTexture;
         this.hardcoreHalfTexture = hardcoreHalfTexture;
         this.hardcoreHalfBlinkingTexture = hardcoreHalfBlinkingTexture;
      }

      Identifier getTexture(boolean hardcore, boolean half, boolean blinking) {
         if (!hardcore) {
            if (half) {
               return blinking ? this.halfBlinkingTexture : this.halfTexture;
            } else {
               return blinking ? this.fullBlinkingTexture : this.fullTexture;
            }
         } else if (half) {
            return blinking ? this.hardcoreHalfBlinkingTexture : this.hardcoreHalfTexture;
         } else {
            return blinking ? this.hardcoreFullBlinkingTexture : this.hardcoreFullTexture;
         }
      }

      static HotBarHUD.HeartType fromPlayerState(PlayerEntity player) {
         if (player.hasStatusEffect(StatusEffects.POISON)) {
            return POISONED;
         } else if (player.hasStatusEffect(StatusEffects.WITHER)) {
            return WITHERED;
         } else {
            return player.isFrozen() ? FROZEN : NORMAL;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static class PendingItemRender {
      ItemStack stack;
      float x;
      float y;
      int seed;
      float scale;

      PendingItemRender(ItemStack stack, float x, float y, int seed, float scale) {
         this.stack = stack;
         this.x = x;
         this.y = y;
         this.seed = seed;
         this.scale = scale;
      }
   }
}
