package ru.fluxvisuals.utils.cosmetics.bbmodel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Player state used to choose a built-in .bbmodel animation. */
@Environment(EnvType.CLIENT)
public record BbPoseState(boolean moving, boolean sprinting, boolean crouching, boolean gliding,
      boolean flying, boolean swimming, boolean usingItem, boolean attacking, boolean blinking,
      float speed) {
   public static final BbPoseState IDLE = new BbPoseState(false, false, false, false, false, false,
         false, false, false, 0.0F);
}
