package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.visuals.FirstPersonModel;

/**
 * EntityRenderManagerMixin — заставляет рендерить тело локального игрока
 * в первом лице (First Person Model). Тело рендерится на позиции камеры
 * ванильным пайплайном — это стандартный подход FPM-модов.
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityRenderManager.class)
public class EntityRenderManagerMixin {

   @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
   private void onShouldRender(Entity entity, Frustum frustum, double x, double y, double z,
                               CallbackInfoReturnable<Boolean> cir) {
      FirstPersonModel module = null;
      if (FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null) {
         module = FluxVisualsClient.get.manager.get(FirstPersonModel.class);
      }
      if (module == null || !module.shouldRenderBody()) return;

      MinecraftClient mc = MinecraftClient.getInstance();
      if (entity == mc.player) {
         // Force the local player body to render in first person
         cir.setReturnValue(true);
      }
   }
}
