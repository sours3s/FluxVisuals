package ru.fluxvisuals.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.FramePass;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.render.EventRender3D;
import ru.fluxvisuals.event.render.WorldRenderEvent;
import ru.fluxvisuals.module.impl.visuals.DynamicLights;
import ru.fluxvisuals.module.impl.visuals.ShaderFog;
import ru.fluxvisuals.util.render.backends.gl.GlState;
import ru.fluxvisuals.util.render.capture.EntityFramebufferCaptureManager;

@Environment(EnvType.CLIENT)
@Mixin({ WorldRenderer.class })
public class WorldRendererMixin {
   @Shadow @Final private DefaultFramebufferSet framebufferSet;

   @Inject(method = { "render" }, at = { @At("HEAD") })
   private void beginEntityCapture(
         ObjectAllocator allocator,
         RenderTickCounter tickCounter,
         boolean renderBlockOutline,
         Camera camera,
         Matrix4f positionMatrix,
         Matrix4f basicProjectionMatrix,
         Matrix4f projectionMatrix,
         GpuBufferSlice fog,
         Vector4f fogColor,
         boolean shouldRenderSky,
         CallbackInfo ci) {
      EntityFramebufferCaptureManager.getInstance().beginFrame((WorldRenderer) (Object) this, tickCounter, camera);
   }

   @Inject(method = { "render" }, at = { @At("RETURN") })
   private void publishWorldRenderEvent(
         ObjectAllocator allocator,
         RenderTickCounter tickCounter,
         boolean renderBlockOutline,
         Camera camera,
         Matrix4f positionMatrix,
         Matrix4f basicProjectionMatrix,
         Matrix4f projectionMatrix,
         GpuBufferSlice fog,
         Vector4f fogColor,
         boolean shouldRenderSky,
         CallbackInfo ci) {
      MatrixStack stack = new MatrixStack();
      Matrix4f basePositionMatrix = new Matrix4f(positionMatrix);
      stack.multiplyPositionMatrix(new Matrix4f(basePositionMatrix));
      EventManager.call(new EventRender3D(stack, tickCounter.getTickProgress(true)));
      EntityFramebufferCaptureManager.getInstance().endFrame();
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null) {
         GameRenderer gameRenderer = client.gameRenderer;
         if (gameRenderer != null && camera != null) {
            GlState.Snapshot snapshot = GlState.push();
            ru.fluxvisuals.util.render.world.WorldRenderer worldRenderer = null;

            try {
               worldRenderer = ru.fluxvisuals.util.render.world.WorldRenderer.begin(client, tickCounter, camera,
                     positionMatrix, projectionMatrix);
               float frameDepth = worldRenderer.tickDelta();

               try {
                  EventManager.call(new WorldRenderEvent(client, gameRenderer, worldRenderer, frameDepth));
               } finally {
                  if (worldRenderer != null) {
                     try {
                        worldRenderer.flush();
                     } finally {
                        worldRenderer.close();
                     }
                  }
               }
            } finally {
               GlState.pop(snapshot);
            }
         }
      }
   }

   @Inject(
      method = {"getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;)I"},
      at = {@At("RETURN")}
   )
   private static void addDynamicLight2(BlockRenderView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
      addDynamicLightBoost(cir, pos);
   }

   @Inject(
      method = {"getLightmapCoordinates(Lnet/minecraft/client/render/WorldRenderer$BrightnessGetter;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I"},
      at = {@At("RETURN")}
   )
   private static void addDynamicLight(
      WorldRenderer.BrightnessGetter getter, BlockRenderView world, net.minecraft.block.BlockState state,
      BlockPos pos, CallbackInfoReturnable<Integer> cir
   ) {
      addDynamicLightBoost(cir, pos);
   }

   @Unique
   private static void addDynamicLightBoost(CallbackInfoReturnable<Integer> cir, BlockPos pos) {
      if (cir.getReturnValueI() == 0) {
         return;
      }
      if (FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
         return;
      }
      DynamicLights dl = FluxVisualsClient.get.manager.get(DynamicLights.class);
      if (dl == null || !dl.enable) {
         return;
      }
      int boost = dl.getBlockLightBoost(pos);
      if (boost <= 0) {
         return;
      }
      int packed = cir.getReturnValueI();
      int sky = LightmapTextureManager.getSkyLightCoordinates(packed);
      int block = Math.min(15, LightmapTextureManager.getBlockLightCoordinates(packed) + boost);
      cir.setReturnValue(LightmapTextureManager.pack(sky, block));
   }

   @Unique
   private static ShaderFog getShaderFog() {
      if (FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
         return null;
      }
      return FluxVisualsClient.get.manager.get(ShaderFog.class);
   }

   @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
   private void fluxvisuals$onRenderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, GpuBufferSlice fogBuffer, CallbackInfo ci) {
      ShaderFog fog = getShaderFog();
      if (fog == null || !fog.enable) {
         return;
      }
      FramePass framePass = frameGraphBuilder.createPass("shader_fog_sky");
      this.framebufferSet.mainFramebuffer = framePass.transfer(this.framebufferSet.mainFramebuffer);
      framePass.setRenderer(() -> {
         RenderSystem.setShaderFog(fogBuffer);
         fog.renderShader();
      });
      ci.cancel();
   }

   @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
   private void fluxvisuals$onRenderClouds(FrameGraphBuilder frameGraphBuilder, CloudRenderMode mode, Vec3d cameraPos, long l, float f, int i, float g, CallbackInfo ci) {
      ShaderFog fog = getShaderFog();
      if (fog != null && fog.enable) {
         ci.cancel();
      }
   }
}
