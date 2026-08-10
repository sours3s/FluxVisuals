package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.utils.LagDetector;
import ru.fluxvisuals.module.impl.utils.SoundFX;
import ru.fluxvisuals.module.impl.visuals.BetterWorld;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
   @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"), cancellable = true)
   private void onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
      BetterWorld betterWorld = BetterWorld.getInstance();
      if (betterWorld != null && betterWorld.enable && betterWorld.changeTime.get()) {
         ci.cancel();
      }
      LagDetector.onWorldTimePacket(packet);
   }

   @Inject(method = "onEntityStatus", at = @At("HEAD"))
   private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
      SoundFX.handleEntityStatus(packet);
   }
}
