package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.keyboard.Keyboard;
import ru.fluxvisuals.util.render.animation.util.Animation;
import ru.fluxvisuals.util.render.animation.util.Easings;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.animation.AnimationMath;
import ru.fluxvisuals.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public class KeyBindHUD {
   public static MinecraftClient mc = MinecraftClient.getInstance();

   private static final Animation openAnimation = new Animation();
   private static final Animation slideAnimation = new Animation();
   private static final Map<Item, DecelerateAnimation> pressAnims = new HashMap<>();

   private static final float HEIGHT = 14.5F;
   private static final float PADDING_SIDE = 4.0F;
   private static final float SPACING = 0.5F;
   private static final float ENTRY_GAP = 2.0F;
   private static final float TEXT_SIZE = 7.0F;
   private static final float COUNT_SIZE = 5.5F;

   private static class DecelerateAnimation {
      private float output = 0.0F;
      private float target = 0.0F;
      private float duration;

      public DecelerateAnimation(float duration, float initial) {
         this.duration = duration;
         this.output = initial;
         this.target = initial;
      }

      public void setPressed(boolean pressed) {
         target = pressed ? 1.0F : 0.0F;
      }

      public void update() {
         if (output != target) {
            float diff = target - output;
            float step = diff * 0.15F; // deceleration
            if (Math.abs(step) < 0.001F) {
               output = target;
            } else {
               output += step;
            }
         }
      }

      public float getOutput() {
         return output;
      }
   }

   public static void keybind(Renderer2D r2) {
      if (mc.player == null) return;

      // Collect active binds from enabled modules
      List<BindEntry> active = new ArrayList<>();
      if (FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null) {
         for (Module module : FluxVisualsClient.get.manager.getModules()) {
            if (module.bind > 0 && (module.enable || module.mAnim.getValue() > 0.001)) {
               Item item = getModuleItem(module);
               ItemStack stack = item.getDefaultStack();
               if (!mc.player.getItemCooldownManager().isCoolingDown(stack)) {
                  active.add(new BindEntry(item, module.bind, item.getDefaultStack()));
               }
            }
         }
      }

      boolean shown = mc.currentScreen instanceof ChatScreen || !active.isEmpty();
      openAnimation.update();
      openAnimation.run(shown ? 1.0 : 0.0, 0.4F, Easings.CIRC_OUT);
      float animAlpha = (float) openAnimation.get();

      // Slide animation (from top)
      slideAnimation.update();
      slideAnimation.run(shown ? 0.0 : -30.0, 0.4F, Easings.EXPO_OUT);
      float slideY = (float) slideAnimation.get();

      if (animAlpha < 0.01F) return;

      // Calculate widths
      float totalWidth = 0;
      List<Float> bindWidths = new ArrayList<>();
      for (BindEntry entry : active) {
         String keyName = Keyboard.keyName(entry.keyCode);
         float kw = r2.measureText(FontRegistry.INTER_MEDIUM, keyName, TEXT_SIZE).width + (PADDING_SIDE * 2);
         bindWidths.add(kw);
         totalWidth += HEIGHT + SPACING + kw + ENTRY_GAP;
      }
      if (!active.isEmpty()) totalWidth -= ENTRY_GAP;

      // Default position (center-bottom area)
      float preferredX = (mc.getWindow().getWidth() - totalWidth) / 2.0F;
      float preferredY = mc.getWindow().getHeight() - 80.0F;

      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag("keybinds", preferredX, preferredY, totalWidth, HEIGHT);
      float x = session.positionX();
      float y = session.positionY() + slideY;

      float currentX = x;

      for (int i = 0; i < active.size(); i++) {
         BindEntry entry = active.get(i);
         Item item = entry.item;
         int keyCode = entry.keyCode;
         float kw = bindWidths.get(i);
         DecelerateAnimation pAnim = pressAnims.computeIfAbsent(item, k -> new DecelerateAnimation(150, 1.0F));
         boolean isPressed = false;
         long handle = mc.getWindow().getHandle();

         if (keyCode >= 0) {
            if (keyCode < 8) {
               isPressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, keyCode) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            } else {
               isPressed = org.lwjgl.glfw.GLFW.glfwGetKey(handle, keyCode) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            }
         }

         pAnim.setPressed(isPressed);
         pAnim.update();
         float jumpOffset = pAnim.getOutput() * 3.0F;

         float renderY = y - jumpOffset;

         drawStyle(r2, currentX, renderY, HEIGHT, HEIGHT, animAlpha);

         int count = getCount(item);
         renderItemIcon(r2, entry.stack, count, currentX + 1.5F, renderY + 1.5F, 11.5F, animAlpha);

         float bindX = currentX + HEIGHT + SPACING;
         drawStyle(r2, bindX + 0.5F, renderY, kw, HEIGHT, animAlpha);

         String keyStr = Keyboard.keyName(keyCode);
         float tx = bindX + (kw / 2.0F) - (r2.measureText(FontRegistry.INTER_MEDIUM, keyStr, TEXT_SIZE).width / 2.0F);
         int bindColor = Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, (int) (255 * animAlpha));
         r2.text(FontRegistry.INTER_MEDIUM, tx, renderY + (HEIGHT / 2.0F - TEXT_SIZE / 2.0F) - 0.3F, TEXT_SIZE, keyStr, bindColor);

         currentX += (HEIGHT + SPACING + kw + ENTRY_GAP);
      }

      DraggableManager.getInstance().endDrag(session);
   }

   private static void drawStyle(Renderer2D r2, float rx, float ry, float rw, float rh, float alpha) {
      float round = 4.5F;
      if (Hud.blur.get("HUD")) {
         r2.prepareBlur(23.0F);
         r2.blur(rx, ry, rw, rh, round, alpha);
      }
      // Glass style (transparent with outline)
      int bgColor = Renderer2D.ColorUtil.replAlpha(0xFF3E3E47, 0);
      r2.rect(rx, ry, rw, rh, round, bgColor);
      int outAlpha = (int) Math.min(Math.max(25 * alpha, 0), 255);
      int outColor = Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, outAlpha);
      r2.rectOutline(rx, ry, rw, rh, round, outColor, 0.4F);
   }

   private static void renderItemIcon(Renderer2D r2, ItemStack stack, int count, float ix, float iy, float size, float alpha) {
      if (stack == null || stack.isEmpty()) return;

      // For Renderer2D, we can't directly draw items - use DrawContext when available
      // This is a simplified version - the actual item rendering needs DrawContext
      r2.rect(ix, iy, size, size, 3.0F, Renderer2D.ColorUtil.replAlpha(0xFF333333, (int)(alpha * 200)));

      if (count >= 0) {
         String cStr = String.valueOf(count);
         float cw = r2.measureText(FontRegistry.INTER_MEDIUM, cStr, COUNT_SIZE).width;

         float tx = ix + size - cw;
         float ty = iy + size - COUNT_SIZE;

         int cCol = count == 0 ?
                 Renderer2D.ColorUtil.replAlpha(0xFFFF5050, (int)(255 * alpha)) :
                 Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, (int)(200 * alpha));

         r2.text(FontRegistry.INTER_MEDIUM, tx, ty, COUNT_SIZE, cStr, cCol);
      }
   }

   private static int getCount(Item item) {
      if (mc.player == null) return 0;
      int count = 0;
      for (int i = 0; i < mc.player.getInventory().size(); i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == item) count += stack.getCount();
      }
      if (mc.player.getOffHandStack().getItem() == item) count += mc.player.getOffHandStack().getCount();
      return count;
   }

   private static Item getModuleItem(Module module) {
      // Return a representative item for the module based on its name/category
      // This is a placeholder - in a real implementation, modules might provide their own item
      String name = module.name.toLowerCase();
      if (name.contains("killaura") || name.contains("aura") || name.contains("combat")) return Items.DIAMOND_SWORD;
      if (name.contains("autoarmor") || name.contains("armor")) return Items.DIAMOND_CHESTPLATE;
      if (name.contains("autoclicker") || name.contains("click")) return Items.STICK;
      if (name.contains("scaffold") || name.contains("bridge")) return Items.OAK_PLANKS;
      if (name.contains("flight") || name.contains("fly")) return Items.FEATHER;
      if (name.contains("speed") || name.contains("velocity")) return Items.SUGAR;
      if (name.contains("reach")) return Items.DIAMOND_SWORD;
      if (name.contains("cheststealer") || name.contains("stealer")) return Items.CHEST;
      if (name.contains("autototem") || name.contains("totem")) return Items.TOTEM_OF_UNDYING;
      if (name.contains("crystal") || name.contains("auto")) return Items.END_CRYSTAL;
      if (name.contains("pearl")) return Items.ENDER_PEARL;
      if (name.contains("gap")) return Items.GOLDEN_APPLE;
      if (name.contains("pot")) return Items.POTION;
      if (name.contains("eat") || name.contains("food")) return Items.COOKED_BEEF;
      if (name.contains("inv")) return Items.CHEST;
      if (name.contains("blink")) return Items.ENDER_EYE;
      if (name.contains("freecam")) return Items.COMPASS;
      if (name.contains("xray") || name.contains("esp")) return Items.GLOWSTONE;
      if (name.contains("tracers")) return Items.REDSTONE;
      return null;
   }

   private static class BindEntry {
      final Item item;
      final int keyCode;
      final ItemStack stack;

      BindEntry(Item item, int keyCode, ItemStack stack) {
         this.item = item;
         this.keyCode = keyCode;
         this.stack = stack;
      }
   }

   /** Check if there are any active keybinds to display */
   public static boolean hasContent() {
      if (mc.player == null) return false;
      if (FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null) {
         for (Module module : FluxVisualsClient.get.manager.getModules()) {
            if (module.bind > 0 && (module.enable || module.mAnim.getValue() > 0.001)) {
               Item item = getModuleItem(module);
               ItemStack stack = item.getDefaultStack();
               if (!mc.player.getItemCooldownManager().isCoolingDown(stack)) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   /** Empty placeholder for chat screen positioning */
   public static void renderEmpty(Renderer2D r2) {
      float w = 130.0F;
      float h = 30.0F;
      float preferredX = (mc.getWindow().getWidth() - w) / 2.0F;
      float preferredY = mc.getWindow().getHeight() - 80.0F;
      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag("keybinds", preferredX, preferredY, w, h);
      float x = session.positionX();
      float y = session.positionY();
      drawStyle(r2, x, y, w, h, 1.0F);
      r2.text(FontRegistry.INTER_MEDIUM, x + 14.0F, y + 8.0F, 14.0F, "Keybinds",
            ru.fluxvisuals.util.color.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), 1.0F));
      DraggableManager.getInstance().endDrag(session);
   }
}