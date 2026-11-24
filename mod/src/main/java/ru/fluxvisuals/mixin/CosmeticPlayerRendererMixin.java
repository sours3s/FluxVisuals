package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.model.Model;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.fluxvisuals.utils.cosmetics.BbCosmeticManager;
import ru.fluxvisuals.utils.cosmetics.CosmeticBridge;

/** Replaces the local player's vanilla model with the built-in .bbmodel avatar. */
@Environment(EnvType.CLIENT)
@Mixin(net.minecraft.client.render.entity.LivingEntityRenderer.class)
public abstract class CosmeticPlayerRendererMixin {
   @Redirect(
      method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
      )
   )
   @SuppressWarnings({ "rawtypes", "unchecked" })
   private void fluxvisuals$submitCosmeticModel(OrderedRenderCommandQueue queue, Model model, Object renderState,
         MatrixStack matrices, RenderLayer layer, int light, int overlay, int mixColor, Sprite sprite,
         int outlineColor, ModelCommandRenderer.CrumblingOverlayCommand crumbling) {
      if (renderState instanceof PlayerEntityRenderState playerState
            && CosmeticBridge.useBbModel()
            && MinecraftClient.getInstance().player != null
            && playerState.id == MinecraftClient.getInstance().player.getId()) {
         BbCosmeticManager.renderAvatar(matrices, queue, light, overlay);
         if (BbCosmeticManager.isAvatarActive()) {
            return;
         }
      }
      queue.submitModel(model, renderState, matrices, layer, light, overlay, mixColor, sprite, outlineColor, crumbling);
   }
}
