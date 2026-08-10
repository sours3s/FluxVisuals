package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.config.friend.FriendManager;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.player.AttackEvent;
import ru.fluxvisuals.module.impl.visuals.Friends;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
   @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
   private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
      EventManager.call(new AttackEvent(target));
      Friends friends = ru.fluxvisuals.client.FluxVisualsClient.get.manager != null
         ? ru.fluxvisuals.client.FluxVisualsClient.get.manager.get(Friends.class)
         : null;
      if (friends != null && friends.enable && friends.friendlyFire.get()) {
         if (target instanceof PlayerEntity targetPlayer) {
            if (FriendManager.isFriend(targetPlayer.getName().getString())) {
               ci.cancel(); // Отменяем атаку на друга
            }
         }
      }
   }
}
