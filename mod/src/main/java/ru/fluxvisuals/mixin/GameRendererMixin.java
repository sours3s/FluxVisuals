package ru.fluxvisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.ProjectionMatrix3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fluxvisuals.module.impl.visuals.AspectRation;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
   @Shadow
   public abstract float getFarPlaneDistance();

   @Inject(method = "tick", at = @At("HEAD"))
   private void onTick(CallbackInfo ci) {
      // Логика обновления каждый тик
   }

   /**
    * Подменяем базовую проекцию, чтобы модуль Aspect Ratio влиял и на мир, и на руки.
    * Vanilla: new Matrix4f().perspective(fov * 0.017453292F, width / height, 0.05F, far).
    */
   @Inject(method = "getBasicProjectionMatrix", at = @At("HEAD"), cancellable = true)
   private void fluxvisuals$aspectRatio(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
      float delta = AspectRation.getAspectRation();
      if (delta == 0.0F || MinecraftClient.getInstance() == null) {
         return;
      }
      var window = MinecraftClient.getInstance().getWindow();
      if (window == null || window.getFramebufferHeight() <= 0) {
         return;
      }
      float aspect = (float) window.getFramebufferWidth() / (float) window.getFramebufferHeight() + delta;
      if (!Float.isFinite(aspect) || aspect <= 0.0F) {
         return;
      }
      cir.setReturnValue(new Matrix4f().perspective(
            fovDegrees * ((float) Math.PI / 180.0F), aspect, 0.05F, this.getFarPlaneDistance()));
   }

   /** Руки/предметы используют отдельную проекцию — корректируем и её. */
   @WrapOperation(
         method = "renderWorld",
         at = @At(value = "INVOKE",
               target = "Lnet/minecraft/client/render/ProjectionMatrix3;set(IIF)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;")
   )
   private GpuBufferSlice fluxvisuals$aspectRatioHands(ProjectionMatrix3 instance, int width, int height,
         float fov, Operation<GpuBufferSlice> original) {
      float target = AspectRation.getTargetAspect();
      if (target > 0.0F && height > 0) {
         width = Math.max(1, Math.round(height * target));
      }
      return original.call(instance, width, height, fov);
   }
}
