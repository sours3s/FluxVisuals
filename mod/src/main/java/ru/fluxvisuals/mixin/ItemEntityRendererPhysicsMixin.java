package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.fluxvisuals.module.impl.visuals.ItemPhysics;

/**
 * Отключает ванильное вращение выброшенного предмета, пока включён Item Physics.
 * Плоское положение предмету задаёт {@link ItemRendererPhysicsMixin}.
 */
@Environment(EnvType.CLIENT)
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererPhysicsMixin {
   @Redirect(
         require = 0,
         method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
         at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionfc;)V")
   )
   private void fluxvisuals$itemPhysicsRotation(MatrixStack instance, Quaternionfc quaternion) {
      ItemPhysics module = ItemPhysics.getInstance();
      if (module == null || !module.enable) {
         instance.multiply(quaternion);
      }
   }
}
