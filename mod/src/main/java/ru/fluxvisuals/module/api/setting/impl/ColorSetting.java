package ru.fluxvisuals.module.api.setting.impl;

import java.awt.Color;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ColorSetting extends HueSetting {
   private int rgba;
   private float lastHue;
   private float lastSaturation;
   private float lastBrightness;

   public ColorSetting(String name, Color defaultColor, Supplier<Boolean> visibility) {
      super(name, 0.0F, 1.0F, 1.0F);
      this.hidden = visibility;
      this.setColor(defaultColor);
   }

   public ColorSetting(String name, Color defaultColor) {
      this(name, defaultColor, () -> false);
   }

   @Override
   public void setColor(Color color) {
      this.rgba = color.getRGB();
      super.setColor(color);
      this.lastHue = this.current;
      this.lastSaturation = this.saturation;
      this.lastBrightness = this.brightness;
   }

   @Override
   public Color getColor() {
      this.syncFromHue();
      return new Color(this.rgba, true);
   }

   @Override
   public int getRGB() {
      return this.getColor().getRGB();
   }

   @Override
   public int getRGBA(int alpha) {
      Color color = this.getColor();
      return alpha << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
   }

   private void syncFromHue() {
      if (Float.compare(this.current, this.lastHue) != 0
            || Float.compare(this.saturation, this.lastSaturation) != 0
            || Float.compare(this.brightness, this.lastBrightness) != 0) {
         this.rgba = Color.HSBtoRGB(this.current / this.maximum, this.saturation, this.brightness);
         this.lastHue = this.current;
         this.lastSaturation = this.saturation;
         this.lastBrightness = this.brightness;
      }
   }
}
