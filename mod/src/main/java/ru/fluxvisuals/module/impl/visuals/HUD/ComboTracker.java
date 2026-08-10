package ru.fluxvisuals.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;

/**
 * Трекер комбо: считает подряд идущие удары по одному и тому же таргету.
 * Сброс при смене цели или таймауте COMBO_TIMEOUT_MS.
 */
@Environment(EnvType.CLIENT)
public final class ComboTracker {
   public static final long COMBO_TIMEOUT_MS = 3000L;

   private LivingEntity currentTarget = null;
   private int comboCount = 0;
   private long lastHitTime = 0L;

   /** Вызывается при каждом ударе по сущности. */
   public void onHit(LivingEntity target) {
      long now = System.currentTimeMillis();
      if (target == currentTarget && (now - lastHitTime) < COMBO_TIMEOUT_MS) {
         comboCount++;
      } else if (target == currentTarget) {
         comboCount = 1;
      } else {
         currentTarget = target;
         comboCount = 1;
      }
      lastHitTime = now;
   }

   /** Текущее значение комбо (0 если таргет не активен). */
   public int getCombo() {
      if (System.currentTimeMillis() - lastHitTime > COMBO_TIMEOUT_MS) {
         comboCount = 0;
         currentTarget = null;
      }
      return comboCount;
   }

   /** Текущий таргет (для отображения HP). */
   public LivingEntity getTarget() {
      if (System.currentTimeMillis() - lastHitTime > COMBO_TIMEOUT_MS) {
         currentTarget = null;
      }
      return currentTarget;
   }

   public void reset() {
      comboCount = 0;
      currentTarget = null;
      lastHitTime = 0L;
   }
}
