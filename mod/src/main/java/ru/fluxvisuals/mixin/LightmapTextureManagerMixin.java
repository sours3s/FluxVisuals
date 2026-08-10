package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.visuals.Gamma;

@Environment(EnvType.CLIENT)
@Mixin({LightmapTextureManager.class})
public class LightmapTextureManagerMixin {
   @Redirect(
      method = {"update"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/lang/Double;floatValue()F",
         ordinal = 1
      )
   )
   private float fluxvisuals$getGammaValue(Double instance) {
      if (FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null) {
         Gamma module = FluxVisualsClient.get.manager.get(Gamma.class);
         if (module != null && module.enable && module.mode.is("Гамма")) {
            return 200.0F;
         }
      }

      return instance.floatValue();
   }

   @org.spongepowered.asm.mixin.injection.Inject(
      method = "getDarkness",
      at = @org.spongepowered.asm.mixin.injection.At("HEAD"),
      cancellable = true
   )
   private void onGetDarkness(net.minecraft.entity.LivingEntity entity, float factor, float tickProgress, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
      if (ru.fluxvisuals.module.impl.visuals.NoRender.getInstance().enable && ru.fluxvisuals.module.impl.visuals.NoRender.elements.get("Warden Darkness")) {
         cir.setReturnValue(0.0f);
      }
   }
}
