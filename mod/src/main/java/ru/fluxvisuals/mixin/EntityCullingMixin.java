package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.visuals.EntityCulling;

/**
 * EntityCullingMixin — скрывает сущности, невидимые за блоками (raycast).
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityRenderer.class)
public class EntityCullingMixin<T extends Entity> {

   @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
   private void onShouldRender(T entity, net.minecraft.client.render.Frustum frustum,
                                double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      EntityCulling module = null;
      if (FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null) {
         module = FluxVisualsClient.get.manager.get(EntityCulling.class);
      }
      if (module == null || !module.enable) return;

      // Don't skip if shouldRender returns true (entity is visible)
      if (!module.shouldRender(entity)) {
         cir.setReturnValue(false);
      }
   }
}
