package ru.fluxvisuals.module.impl.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderTickCounter;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.render.EventRender2D;
import ru.fluxvisuals.event.render.RenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.animation.util.Animation;
import ru.fluxvisuals.util.render.animation.util.Easings;

/**
 * Анимации интерфейса: инвентарь, таб, чат и хотбар (перенесено из GodWeer).
 * Анимации применяются в миксинах HandleScreen/InventoryScreen/Tab/ChatHud/InGameHud.
 */
@IModule(
   name = "Better Minecraft",
   description = "Анимации инвентаря, таба, чата и хотбара",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class BetterMinecraft extends Module {
   public static final BooleanSetting inventoryAnim = new BooleanSetting("Inventory Anim", true);
   public static final BooleanSetting tabAnim = new BooleanSetting("Tab Anim", true);
   public static final BooleanSetting chatAnim = new BooleanSetting("Chat Anim", true);
   public static final BooleanSetting chatCharAnim = new BooleanSetting("Chat Letter Anim", true);
   public static final BooleanSetting hotbarAnim = new BooleanSetting("Hotbar Anim", true);
   public static final SliderSetting animSpeed = new SliderSetting("Anim Speed", 180.0F, 150.0F, 300.0F, 1.0F, false);

   public final Animation inventoryAnimation = new Animation();
   public final Animation tabAnimation = new Animation();
   public final Animation chatAnimation = new Animation();
   public final Animation hotbarAnimation = new Animation();

   private boolean lastInventoryOpen = false;
   private boolean lastTabOpen = false;
   private boolean lastChatOpen = false;
   private boolean initialized = false;

   public BetterMinecraft() {
      this.addSettings(new Setting[]{inventoryAnim, tabAnim, chatAnim, chatCharAnim, hotbarAnim, animSpeed});
   }

   @Override
   public void onEnable() {
      initialized = false;
      try { if (mc.worldRenderer != null) mc.worldRenderer.reload(); } catch (Exception ignored) {}
   }

   @Override
   public void onDisable() {
      inventoryAnimation.setValue(1.0);
      tabAnimation.setValue(0.0);
      chatAnimation.setValue(1.0);
      hotbarAnimation.setValue(1.0);
      lastInventoryOpen = false;
      lastTabOpen = false;
      lastChatOpen = false;
      initialized = false;
      try { if (mc.worldRenderer != null) mc.worldRenderer.reload(); } catch (Exception ignored) {}
   }

   @EventInit
   public void onRender(RenderEvent e) {
      if (mc.player == null) return;

      float dur = animSpeed.get() / 1000.0f;
      if (!initialized) {
         initialized = true;
         hotbarAnimation.setValue(1.0);
      }

      boolean inventoryOpen = mc.currentScreen instanceof HandledScreen<?>;
      boolean tabOpen = mc.options.playerListKey.isPressed();
      boolean chatOpen = mc.currentScreen instanceof ChatScreen;

      if (inventoryOpen != lastInventoryOpen) {
         if (inventoryOpen) inventoryAnimation.setValue(0.0);
         inventoryAnimation.run(inventoryOpen ? 1.0 : 0.0, dur, Easings.CUBIC_OUT);
         lastInventoryOpen = inventoryOpen;
      }
      inventoryAnimation.update();

      if (tabOpen != lastTabOpen) {
         tabAnimation.run(tabOpen ? 1.0 : 0.0, dur, Easings.CUBIC_OUT);
         lastTabOpen = tabOpen;
      }
      tabAnimation.update();

      if (chatOpen != lastChatOpen) {
         if (chatOpen) chatAnimation.setValue(0.0);
         chatAnimation.run(chatOpen ? 1.0 : 0.0, dur, Easings.CUBIC_OUT);
         lastChatOpen = chatOpen;
      }
      chatAnimation.update();

      boolean hotbarVisible = !inventoryOpen;
      float hotbarTarget = hotbarVisible ? 1.0f : 0.0f;
      hotbarAnimation.run(hotbarTarget, dur, Easings.CUBIC_OUT, true);
      hotbarAnimation.update();
   }

   @EventInit
   public void onRender2D(EventRender2D e) {
      if (!hotbarAnim.get() || mc.player == null) return;
      float t = hotbarAnimation.get();
      if (t <= 0.001f) return;

      DrawContext context = e.context();
      float offsetY = 30.0F * (1.0F - t);
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(0, offsetY);
   }

   @EventInit
   public void onRender2DPost(EventRender2D e) {
      if (!hotbarAnim.get() || mc.player == null) return;
      float t = hotbarAnimation.get();
      if (t <= 0.001f) return;

      DrawContext context = e.context();
      context.getMatrices().popMatrix();
   }

   public float getInventoryAnim() { return inventoryAnimation.get(); }
   public float getTabAnim() { return tabAnimation.get(); }
   public float getChatAnim() { return chatAnimation.get(); }
   public float getHotbarAnim() { return hotbarAnimation.get(); }
}
