package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.visuals.NameTags;
import ru.fluxvisuals.util.render.capture.EntityFramebufferCaptureManager;

@Environment(EnvType.CLIENT)
@Mixin({EntityRenderer.class})
public abstract class EntityRendererMixin<S extends EntityRenderState> {

   /**
    * Cancel vanilla nametag rendering for any entity type that the custom NameTags
    * module is currently drawing, so the player only sees one tag.
    */
   @Inject(
      method = {"renderLabelIfPresent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderLabelIfPresent(
         S state,
         MatrixStack matrices,
         OrderedRenderCommandQueue queue,
         CameraRenderState cameraRenderState,
         CallbackInfo ci) {
      if (FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
         return;
      }
      NameTags nameTags = FluxVisualsClient.get.manager.get(NameTags.class);
      if (nameTags == null || !nameTags.enable) {
         return;
      }

      if (state instanceof PlayerEntityRenderState && nameTags.entityType.get("Player")) {
         ci.cancel();
         return;
      }
      if (state instanceof ItemEntityRenderState && nameTags.entityType.get("Item")) {
         ci.cancel();
         return;
      }
      if (state instanceof LivingEntityRenderState
            && !(state instanceof PlayerEntityRenderState)
            && nameTags.entityType.get("Mobs")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderLabelIfPresent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void skipLabelDuringCapture(
         EntityRenderState state,
         MatrixStack matrices,
         OrderedRenderCommandQueue queue,
         CameraRenderState cameraRenderState,
         CallbackInfo ci) {
      if (EntityFramebufferCaptureManager.getInstance().isExecutingCapturePass()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"updateRenderState"},
      at = {@At("RETURN")}
   )
   private void onUpdateRenderState(net.minecraft.entity.Entity entity, S state, float tickProgress, CallbackInfo ci) {
      if (ru.fluxvisuals.module.impl.visuals.NoRender.getInstance().enable
            && ru.fluxvisuals.module.impl.visuals.NoRender.elements.get("Fire")) {
         state.onFire = false;
      }
   }
}
