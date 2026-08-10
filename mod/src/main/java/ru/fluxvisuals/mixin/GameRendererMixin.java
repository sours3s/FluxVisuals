package ru.fluxvisuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.visuals.AspectRation;
import ru.fluxvisuals.module.impl.visuals.CameraCustomer;
import ru.fluxvisuals.module.impl.visuals.HUD.InformationHUD;
import ru.fluxvisuals.util.other.Mathf;
import ru.fluxvisuals.module.impl.visuals.CustomWorld;
import ru.fluxvisuals.util.render.math.animation.AnimationMath;

@Environment(EnvType.CLIENT)
@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {
   @Shadow
   @Final
   public abstract float getFarPlaneDistance();

   @Shadow
   public abstract float getSkyDarkness(float var1);

   @Inject(
      method = {"getBasicProjectionMatrix"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getBasicProjectionMatrix(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
      Matrix4f matrix4f = new Matrix4f();
      cir.cancel();

      float aspect = (float)MinecraftClient.getInstance().getWindow().getFramebufferWidth() / MinecraftClient.getInstance().getWindow().getFramebufferHeight();

      // Apply AspectRatio / CameraCustomer aspect
      // Модификатор — это ОТНОШЕНИЕ (текущий аспект / целевой аспект), поэтому его надо
      // УМНОЖАТЬ на текущий aspect, а не прибавлять. 0.0F = модуль выключен (без изменений).
      float aspectMod = AspectRation.getAspectRation();
      if (aspectMod == 0.0F) {
         aspectMod = CameraCustomer.getAspectRatio();
      }
      if (aspectMod > 0.0F) {
         aspect *= aspectMod;
      }

      // Apply zoom scaling from CameraCustomer
      CameraCustomer camMod = FluxVisualsClient.get.manager.get(CameraCustomer.class);
      float finalFov = fovDegrees;
      if (camMod != null && camMod.enable) {
         double zoom = camMod.getCurrentZoom();
         if (zoom != 1.0) {
            finalFov = (float) (fovDegrees / zoom);
         }
         // Apply custom FOV if enabled
         if (camMod.fovEnabled.get()) {
            if (camMod.fovSmooth.get()) {
               finalFov = AnimationMath.animation(finalFov, camMod.fovValue.get(), 0.1F);
            } else {
               finalFov = camMod.fovValue.get();
            }
         }
      }

      cir.setReturnValue(matrix4f.perspective(finalFov * (float) (Math.PI / 180.0), aspect, 0.05F, this.getFarPlaneDistance()));
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/memory/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
         shift = Shift.AFTER
      )}
   )
   private void renderWorld(RenderTickCounter renderTickCounter, CallbackInfo ci) {
      if (InformationHUD.mc.player != null && InformationHUD.mc.world != null) {
         Camera camera = InformationHUD.mc.gameRenderer.getCamera();
         MatrixStack matrixStack = new MatrixStack();
         RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.peek().getPositionMatrix());
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
         float tickDelta = InformationHUD.mc.getRenderTickCounter().getTickProgress(true);
         float fov = ((GameRendererAccessor)InformationHUD.mc.gameRenderer).invokeGetFov(camera, tickDelta, true);
         Mathf.lastProjMat.set(InformationHUD.mc.gameRenderer.getBasicProjectionMatrix(fov));
         Mathf.lastModMat.set(RenderSystem.getModelViewMatrix());
         Mathf.lastWorldSpaceMatrix.set(matrixStack.peek().getPositionMatrix());
         RenderSystem.getModelViewStack().popMatrix();
      }
   }

}
