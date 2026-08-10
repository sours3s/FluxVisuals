package ru.fluxvisuals.module.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.event.input.MouseScrollEvent;
import ru.fluxvisuals.event.lifecycle.ClientTickEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.module.impl.visuals.HUD.ArrayListHUD;
import ru.fluxvisuals.module.impl.visuals.HUD.InformationHUD;
import ru.fluxvisuals.module.impl.visuals.HUD.KeyBindHUD;
import ru.fluxvisuals.module.impl.visuals.HUD.PotionsHUD;
import ru.fluxvisuals.module.impl.visuals.HUD.TargetHUD;
import ru.fluxvisuals.module.impl.visuals.HUD.WaterMark;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.render.animation.util.Animation;
import ru.fluxvisuals.util.render.animation.util.Easings;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

@IModule(
   name = "Hud",
   description = "Manages HUD overlays, notification layouts, and draggable elements",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class Hud extends Module {
   public static Animation animC = new Animation();
   public static Animation editHintAnim = new Animation();
   public static MultiBooleanSetting element = new MultiBooleanSetting(
      "Interface Elements",
      new BooleanSetting("Watermark", true),
      new BooleanSetting("ArrayList", true),
      new BooleanSetting("Information", true),
      new BooleanSetting("TargetHUD", true),
      new BooleanSetting("Notifications", true),
      new BooleanSetting("Keybind List", true),
      new BooleanSetting("Potion List", true),
      new BooleanSetting("Hotbar Binds", true)
   );
   public static MultiBooleanSetting notify = new MultiBooleanSetting(
      "Notification Settings",
      new BooleanSetting("Modules", true),
      new BooleanSetting("Low Health", true),
      new BooleanSetting("Low Durability", true)
   );
   // Пороги предупреждений для notify: здоровье (в HP) и прочность брони (в %).
   public final SliderSetting healthThreshold = new SliderSetting("Health Threshold", 8.0F, 2.0F, 20.0F, 1.0F, false);
   public final SliderSetting durabilityThreshold = new SliderSetting("Durability %", 15.0F, 5.0F, 60.0F, 5.0F, true);
   public static MultiBooleanSetting blur = new MultiBooleanSetting(
      "Blur",
      new BooleanSetting("HUD", true),
      new BooleanSetting("GUI", true)
   );
   private final List<Hud.Notification> notifications = new ArrayList<>();
   private final List<Hud.NotifItem> pendingNotificationItems = new ArrayList<>();
   private long lastHealthWarnAt = 0L;
   private long lastDurabilityWarnAt = 0L;

   public Hud() {
      this.addSettings(new Setting[]{
         element, notify, blur, healthThreshold, durabilityThreshold,
         InformationHUD.metrics, ArrayListHUD.sortByWidth, ArrayListHUD.smooth,
         TargetHUD.style, TargetHUD.showItems, TargetHUD.showOnHover, TargetHUD.particles, TargetHUD.scale,
         PotionsHUD.scale
      });
   }

   /**
    * Предупреждения о малом здоровье и износе брони (настройки "Low Health"/"Low Durability").
    * Раньше эти флаги были, но никто их не использовал — модуль не работал.
    */
   @EventInit
   public void onTick(ClientTickEvent event) {
      if (!this.enable || mc.player == null || mc.world == null) {
         return;
      }
      long now = System.currentTimeMillis();

      if (notify.get("Low Health") && mc.player.getHealth() <= this.healthThreshold.get()) {
         if (now - this.lastHealthWarnAt > 3000L) {
            this.lastHealthWarnAt = now;
            this.showNotification("warn", "Низкое здоровье!", 3000L, 0xFFFF5353);
         }
      }

      if (notify.get("Low Durability")) {
         EquipmentSlot[] armorSlots = new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
         for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            if (stack.isEmpty() || !stack.isDamageable()) {
               continue;
            }
            int maxDamage = stack.getMaxDamage();
            int remaining = Math.max(0, maxDamage - stack.getDamage());
            if (remaining == 0) {
               if (now - this.lastDurabilityWarnAt > 3000L) {
                  this.lastDurabilityWarnAt = now;
                  this.showNotification("warn", "Броня сломалась!", 3000L, 0xFFFF5353);
               }
               break;
            }
            float pct = remaining * 100.0F / maxDamage;
            if (pct <= this.durabilityThreshold.get()) {
               if (now - this.lastDurabilityWarnAt > 3000L) {
                  this.lastDurabilityWarnAt = now;
                  this.showNotification("warn", "Броня изнашивается!", 3000L, 0xFFFFB347);
               }
               break;
            }
         }
      }
   }

   @EventInit
   public void onScroll(MouseScrollEvent e) {
      if (!this.enable) {
         return;
      }
      if (!(mc.currentScreen instanceof ChatScreen)) {
         return;
      }
      long handle = e.window();
      if (handle == 0L) {
         return;
      }
      boolean ctrl = org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS
            || org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
      if (!ctrl) {
         return;
      }
      if (DraggableManager.getInstance().handleScroll(e.cursorX(), e.cursorY(), e.vertical(), true)) {
         e.cancel();
      }
   }

   @EventInit
   public void onRender(EventScreen e) {
      if (mc.player != null && mc.world != null) {
         Renderer2D r2 = e.renderer();
         if (r2 != null) {
            // Правило для ВСЕХ элементов: пустой элемент не виден в игре,
            // а при открытом чате показывается (чтобы его можно было передвинуть).
            // Если в элементе есть контент — он виден всегда.
            boolean chatOpen = mc.currentScreen instanceof ChatScreen;

            if (element.get("Notifications")) {
               this.renderNotifications(r2, e.drawContext());
            }

            if (element.get("Watermark")) {
               WaterMark.waterMark(r2);
            }

            if (element.get("Information")) {
               InformationHUD.information(r2);
            }

            if (element.get("TargetHUD")) {
               TargetHUD.targetHUD(r2, e.drawContext());
            }

            if (element.get("ArrayList")) {
               if (ArrayListHUD.hasContent()) {
                  ArrayListHUD.arraylist(r2);
               } else if (chatOpen) {
                  renderEmptyPlaceholder(r2, "arrayList", "ArrayList", 20.0F, 120.0F);
               }
            }

            if (element.get("Potion List")) {
               if (PotionsHUD.hasContent()) {
                  PotionsHUD.potions(r2, e.drawContext());
               } else if (chatOpen) {
                  renderEmptyPlaceholder(r2, "potionsHUD", "Potions", 20.0F, 474.0F);
               }
            }

            if (element.get("Keybind List")) {
               KeyBindHUD.keybind(r2);
            }

            renderEditHints(r2, e.viewportWidth(), e.viewportHeight());
         }
      }
   }

   /** Пустой элемент при открытом чате: перетаскиваемый плейсхолдер, чтобы видеть/двигать позицию. */
   private static void renderEmptyPlaceholder(Renderer2D r2, String dragId, String label, float preferredX, float preferredY) {
      float w = 88.0F;
      float h = 22.0F;
      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag(dragId, preferredX, preferredY, w, h);
      float x = session.positionX();
      float y = session.positionY();
      drawClientRectLight(r2, x, y, w, h, 6.0F, 1.0F, 1.0F);
      r2.text(FontRegistry.INTER_MEDIUM, x + 10.0F, y + 6.0F, 11.0F, label, 0x55FFFFFF);
      DraggableManager.getInstance().endDrag(session);
   }

   public void showNotification(String icon, String text, long duration) {
      Hud.Notification notification = new Hud.Notification(icon, text, duration);
      this.notifications.add(notification);
   }

   public void showNotification(String icon, String text, long duration, int iconColor) {
      Hud.Notification notification = new Hud.Notification(icon, text, duration, iconColor);
      this.notifications.add(notification);
   }

   public void showNotification(String icon, String text) {
      this.showNotification(icon, text, 3000L);
   }

   // Notification that shows the actual item icon on the left (used by UseTracker etc.).
   public void showItemNotification(ItemStack stack, String text, long duration, int iconColor) {
      if (stack == null || stack.isEmpty()) {
         return;
      }
      Hud.Notification notification = new Hud.Notification("item", text, duration, iconColor);
      notification.itemStack = stack.copy();
      this.notifications.add(notification);
   }

   // Two-part item notification: "prefix" (who + action, muted) + "highlight" (item name, accented/bold).
   public void showItemNotification(ItemStack stack, String prefix, String highlight, long duration, int accentColor) {
      if (stack == null || stack.isEmpty()) {
         return;
      }
      String fullText = highlight == null || highlight.isEmpty() ? prefix : prefix + " " + highlight;
      Hud.Notification notification = new Hud.Notification("item", fullText, duration, accentColor);
      notification.itemStack = stack.copy();
      notification.prefix = prefix;
      notification.highlight = highlight;
      this.notifications.add(notification);
   }

   public void renderNotifications(Renderer2D matrix, DrawContext drawContext) {
      this.notifications.removeIf(notificationx -> {
         notificationx.animation.update();
         notificationx.yAnimation.update();
         return notificationx.isExpired() && notificationx.animation.getValue() <= 0.01;
      });
      if (!this.notifications.isEmpty()) {
         float margin = 4.0F;
         float notificationHeight = 40.0F;
         float spacing = 6.0F;

         // Compute bounding box of the entire notification stack for draggability.
         float maxNotificationWidth = 0.0F;
         float stackHeight = 0.0F;
         for (Hud.Notification n : this.notifications) {
            float animValue = n.animation.get();
            if (animValue <= 0.01F) {
               continue;
            }
            float iconWidth = 8.0F;
            boolean twoPart = n.highlight != null && !n.highlight.isEmpty();
            float prefixWidth = twoPart
               ? matrix.measureText(FontRegistry.INTER_MEDIUM, n.prefix, 26.0F).width
               : 0.0F;
            float spaceWidth = twoPart ? matrix.measureText(FontRegistry.INTER_MEDIUM, " ", 26.0F).width : 0.0F;
            float highlightWidth = twoPart
               ? matrix.measureText(FontRegistry.INTER_SEMIBOLD, n.highlight, 27.0F).width
               : 0.0F;
            float textWidth = twoPart
               ? prefixWidth + spaceWidth + highlightWidth
               : matrix.measureText(FontRegistry.INTER_MEDIUM, n.text, 26.0F).width;
            float w = margin * 3.0F + iconWidth + textWidth + 8.0F + 30.0F + 8.0F;
            if (w > maxNotificationWidth) {
               maxNotificationWidth = w;
            }
            stackHeight += (notificationHeight + spacing) * animValue;
         }
         maxNotificationWidth = Math.max(maxNotificationWidth, 200.0F);
         stackHeight = Math.max(stackHeight, notificationHeight);

         float anchorPreferredX = mc.getWindow().getWidth() / 2.0F - maxNotificationWidth / 2.0F;
         float anchorPreferredY = mc.getWindow().getHeight() / 2.0F + 140.0F;
         DraggableManager.DragSession notifSession = DraggableManager.getInstance()
               .beginDrag("notifications", anchorPreferredX, anchorPreferredY, maxNotificationWidth, stackHeight);
         float anchorX = notifSession.positionX();
         float anchorY = notifSession.positionY();
         float currentY = anchorY;

         for (int i = this.notifications.size() - 1; i >= 0; i--) {
            Hud.Notification notification = this.notifications.get(i);
            boolean shouldShow = !notification.isExpired();
            notification.animation.run(shouldShow ? 1.0 : 0.0, 0.16F, Easings.QUAD_OUT, false);
            float animValue = notification.animation.get();
            if (!(animValue <= 0.01F)) {
               float iconWidth = 8.0F;
               boolean twoPart = notification.highlight != null && !notification.highlight.isEmpty();
               float prefixWidth = twoPart
                  ? matrix.measureText(FontRegistry.INTER_MEDIUM, notification.prefix, 26.0F).width
                  : 0.0F;
               float spaceWidth = twoPart ? matrix.measureText(FontRegistry.INTER_MEDIUM, " ", 26.0F).width : 0.0F;
               float highlightWidth = twoPart
                  ? matrix.measureText(FontRegistry.INTER_SEMIBOLD, notification.highlight, 27.0F).width
                  : 0.0F;
               float textWidth = twoPart
                  ? prefixWidth + spaceWidth + highlightWidth
                  : matrix.measureText(FontRegistry.INTER_MEDIUM, notification.text, 26.0F).width;
               float notificationWidth = margin * 3.0F + iconWidth + textWidth + 8.0F + 30.0F;
               float screenCenterX = anchorX + maxNotificationWidth / 2.0F;
               float targetX = screenCenterX - notificationWidth / 2.0F;
               notification.yAnimation.run(currentY, 0.1F, Easings.QUAD_OUT, false);
               float scale = 0.9F + 0.1F * animValue;
               matrix.pushTranslation(targetX + notificationWidth / 2.0F, currentY + notificationHeight / 2.0F);
               matrix.pushScale(scale, scale, 0.0F);
               matrix.pushTranslation(-(targetX + notificationWidth / 2.0F), -(currentY + notificationHeight / 2.0F));
               drawClientRect(matrix, targetX - 4.0F, currentY, notificationWidth + 8.0F, notificationHeight, 13.0F, animValue, 1.0F);
               matrix.rect(
                  targetX + 35.0F,
                  currentY + notificationHeight / 2.0F + 3.0F - 10.5F,
                  2.0F,
                  11.22F,
                  4.0F,
                  ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), animValue * 0.2F)
               );
               int iconColor = notification.iconColor == -1
                  ? ColorUtil.replAlpha(ColorUtil.fade(), animValue)
                  : ColorUtil.replAlpha(notification.iconColor, animValue);
               if (notification.itemStack != null && !notification.itemStack.isEmpty()) {
                  float cx = targetX + notificationWidth / 2.0F;
                  float cy = currentY + notificationHeight / 2.0F;
                  float itemX = targetX + margin + 7.0F + 11.0F;
                  float itemY = currentY + notificationHeight / 2.0F + 0.5F;
                  this.pendingNotificationItems.add(new Hud.NotifItem(notification.itemStack, cx, cy, itemX, itemY, scale, animValue));
               } else if (notification.icon.contains("on")) {
                  matrix.text(
                     FontRegistry.ICONS,
                     targetX + margin + margin + 2.0F,
                     currentY + notificationHeight / 2.0F + 7.5F,
                     37.0F,
                     "L",
                     ColorUtil.replAlpha(iconColor, animValue)
                  );
               } else if (notification.icon.contains("off")) {
                  matrix.text(
                     FontRegistry.ICONS,
                     targetX + margin + margin + 2.0F,
                     currentY + notificationHeight / 2.0F + 7.5F,
                     37.0F,
                     "J",
                     ColorUtil.replAlpha(iconColor, animValue)
                  );
               } else if (notification.icon.contains("warn")) {
                  matrix.text(
                     FontRegistry.ICONS,
                     targetX + margin + margin + 2.0F,
                     currentY + notificationHeight / 2.0F + 5.5F,
                     28.0F,
                     "F",
                     ColorUtil.replAlpha(iconColor, animValue)
                  );
               } else if (notification.icon.contains("cfg")) {
                  matrix.text(
                     FontRegistry.ICONS,
                     targetX + margin + margin + 2.0F,
                     currentY + notificationHeight / 2.0F + 5.5F,
                     28.0F,
                     "G",
                     ColorUtil.replAlpha(iconColor, animValue)
                  );
               } else {
                  matrix.text(
                     FontRegistry.ICONS,
                     targetX + margin + margin + 2.0F,
                     currentY + notificationHeight / 2.0F + 7.5F,
                     37.0F,
                     notification.icon,
                     ColorUtil.replAlpha(iconColor, animValue)
                  );
               }

               float textBaseX = targetX + notificationWidth / 2.0F + margin - textWidth / 2.0F + 12.0F;
               float textBaseY = currentY + notificationHeight / 2.0F + 3.3F;
               if (twoPart) {
                  int prefixColor = ColorUtil.replAlpha(0xFFC2C6D0, animValue);
                  int highlightColor = ColorUtil.replAlpha(
                     notification.iconColor == -1 ? 0xFFFFFFFF : notification.iconColor, animValue
                  );
                  matrix.text(FontRegistry.INTER_MEDIUM, textBaseX, textBaseY, 26.0F, notification.prefix, prefixColor);
                  matrix.text(
                     FontRegistry.INTER_SEMIBOLD,
                     textBaseX + prefixWidth + spaceWidth,
                     textBaseY,
                     27.0F,
                     notification.highlight,
                     highlightColor
                  );
               } else {
                  matrix.text(
                     FontRegistry.INTER_MEDIUM,
                     textBaseX,
                     textBaseY,
                     26.0F,
                     notification.text,
                     ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), animValue)
                  );
               }
               matrix.popTransform();
               matrix.popScale();
               matrix.popTransform();
               currentY += (notificationHeight + spacing) * animValue;
            }
         }

         if (drawContext != null && !this.pendingNotificationItems.isEmpty()) {
            for (Hud.NotifItem it : this.pendingNotificationItems) {
               drawContext.getMatrices().pushMatrix();
               drawContext.getMatrices().translate(it.cx(), it.cy());
               drawContext.getMatrices().scale(it.scale(), it.scale());
               drawContext.getMatrices().translate(-it.cx(), -it.cy());
               
               float itemScale = (22.0F / 16.0F) * Math.max(0.0F, it.animValue());
               drawContext.getMatrices().translate(it.itemX(), it.itemY());
               drawContext.getMatrices().scale(itemScale, itemScale);
               drawContext.getMatrices().translate(-8.0F, -8.0F);
               
               drawContext.drawItem(it.stack(), 0, 0);
               drawContext.getMatrices().popMatrix();
            }
         }

         DraggableManager.getInstance().endDrag(notifSession);
      }

      this.pendingNotificationItems.clear();
   }

   public static void drawClientRect(Renderer2D r2, float x, float y, float w, float h, float radius, float alpha, float thickness) {
      r2.shadow(x, y, w, h, radius, 8.6F, 0.5F, ColorUtil.getColor(0, alpha * 0.15F));
      if (blur.get("HUD")) {
         r2.prepareBlur(23.0F);
         r2.blur(x, y, w, h, radius, alpha);
      }

      if (thickness > 0.0F) {
         r2.rectOutline(x - 1.0F, y - 1.0F, w + 2.0F, h + 2.0F, radius, ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), alpha * 0.1F), thickness);
      }
      r2.rect(x, y, w, h, radius, ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), alpha * 0.7F));
   }

   /**
    * Lightweight version of drawClientRect — NO shadow, NO blur.
    * Use for mass-rendered elements (ArrayList pills, NameTags, Potions rows)
    * where full drawClientRect would kill FPS.
    */
   public static void drawClientRectLight(Renderer2D r2, float x, float y, float w, float h, float radius, float alpha, float thickness) {
      if (thickness > 0.0F) {
         r2.rectOutline(x - 1.0F, y - 1.0F, w + 2.0F, h + 2.0F, radius, ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), alpha * 0.1F), thickness);
      }
      r2.rect(x, y, w, h, radius, ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), alpha * 0.7F));
   }

   private static void renderEditHints(Renderer2D r2, int viewportWidth, int viewportHeight) {
      editHintAnim.update();
      boolean chatOpen = mc.currentScreen instanceof ChatScreen;
      editHintAnim.run(chatOpen ? 1.0 : 0.0, 0.45F, Easings.CIRC_OUT, false);
      float anim = (float) editHintAnim.get();
      if (anim <= 0.005F) {
         return;
      }

      String moveLabel = "ЛКМ";
      String moveDesc = " — переместить";
      String scaleLabel = "Ctrl + колесо";
      String scaleDesc = " — масштаб";

      float labelSize = 22.0F;
      float descSize = 22.0F;

      float moveLabelW = r2.measureText(FontRegistry.INTER_SEMIBOLD, moveLabel, labelSize).width;
      float moveDescW = r2.measureText(FontRegistry.INTER_MEDIUM, moveDesc, descSize).width;
      float scaleLabelW = r2.measureText(FontRegistry.INTER_SEMIBOLD, scaleLabel, labelSize).width;
      float scaleDescW = r2.measureText(FontRegistry.INTER_MEDIUM, scaleDesc, descSize).width;

      float moveTotalW = moveLabelW + moveDescW;
      float scaleTotalW = scaleLabelW + scaleDescW;

      float gap = 18.0F;
      float separatorW = 1.5F;

      float totalWidth = moveTotalW + gap + separatorW + gap + scaleTotalW;

      // Place comfortably above the hotbar.
      float baseY = viewportHeight - 150.0F;
      float liftedY = baseY + (1.0F - anim) * 14.0F;
      float startX = (viewportWidth - totalWidth) / 2.0F;

      r2.pushAlpha(anim);

      int mainColor = Renderer2D.ColorUtil.getMainColor(1, 1);
      int textColor = Renderer2D.ColorUtil.getTextColor(1, 1);
      int mutedColor = ColorUtil.replAlpha(textColor, 0.55F);
      int separatorColor = ColorUtil.replAlpha(mainColor, 0.35F);

      float textY = liftedY + 18.0F;
      float cursorX = startX;

      r2.text(FontRegistry.INTER_SEMIBOLD, cursorX, textY, labelSize, moveLabel, mainColor);
      cursorX += moveLabelW;
      r2.text(FontRegistry.INTER_MEDIUM, cursorX, textY, descSize, moveDesc, mutedColor);
      cursorX += moveDescW + gap;

      r2.rect(cursorX, liftedY + 6.0F, separatorW, 14.0F, 1.0F, separatorColor);
      cursorX += separatorW + gap;

      r2.text(FontRegistry.INTER_SEMIBOLD, cursorX, textY, labelSize, scaleLabel, mainColor);
      cursorX += scaleLabelW;
      r2.text(FontRegistry.INTER_MEDIUM, cursorX, textY, descSize, scaleDesc, mutedColor);

      r2.popAlpha();
   }

   @Environment(EnvType.CLIENT)
   public record NotifItem(ItemStack stack, float cx, float cy, float itemX, float itemY, float scale, float animValue) {
   }

   @Environment(EnvType.CLIENT)
   public static class Notification {
      private String icon;
      private String text;
      private long createTime;
      private long duration;
      private Animation animation = new Animation();
      private Animation yAnimation = new Animation();
      private int iconColor = -1;
      private ItemStack itemStack = null;
      private String prefix = null;
      private String highlight = null;

      public Notification(String icon, String text, long duration) {
         this.icon = icon;
         this.text = text;
         this.duration = duration;
         this.createTime = System.currentTimeMillis();
      }

      public Notification(String icon, String text, long duration, int iconColor) {
         this.icon = icon;
         this.text = text;
         this.duration = duration;
         this.createTime = System.currentTimeMillis();
         this.iconColor = iconColor;
      }

      public boolean isExpired() {
         return System.currentTimeMillis() - this.createTime > this.duration;
      }

      public float getProgress() {
         return Math.min(1.0F, (float)(System.currentTimeMillis() - this.createTime) / (float)this.duration);
      }

      @Generated
      public String getIcon() {
         return this.icon;
      }

      @Generated
      public String getText() {
         return this.text;
      }

      @Generated
      public long getCreateTime() {
         return this.createTime;
      }

      @Generated
      public long getDuration() {
         return this.duration;
      }

      @Generated
      public Animation getAnimation() {
         return this.animation;
      }

      @Generated
      public Animation getYAnimation() {
         return this.yAnimation;
      }

      @Generated
      public int getIconColor() {
         return this.iconColor;
      }

      @Generated
      public void setIcon(String icon) {
         this.icon = icon;
      }

      @Generated
      public void setText(String text) {
         this.text = text;
      }

      @Generated
      public void setCreateTime(long createTime) {
         this.createTime = createTime;
      }

      @Generated
      public void setDuration(long duration) {
         this.duration = duration;
      }

      @Generated
      public void setAnimation(Animation animation) {
         this.animation = animation;
      }

      @Generated
      public void setYAnimation(Animation yAnimation) {
         this.yAnimation = yAnimation;
      }

      @Generated
      public void setIconColor(int iconColor) {
         this.iconColor = iconColor;
      }

      @Generated
      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof Hud.Notification other)) {
            return false;
         } else if (!other.canEqual(this)) {
            return false;
         } else if (this.getCreateTime() != other.getCreateTime()) {
            return false;
         } else if (this.getDuration() != other.getDuration()) {
            return false;
         } else if (this.getIconColor() != other.getIconColor()) {
            return false;
         } else {
            Object this$icon = this.getIcon();
            Object other$icon = other.getIcon();
            if (this$icon == null ? other$icon == null : this$icon.equals(other$icon)) {
               Object this$text = this.getText();
               Object other$text = other.getText();
               if (this$text == null ? other$text == null : this$text.equals(other$text)) {
                  Object this$animation = this.getAnimation();
                  Object other$animation = other.getAnimation();
                  if (this$animation == null ? other$animation == null : this$animation.equals(other$animation)) {
                     Object this$yAnimation = this.getYAnimation();
                     Object other$yAnimation = other.getYAnimation();
                     return this$yAnimation == null ? other$yAnimation == null : this$yAnimation.equals(other$yAnimation);
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }

      @Generated
      protected boolean canEqual(Object other) {
         return other instanceof Hud.Notification;
      }

      @Generated
      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         long $createTime = this.getCreateTime();
         result = result * 59 + (int)($createTime >>> 32 ^ $createTime);
         long $duration = this.getDuration();
         result = result * 59 + (int)($duration >>> 32 ^ $duration);
         result = result * 59 + this.getIconColor();
         Object $icon = this.getIcon();
         result = result * 59 + ($icon == null ? 43 : $icon.hashCode());
         Object $text = this.getText();
         result = result * 59 + ($text == null ? 43 : $text.hashCode());
         Object $animation = this.getAnimation();
         result = result * 59 + ($animation == null ? 43 : $animation.hashCode());
         Object $yAnimation = this.getYAnimation();
         return result * 59 + ($yAnimation == null ? 43 : $yAnimation.hashCode());
      }

      @Generated
      @Override
      public String toString() {
         return "Hud.Notification(icon="
            + this.getIcon()
            + ", text="
            + this.getText()
            + ", createTime="
            + this.getCreateTime()
            + ", duration="
            + this.getDuration()
            + ", animation="
            + this.getAnimation()
            + ", yAnimation="
            + this.getYAnimation()
            + ", iconColor="
            + this.getIconColor()
            + ")";
      }
   }
}
