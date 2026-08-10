package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.impl.EventFirework;

@Environment(EnvType.CLIENT)
@Mixin({ FireworkRocketEntity.class })
public class FireworkRocketEntityMixin {
   @Shadow
   private LivingEntity shooter;

   @ModifyConstant(method = { "tick" }, constant = { @Constant(doubleValue = 1.5) })
   private double fluxvisuals$modifyBoost(double original) {
      if (this.shooter == null) {
         return original;
      }

      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         return original;
      }

      EventFirework event = new EventFirework(this.shooter, (float) original);
      EventManager.call(event);
      return event.getSpeed();
   }
}
