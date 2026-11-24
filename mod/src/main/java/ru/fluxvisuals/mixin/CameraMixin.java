package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.player.EventRotation;
import ru.fluxvisuals.event.render.CameraPositionEvent;
import ru.fluxvisuals.module.impl.visuals.SmoothCamera;
import ru.fluxvisuals.module.impl.visuals.CameraOverhaul;

@Environment(EnvType.CLIENT)
@Mixin({Camera.class})
public abstract class CameraMixin {
   @Unique
   private EventRotation fluxvisuals$rotationEvent;
   @Unique
   private float fluxvisuals$originalYaw;
   @Unique
   private float fluxvisuals$originalPitch;
   @Unique
   private float fluxvisuals$smoothYaw;
   @Unique
   private float fluxvisuals$smoothPitch;
   @Unique
   private boolean fluxvisuals$smoothInitialized;
   @Unique
   private long fluxvisuals$lastTimeNanos;

   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Shadow
   public abstract Vec3d getCameraPos();

   @Inject(
      method = {"update"},
      at = {@At("HEAD")}
   )
   private void onUpdateHead(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
      if (focusedEntity != null) {
         this.fluxvisuals$originalYaw = focusedEntity.getYaw(tickProgress);
         this.fluxvisuals$originalPitch = focusedEntity.getPitch(tickProgress);
         this.fluxvisuals$rotationEvent = new EventRotation(this.fluxvisuals$originalYaw, this.fluxvisuals$originalPitch, tickProgress);
         EventManager.call(this.fluxvisuals$rotationEvent);
      } else {
         this.fluxvisuals$rotationEvent = null;
      }
   }

   @Redirect(
      method = {"update"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
      )
   )
   private void redirectSetRotation(Camera instance, float yaw, float pitch) {
      SmoothCamera smoothCamera = SmoothCamera.getInstance();
      if (smoothCamera != null && smoothCamera.enable
            && MinecraftClient.getInstance().options.getPerspective() == Perspective.FIRST_PERSON) {
         this.applySmoothCamera(instance, smoothCamera);
         return;
      }
      if (this.fluxvisuals$rotationEvent == null
         || this.fluxvisuals$rotationEvent.getYaw() == this.fluxvisuals$originalYaw && this.fluxvisuals$rotationEvent.getPitch() == this.fluxvisuals$originalPitch) {
         this.setRotation(yaw, pitch);
      } else {
         this.setRotation(this.fluxvisuals$rotationEvent.getYaw(), this.fluxvisuals$rotationEvent.getPitch());
      }
   }

   @Unique
   private void applySmoothCamera(Camera instance, SmoothCamera smoothCamera) {
      Entity focusedEntity = instance.getFocusedEntity();
      if (focusedEntity == null) {
         return;
      }
      float targetYaw = focusedEntity.getYaw();
      float targetPitch = focusedEntity.getPitch();
      if (!this.fluxvisuals$smoothInitialized) {
         this.fluxvisuals$smoothYaw = targetYaw;
         this.fluxvisuals$smoothPitch = targetPitch;
         this.fluxvisuals$lastTimeNanos = System.nanoTime();
         this.fluxvisuals$smoothInitialized = true;
         this.setRotation(this.fluxvisuals$smoothYaw, this.fluxvisuals$smoothPitch);
         return;
      }
      long now = System.nanoTime();
      float dt = Math.min((float)(now - this.fluxvisuals$lastTimeNanos) / 1.0E9F, 0.1F);
      this.fluxvisuals$lastTimeNanos = now;
      float speed = smoothCamera.getDelayValue();
      float factor = 1.0F - (float)Math.exp(-speed * dt);
      float dyaw = CameraMixin.wrapDegrees(targetYaw - this.fluxvisuals$smoothYaw);
      this.fluxvisuals$smoothYaw += dyaw * factor;
      this.fluxvisuals$smoothPitch += (targetPitch - this.fluxvisuals$smoothPitch) * factor;
      this.setRotation(this.fluxvisuals$smoothYaw, this.fluxvisuals$smoothPitch);
   }

   @Unique
   private static float wrapDegrees(float deg) {
      deg %= 360.0F;
      if (deg >= 180.0F) {
         deg -= 360.0F;
      }
      if (deg < -180.0F) {
         deg += 360.0F;
      }
      return deg;
   }

   @Inject(
      method = {"update"},
      at = {@At("RETURN")}
   )
   private void onUpdateReturn(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
      CameraPositionEvent positionEvent = new CameraPositionEvent(this.getCameraPos(), tickProgress);
      EventManager.call(positionEvent);
      Vec3d position = positionEvent.getPosition();
      if (position != null) {
         ((CameraAccessor)this).invokeSetPos(position.x, position.y, position.z);
      }
      this.fluxvisuals$rotationEvent = null;

      // CameraOverhaul: apply pitch/yaw sway based on movement
      if (focusedEntity != null && MinecraftClient.getInstance().options.getPerspective() == Perspective.FIRST_PERSON) {
         CameraOverhaul overhaul = ru.fluxvisuals.client.FluxVisualsClient.get.manager != null
            ? ru.fluxvisuals.client.FluxVisualsClient.get.manager.get(CameraOverhaul.class) : null;
         if (overhaul != null && overhaul.enable) {
            float currentYaw = this.fluxvisuals$smoothInitialized ? this.fluxvisuals$smoothYaw : focusedEntity.getYaw(tickProgress);
            float currentPitch = this.fluxvisuals$smoothInitialized ? this.fluxvisuals$smoothPitch : focusedEntity.getPitch(tickProgress);
            float yawOffset = overhaul.getYawOffset();
            float pitchOffset = overhaul.getPitchOffset();
            if (Math.abs(yawOffset) > 0.001F || Math.abs(pitchOffset) > 0.001F) {
               this.setRotation(currentYaw + yawOffset, currentPitch + pitchOffset);
            }
         }
      }
   }
}
