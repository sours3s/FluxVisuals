package ru.fluxvisuals.module.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.util.render.animation.util.Animation;

@Environment(EnvType.CLIENT)
public enum Category {
   Visuals("Visuals", "e"),
   Utils("Utils", "h"),
   Configs("Configs", "c"),
   Friends("Friends", "d");

   private final String name;
   private final String icon;
   public Animation anim33 = new Animation();
   public Animation anim44 = new Animation();

   private Category(String name, String icon) {
      this.name = name;
      this.icon = icon;
   }

   public String getIcon() {
      return this.icon;
   }

   public String getName() {
      return this.name;
   }
}
