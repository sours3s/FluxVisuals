package ru.fluxvisuals.mixin;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.impl.EventChangeWorld;
import ru.fluxvisuals.event.impl.EventUpdate;
import ru.fluxvisuals.event.lifecycle.ClientTickEvent;
import ru.fluxvisuals.event.player.EventMotion;

@Environment(EnvType.CLIENT)
@Mixin({ MinecraftClient.class })
public abstract class MinecraftClientMixin {
   @Shadow public abstract Window getWindow();

   @Inject(method = "<init>", at = @At("TAIL"))
   private void fluxvisuals$initClient(CallbackInfo ci) {
      if (ru.fluxvisuals.Client.RENDERER == null) {
         ru.fluxvisuals.Client.init();
      }
   }

   @Inject(method = { "tick" }, at = { @At("HEAD") })
   private void initRenderer(CallbackInfo ci) {
      if (FluxVisualsClient.isModInitialized()) {
         FluxVisualsClient.ensureRendererInitialized();
      }
   }

   @Inject(method = { "tick" }, at = { @At("HEAD") })
   private void fluxvisuals$motionBlurTick(CallbackInfo ci) {
      ru.fluxvisuals.module.impl.visuals.MotionBlur.tick();
   }

   @Inject(
      method = "reloadResources(ZLnet/minecraft/client/MinecraftClient$LoadingContext;)Ljava/util/concurrent/CompletableFuture;",
      at = @At("HEAD")
   )
   private void fluxvisuals$onResourceReloadStart(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
      ru.fluxvisuals.module.impl.visuals.MotionBlur.onReloadStart();
   }

   @Inject(
      method = "reloadResources(ZLnet/minecraft/client/MinecraftClient$LoadingContext;)Ljava/util/concurrent/CompletableFuture;",
      at = @At("RETURN")
   )
   private void fluxvisuals$onResourceReloadReturn(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
      CompletableFuture<Void> future = cir.getReturnValue();
      if (future != null) {
         future.whenComplete((value, throwable) -> ru.fluxvisuals.module.impl.visuals.MotionBlur.onReloadComplete());
      } else {
         ru.fluxvisuals.module.impl.visuals.MotionBlur.onReloadComplete();
      }
   }

   @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;blitToScreen()V", shift = At.Shift.AFTER))
   private void fluxvisuals$onRender(CallbackInfo ci) {
      if (ru.fluxvisuals.Client.RENDERER != null) {
         ru.fluxvisuals.Client.RENDERER.prepare();
         ru.fluxvisuals.Client.RENDERER.render();
      }
   }

   @Inject(method = { "tick" }, at = { @At("TAIL") })
   private void publishClientTick(CallbackInfo ci) {
      if (FluxVisualsClient.isModInitialized()) {
         MinecraftClient client = (MinecraftClient) (Object) this;
         if (!client.isPaused()) {
            ClientPlayerEntity player = client.player;
            ClientWorld world = client.world;
            if (player != null && world != null) {
               EventManager.call(new ClientTickEvent(client));
               EventManager.call(new EventUpdate());
               EventManager.call(new EventMotion(player.getYaw(), player.getPitch(), player.getX(), player.getY(),
                     player.getZ(), player.isOnGround(), player.getBoundingBox(), () -> {
                     }));
            }
         }
      }
   }

   @Inject(method = { "joinWorld" }, at = { @At("TAIL") })
   public void loadWorld(CallbackInfo ci) {
      EventManager.call(new EventChangeWorld());
   }
}
