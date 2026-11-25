package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

/**
 * Плавные анимации ванильного интерфейса: чат, список игроков, инвентарь,
 * хотбар и насыщение. Состояние анимаций живёт в статических полях, потому что
 * читается из миксинов, не имеющих ссылки на экземпляр модуля.
 */
@IModule(
   name = "Better Minecraft",
   description = "Анимации чата, таба, инвентаря, хотбара и отображение насыщения",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class BetterMinecraft extends Module {
   private static final Identifier SATURATION_FULL_SPRITE = Identifier.ofVanilla("hud/food_full");
   private static final Identifier SATURATION_HALF_SPRITE = Identifier.ofVanilla("hud/food_half");

   public static final BooleanSetting inventoryAnim = new BooleanSetting("Inventory Anim", true);
   public static final BooleanSetting tabAnim = new BooleanSetting("Tab Anim", true);
   public static final BooleanSetting chatAnim = new BooleanSetting("Chat Anim", true);
   public static final BooleanSetting chatCharAnim = new BooleanSetting("Chat Letter Anim", true);
   public static final BooleanSetting itemMoveAnim = new BooleanSetting("Item Move Anim", true);
   public static final BooleanSetting hotbarAnim = new BooleanSetting("Hotbar Anim", true);
   public static final BooleanSetting hotbarChatLift = new BooleanSetting("Hotbar Chat Lift", true);
   public static final BooleanSetting saturationDisplay = new BooleanSetting("Saturation Display", true);
   public static final BooleanSetting hideMessageIndicator = new BooleanSetting("Hide Message Indicator", false);
   public static final SliderSetting animSpeed = new SliderSetting("Anim Speed", 300.0F, 80.0F, 800.0F, 10.0F, false);

   private static final Smoothed hotbarSelection = new Smoothed(180L);
   private static final Smoothed chatLift = new Smoothed(260L);

   private static long inventoryOpenNanos;
   private static long chatMessageNanos;
   private static long chatOpenNanos;
   private static long hotbarSelectionFrameNanos;
   private static long tabAnimStartNanos;
   private static boolean chatScreenWasOpen;
   private static boolean tabVisible;
   private static float tabFrom;
   private static float tabTo;

   public BetterMinecraft() {
      this.addSettings(new Setting[]{
         inventoryAnim, tabAnim, chatAnim, chatCharAnim, itemMoveAnim,
         hotbarAnim, hotbarChatLift, saturationDisplay, hideMessageIndicator, animSpeed
      });
   }

   @Override
   public void onEnable() {
      super.onEnable();
      reset();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      reset();
   }

   private static void reset() {
      inventoryOpenNanos = 0L;
      chatMessageNanos = 0L;
      chatOpenNanos = 0L;
      chatScreenWasOpen = false;
      tabVisible = false;
      tabFrom = 0.0F;
      tabTo = 0.0F;
      hotbarSelection.reset();
      chatLift.reset();
   }

   private static BetterMinecraft get() {
      if (FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
         return null;
      }
      return FluxVisualsClient.get.manager.get(BetterMinecraft.class);
   }

   private static boolean active(BooleanSetting setting) {
      BetterMinecraft mod = get();
      return mod != null && mod.enable && setting.get();
   }

   public static boolean chatAnimationsEnabled() {
      return active(chatAnim);
   }

   public static boolean inventoryAnimationEnabled() {
      return active(inventoryAnim);
   }

   public static boolean itemMoveAnimationEnabled() {
      return active(itemMoveAnim);
   }

   public static boolean tabAnimationEnabled() {
      return active(tabAnim);
   }

   public static boolean hotbarAnimationEnabled() {
      return active(hotbarAnim);
   }

   public static boolean hotbarChatLiftEnabled() {
      return active(hotbarChatLift);
   }

   public static boolean hideMessageIndicatorEnabled() {
      return active(hideMessageIndicator);
   }

   private static float durationMs() {
      return Math.max(1.0F, animSpeed.get());
   }

   private static float clamp01(float value) {
      return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
   }

   private static float easeOutCubic(float p) {
      float t = 1.0F - p;
      return 1.0F - t * t * t;
   }

   private static float easeOutBack(float p) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float t = p - 1.0F;
      return 1.0F + c3 * t * t * t + c1 * t * t;
   }

   private static float easeInBack(float p) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return c3 * p * p * p - c1 * p * p;
   }

   /** Прогресс открытия контейнера: 0 — только открылся, 1 — на месте. */
   public static float inventoryProgress() {
      if (!inventoryAnimationEnabled()) {
         return 1.0F;
      }
      if (inventoryOpenNanos == 0L) {
         return 0.0F;
      }
      float elapsed = (System.nanoTime() - inventoryOpenNanos) / 1_000_000.0F;
      return easeOutCubic(clamp01(elapsed / durationMs()));
   }

   /** Сдвиг модели игрока в инвентаре: снизу вверх при открытии. */
   public static float inventorySlideOffset() {
      if (!inventoryAnimationEnabled()) {
         return 0.0F;
      }
      if (inventoryOpenNanos == 0L) {
         markInventoryOpen();
      }
      float progress = inventoryProgress();
      return -50.0F * (1.0F - easeOutBack(progress));
   }

   public static void markInventoryOpen() {
      inventoryOpenNanos = System.nanoTime();
   }

   /** Разброс по слотам, чтобы предметы «выезжали» не одновременно. */
   public static float itemSlotOffset(int slotId) {
      if (!itemMoveAnimationEnabled() || !(MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?>)) {
         return 0.0F;
      }
      float progress = inventoryProgress();
      if (progress >= 1.0F) {
         return 0.0F;
      }
      float delay = (Math.abs(slotId) % 6) * 0.07F;
      float local = clamp01((progress - delay) / Math.max(0.001F, 1.0F - delay));
      return (1.0F - easeOutBack(local)) * 12.0F;
   }

   /** Плавное перемещение рамки выбранной ячейки хотбара. */
   public static int animateHotbarSelectionX(int x) {
      long now = System.nanoTime();
      long frameDelta = now - hotbarSelectionFrameNanos;
      hotbarSelectionFrameNanos = now;
      if (!hotbarAnimationEnabled() || frameDelta <= 0L || frameDelta > 250_000_000L) {
         hotbarSelection.snap(x);
         return x;
      }
      return Math.round(hotbarSelection.update(x));
   }

   /** Подъём хотбара, когда открыт чат. */
   public static float chatHotbarLiftOffset() {
      boolean chatOpen = MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
      return chatLift.update(hotbarChatLiftEnabled() && chatOpen ? 14.0F : 0.0F);
   }

   public static void onTabVisibility(boolean visible) {
      if (visible == tabVisible) {
         return;
      }
      float current = tabScaleRaw();
      tabVisible = visible;
      tabFrom = current;
      tabTo = visible ? 1.0F : 0.0F;
      tabAnimStartNanos = System.nanoTime();
   }

   private static float tabScaleRaw() {
      if (!tabAnimationEnabled()) {
         return 1.0F;
      }
      long elapsed = (System.nanoTime() - tabAnimStartNanos) / 1_000_000L;
      float p = clamp01((float) elapsed / durationMs());
      float eased = tabTo >= tabFrom ? easeOutBack(p) : easeInBack(p);
      return tabFrom + (tabTo - tabFrom) * eased;
   }

   public static float tabScale() {
      return Math.max(0.01F, Math.min(1.15F, tabScaleRaw()));
   }

   public static boolean shouldRenderClosingTab() {
      return tabAnimationEnabled() && !tabVisible && tabScaleRaw() > 0.01F;
   }

   public static void onChatMessage() {
      if (chatAnimationsEnabled()) {
         chatMessageNanos = System.nanoTime();
      }
   }

   /** Сдвиг всего чата вниз на момент появления сообщения. */
   public static float chatDisplacement(int lineHeight, boolean scrolled) {
      if (!chatAnimationsEnabled() || chatMessageNanos == 0L || scrolled) {
         return 0.0F;
      }
      float elapsed = (System.nanoTime() - chatMessageNanos) / 1_000_000.0F;
      float alpha = clamp01(elapsed / durationMs());
      return lineHeight * 0.8F * (1.0F - alpha);
   }

   public static void onChatScreenClosed() {
      chatScreenWasOpen = false;
   }

   /** Сдвиг панели ввода чата при открытии. */
   public static float chatFieldDisplacement() {
      if (!chatAnimationsEnabled()) {
         return 0.0F;
      }
      MinecraftClient client = MinecraftClient.getInstance();
      long now = System.nanoTime();
      if (client.player != null && !chatScreenWasOpen && !client.player.isSleeping()) {
         chatScreenWasOpen = true;
         chatOpenNanos = now;
      }
      if (chatOpenNanos == 0L) {
         return 0.0F;
      }
      float fade = durationMs();
      float age = Math.min((now - chatOpenNanos) / 1_000_000.0F, fade);
      float alpha = 1.0F - age / fade;
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float eased = c3 * alpha * alpha * alpha - c1 * alpha * alpha;
      float screenFactor = client.getWindow() == null
         ? 1.0F
         : Math.max(0.5F, client.getWindow().getFramebufferHeight() / 1080.0F);
      return eased * 8.0F * screenFactor;
   }

   /** Иконки насыщения над ванильной шкалой еды. */
   public static void renderSaturation(DrawContext context, PlayerEntity player, int top, int right) {
      BetterMinecraft mod = get();
      if (mod == null || !mod.enable || !saturationDisplay.get() || player == null) {
         return;
      }
      float saturation = player.getHungerManager().getSaturationLevel();
      for (int i = 0; i < 10 && saturation - i * 2.0F > 0.0F; i++) {
         Identifier sprite = saturation - i * 2.0F > 1.0F ? SATURATION_FULL_SPRITE : SATURATION_HALF_SPRITE;
         context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, right - i * 8 - 10, top - 10, 9, 9);
      }
   }

   private static final class Smoothed {
      private final long durationMs;
      private float from;
      private float to;
      private float value;
      private long startNanos;
      private boolean initialized;

      private Smoothed(long durationMs) {
         this.durationMs = durationMs;
      }

      private void reset() {
         this.from = 0.0F;
         this.to = 0.0F;
         this.value = 0.0F;
         this.startNanos = 0L;
         this.initialized = false;
      }

      private void snap(float target) {
         this.from = target;
         this.to = target;
         this.value = target;
         this.startNanos = 0L;
         this.initialized = true;
      }

      private float update(float target) {
         long now = System.nanoTime();
         if (!this.initialized) {
            this.snap(target);
            return this.value;
         }
         if (Float.compare(target, this.to) != 0) {
            this.from = this.value;
            this.to = target;
            this.startNanos = now;
         }
         float elapsed = (now - this.startNanos) / 1_000_000.0F;
         float p = clamp01(elapsed / (float) this.durationMs);
         float eased = this.to >= this.from ? easeOutBack(p) : easeInBack(p);
         this.value = this.from + (this.to - this.from) * eased;
         return this.value;
      }
   }
}
