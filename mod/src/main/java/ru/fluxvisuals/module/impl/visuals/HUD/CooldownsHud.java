package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Список активных кулдаунов предметов. Записи объединяются по группе кулдауна,
 * чтобы несколько предметов с общим кулдауном занимали одну строку.
 */
@Environment(EnvType.CLIENT)
public final class CooldownsHud {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private static final String DRAG_ID = "cooldownsHud";
   private static final int INVENTORY_SIZE = 36;

   // Размеры в scaled-единицах.
   private static final float ROW_HEIGHT = 15.0F;
   private static final float PANEL_MIN_WIDTH = 92.0F;
   private static final float BADGE_WIDTH = 22.0F;
   private static final float PADDING = 7.0F;

   private static final Map<Identifier, Sample> samples = new HashMap<>();

   private CooldownsHud() {
   }

   public static boolean hasContent() {
      if (mc.player == null) {
         return false;
      }
      return !collectCooldowns(mc.player.getItemCooldownManager()).isEmpty();
   }

   public static void render(Renderer2D r2, DrawContext ctx) {
      if (r2 == null || mc.player == null || mc.world == null) {
         return;
      }
      ItemCooldownManager manager = mc.player.getItemCooldownManager();
      Map<Identifier, ItemStack> active = collectCooldowns(manager);
      if (active.isEmpty()) {
         samples.clear();
         return;
      }

      float gs = guiScale();
      float fontSize = 7.0F * gs;
      float timeSize = 6.0F * gs;
      float badgeW = BADGE_WIDTH * gs;
      long now = System.currentTimeMillis();

      List<Row> rows = new ArrayList<>();
      float nameW = 0.0F;
      float timeW = 0.0F;
      for (Map.Entry<Identifier, ItemStack> entry : active.entrySet()) {
         float progress = clamp01(manager.getCooldownProgress(entry.getValue(), 0.0F));
         Sample sample = samples.computeIfAbsent(entry.getKey(), key -> new Sample(now, progress));
         String time = updateRemaining(sample, progress, now);
         String name = entry.getValue().getName().getString();
         rows.add(new Row(name, time, progress));
         nameW = Math.max(nameW, r2.measureText(FontRegistry.INTER_MEDIUM, name, fontSize).width);
         timeW = Math.max(timeW, r2.measureText(FontRegistry.INTER_MEDIUM, time, timeSize).width);
      }
      samples.keySet().removeIf(group -> !active.containsKey(group));

      float rowH = ROW_HEIGHT * gs;
      float pad = PADDING * gs;
      float panelW = Math.max(PANEL_MIN_WIDTH * gs, pad * 2.0F + nameW + timeW + badgeW + 12.0F * gs);
      float panelH = rows.size() * rowH + pad * 2.0F;

      // Предпочтительная позиция: правый край, ниже центра экрана (scaled-координаты).
      float preferredX = (mc.getWindow().getScaledWidth() - panelW / gs - 20.0F) * gs;
      float preferredY = (mc.getWindow().getScaledHeight() - panelH / gs - 150.0F) * gs;

      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag(DRAG_ID, preferredX, preferredY, panelW, panelH);
      try {
         float x = session.positionX();
         float y = session.positionY();
         Hud.drawClientRect(r2, x, y, panelW, panelH, 7.0F * gs, 1.0F, 1.0F);

         float rowY = y + pad;
         int accent = Renderer2D.ColorUtil.getMainColor(1, 1);
         for (Row row : rows) {
            Hud.drawClientRectLight(r2, x + pad * 0.5F, rowY,
                  panelW - pad, rowH - gs, 4.0F * gs, 0.7F, 0.0F);

            r2.text(FontRegistry.INTER_MEDIUM, x + pad, rowY + rowH * 0.58F,
                  fontSize, row.name(), Renderer2D.ColorUtil.getTextColor(1, 1));

            float timeX = x + pad + nameW + 6.0F * gs;
            r2.text(FontRegistry.INTER_MEDIUM, timeX, rowY + rowH * 0.58F,
                  timeSize, row.time(), ColorUtil.replAlpha(accent, 0.9F));

            float badgeX = x + panelW - pad - badgeW;
            float badgeY = rowY + 2.0F * gs;
            r2.rect(badgeX, badgeY, badgeW, rowH - 5.0F * gs, 3.0F * gs,
                  ColorUtil.replAlpha(accent, 0.18F));
            String percent = Math.round(row.progress() * 100.0F) + "%";
            float percentW = r2.measureText(FontRegistry.INTER_MEDIUM, percent, timeSize).width;
            r2.text(FontRegistry.INTER_MEDIUM, badgeX + (badgeW - percentW) * 0.5F,
                  badgeY + (rowH - 5.0F * gs) * 0.68F, timeSize, percent, accent);
            rowY += rowH;
         }
      } finally {
         DraggableManager.getInstance().endDrag(session);
      }
   }

   /** Собирает по одному предмету на каждую активную группу кулдауна. */
   private static Map<Identifier, ItemStack> collectCooldowns(ItemCooldownManager manager) {
      Map<Identifier, ItemStack> result = new LinkedHashMap<>();
      if (manager == null) {
         return result;
      }
      for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
         addStack(manager, mc.player.getInventory().getStack(slot), result);
      }
      addStack(manager, mc.player.getOffHandStack(), result);
      return result;
   }

   private static void addStack(ItemCooldownManager manager, ItemStack stack, Map<Identifier, ItemStack> result) {
      if (stack.isEmpty() || !manager.isCoolingDown(stack)) {
         return;
      }
      Identifier group = manager.getGroup(stack);
      if (group != null) {
         result.putIfAbsent(group, stack);
      }
   }

   /**
    * Оставшееся время оценивается по скорости убывания прогресса: значение
    * кулдауна — это доля оставшихся тиков, поэтому дельта даёт длительность тика.
    */
   private static String updateRemaining(Sample sample, float progress, long now) {
      long deltaMs = now - sample.timeMs;
      float deltaProgress = sample.progress - progress;
      if (deltaMs >= 20L && deltaProgress > 0.0005F) {
         float remaining = progress * (float) deltaMs / deltaProgress;
         sample.remainingMs = sample.remainingMs <= 0.0F
               ? remaining
               : sample.remainingMs * 0.65F + remaining * 0.35F;
      }
      sample.timeMs = now;
      sample.progress = progress;
      if (sample.remainingMs <= 0.0F) {
         return "--";
      }
      return String.format(Locale.ROOT, "%.1fs", Math.max(0.0F, sample.remainingMs / 1000.0F));
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private static float guiScale() {
      float scale = mc.getWindow().getScaleFactor();
      return scale > 0.0F ? scale : 1.0F;
   }

   private static final class Sample {
      private long timeMs;
      private float progress;
      private float remainingMs;

      private Sample(long timeMs, float progress) {
         this.timeMs = timeMs;
         this.progress = progress;
      }
   }

   private record Row(String name, String time, float progress) {
   }
}
