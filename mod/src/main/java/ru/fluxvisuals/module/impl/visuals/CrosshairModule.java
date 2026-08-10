package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Crosshair — кастомный прицел с каталогом стилей.
 * Миксин InGameHudMixin.cancelVanillaCrosshair() отменяет ванильный.
 */
@IModule(name = "Crosshair", description = "Кастомный прицел с каталогом стилей", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class CrosshairModule extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static CrosshairModule INSTANCE;

   public final ModeSetting style = new ModeSetting("Style", "Cross", "Cross", "Dot", "GapDot", "T", "Corner", "Circle");
   public final SliderSetting size = new SliderSetting("Size", 6.0F, 2.0F, 15.0F, 0.5F, false);
   public final SliderSetting thickness = new SliderSetting("Thickness", 1.5F, 0.5F, 4.0F, 0.5F, false);
   public final SliderSetting gap = new SliderSetting("Gap", 3.0F, 0.0F, 10.0F, 0.5F, false);
   public final SliderSetting colorR = new SliderSetting("Red", 255.0F, 0.0F, 255.0F, 1.0F, false);
   public final SliderSetting colorG = new SliderSetting("Green", 255.0F, 0.0F, 255.0F, 1.0F, false);
   public final SliderSetting colorB = new SliderSetting("Blue", 255.0F, 0.0F, 255.0F, 1.0F, false);

   public CrosshairModule() {
      this.addSettings(new Setting[]{style, size, thickness, gap, colorR, colorG, colorB});
      INSTANCE = this;
   }

   public static CrosshairModule getInstance() { return INSTANCE; }

   public void renderCrosshair(Renderer2D r2) {
      float cx = mc.getWindow().getScaledWidth() / 2.0F;
      float cy = mc.getWindow().getScaledHeight() / 2.0F;
      float s = size.get();
      float t = thickness.get();
      float g = gap.get();
      int color = 0xFF000000 | ((int) colorR.get() << 16) | ((int) colorG.get() << 8) | (int) colorB.get();
      color = Renderer2D.ColorUtil.replAlpha(color, 200);

      switch (style.get()) {
         case "Cross" -> {
            r2.rect(cx - t / 2, cy - s - g, t, s, 0, color); // top
            r2.rect(cx - t / 2, cy + g, t, s, 0, color); // bottom
            r2.rect(cx - s - g, cy - t / 2, s, t, 0, color); // left
            r2.rect(cx + g, cy - t / 2, s, t, 0, color); // right
         }
         case "Dot" -> {
            r2.circle(cx, cy, s / 2, 0, 1.0F, color);
         }
         case "GapDot" -> {
            r2.rect(cx - t / 2, cy - s - g, t, s, 0, color);
            r2.rect(cx - t / 2, cy + g, t, s, 0, color);
            r2.rect(cx - s - g, cy - t / 2, s, t, 0, color);
            r2.rect(cx + g, cy - t / 2, s, t, 0, color);
            r2.circle(cx, cy, 1.5F, 0, 1.0F, color);
         }
         case "T" -> {
            r2.rect(cx - s, cy - t / 2, s * 2, t, 0, color); // horizontal
            r2.rect(cx - t / 2, cy, t, s, 0, color); // vertical down
         }
         case "Corner" -> {
            float c = s * 0.7F;
            // Top-left
            r2.rect(cx - g - c, cy - t / 2, c, t, 0, color);
            r2.rect(cx - t / 2, cy - g - c, t, c, 0, color);
            // Top-right
            r2.rect(cx + g, cy - t / 2, c, t, 0, color);
            r2.rect(cx - t / 2, cy - g - c, t, c, 0, color);
            // Bottom-left
            r2.rect(cx - g - c, cy - t / 2, c, t, 0, color);
            r2.rect(cx - t / 2, cy + g, t, c, 0, color);
            // Bottom-right
            r2.rect(cx + g, cy - t / 2, c, t, 0, color);
            r2.rect(cx - t / 2, cy + g, t, c, 0, color);
         }
         case "Circle" -> {
            r2.circle(cx, cy, s, 0, 1.0F, Renderer2D.ColorUtil.replAlpha(color, 100));
            r2.circle(cx, cy, s, 0, 1.0F, color);
         }
      }
   }
}
