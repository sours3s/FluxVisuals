package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.utils.HitDelayFix;

/**
 * Миксин для HitDelayFix: возвращает максимальный прогресс атаки,
 * когда модуль включён, убирая клиент-сайд кулдаун.
 */
@Environment(EnvType.CLIENT)
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

   @Inject(method = "getAttackCooldownProgress", at = @At("HEAD"), cancellable = true)
   private void onGetAttackCooldownProgress(float adjustTicks, CallbackInfoReturnable<Float> cir) {
      if (FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) return;
      HitDelayFix fix = FluxVisualsClient.get.manager.get(HitDelayFix.class);
      if (fix != null && fix.enable) {
         cir.setReturnValue(1.0F);
      }
   }
}
