package ru.fluxvisuals.module.api;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.util.render.math.animation.Animation;
import ru.fluxvisuals.util.render.math.animation.impl.EaseInOutQuad;

@Environment(EnvType.CLIENT)
public enum Theme {
   THEME1(new Color(9150463), new Color(856880), new Color(1251638), new Color(9150463), new Color(13489663), new Color(9216255)),
   THEME2(new Color(16747403), new Color(3149069), new Color(3543827), new Color(16747403), new Color(16764365), new Color(16747660)),
   THEME3(new Color(16771211), new Color(3156749), new Color(3551507), new Color(16771211), new Color(16774349), new Color(16771724)),
   THEME4(new Color(9175006), new Color(864296), new Color(1259050), new Color(9175006), new Color(13500399), new Color(9240537)),
   THEME5(new Color(9165567), new Color(859440), new Color(1254710), new Color(9165567), new Color(13494783), new Color(9232639)),
   THEME6(new Color(14191615), new Color(2559280), new Color(2888502), new Color(14191615), new Color(16240127), new Color(14388479)),
   THEME7(new Color(16747513), new Color(3149103), new Color(3412790), new Color(16747513), new Color(16764408), new Color(16747773)),
   THEME8(new Color(16747467), new Color(3149089), new Color(3543849), new Color(16747467), new Color(16764391), new Color(16747733)),

   // ===== Beautiful / unusual themes (main = accent, bg/bg2 = dark gradient, text/text2 = light) =====
   SUNSET(new Color(255, 126, 95), new Color(40, 18, 48), new Color(64, 24, 62), new Color(255, 126, 95), new Color(255, 224, 210), new Color(255, 178, 150)),
   AURORA(new Color(74, 255, 180), new Color(8, 30, 36), new Color(12, 48, 56), new Color(74, 255, 180), new Color(206, 255, 236), new Color(120, 242, 212)),
   GALAXY(new Color(170, 120, 255), new Color(16, 12, 32), new Color(30, 20, 56), new Color(170, 120, 255), new Color(226, 216, 255), new Color(190, 168, 255)),
   NEON(new Color(255, 60, 172), new Color(12, 10, 28), new Color(26, 16, 46), new Color(255, 60, 172), new Color(180, 244, 255), new Color(255, 150, 212)),
   OCEAN(new Color(64, 196, 255), new Color(6, 22, 40), new Color(10, 36, 62), new Color(64, 196, 255), new Color(208, 238, 255), new Color(132, 202, 255)),
   EMERALD(new Color(46, 220, 130), new Color(10, 28, 20), new Color(14, 44, 30), new Color(46, 220, 130), new Color(212, 255, 234), new Color(122, 232, 176)),
   ROSEGOLD(new Color(255, 158, 138), new Color(38, 24, 28), new Color(58, 34, 40), new Color(255, 158, 138), new Color(255, 230, 220), new Color(255, 192, 178)),
   LAVA(new Color(255, 94, 58), new Color(24, 12, 10), new Color(46, 18, 12), new Color(255, 94, 58), new Color(255, 222, 202), new Color(255, 152, 104)),
   ICE(new Color(150, 230, 255), new Color(16, 24, 32), new Color(22, 36, 48), new Color(150, 230, 255), new Color(236, 250, 255), new Color(192, 232, 252)),
   GOLD(new Color(255, 206, 84), new Color(28, 22, 10), new Color(46, 36, 14), new Color(255, 206, 84), new Color(255, 242, 210), new Color(255, 222, 150)),
   BLACK(new Color(120, 125, 135), new Color(10, 12, 14), new Color(18, 20, 24), new Color(45, 50, 55), new Color(255, 255, 255), new Color(170, 175, 180));

   private final Color main;
   private final Color bg;
   private final Color bg2;
   private final Color outline;
   private final Color text;
   private final Color text2;
   public Animation animation = new EaseInOutQuad(300, 1.0);

   private Theme(Color main, Color bg, Color bg2, Color outline, Color text, Color text2) {
      this.main = main;
      this.bg = bg;
      this.bg2 = bg2;
      this.outline = outline;
      this.text = text;
      this.text2 = text2;
   }

   public Color getMain() {
      return this.main;
   }

   public Color getBg() {
      return this.bg;
   }

   public Color getBg2() {
      return this.bg2;
   }

   public Color getOutline() {
      return this.outline;
   }

   public Color getText() {
      return this.text;
   }

   public Color getText2() {
      return this.text2;
   }

   public Animation getAnimation() {
      return this.animation;
   }
}
