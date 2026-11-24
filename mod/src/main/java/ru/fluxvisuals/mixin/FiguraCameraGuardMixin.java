package ru.fluxvisuals.mixin;

import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.figuramc.figura.lua.api.RendererAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 *_blocks Figura camera overrides for the local player while in first person.
 *
 * <p>Avatar scripts regularly move the camera to a head/hat pivot. That places the
 * camera inside the avatar model even when the model parts are hidden. Clearing
 * the overrides at the API level is deterministic and does not depend on Lua
 * event registration order.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(RendererAPI.class)
public abstract class FiguraCameraGuardMixin {
   @Shadow(remap = false)
   private UUID owner;

   @Inject(
         method = {"setCameraPos", "setCameraPivot", "setOffsetCameraPivot", "setCameraRot",
               "setOffsetCameraRot", "setEyeOffset", "setCameraMatrix", "setCameraNormal"},
         at = @At("HEAD"),
         cancellable = true,
         remap = false
   )
   private void fluxvisuals$blockCameraOverride(CallbackInfo ci) {
      if (!fluxvisuals$shouldClear()) {
         return;
      }
      ru.fluxvisuals.utils.cosmetics.FiguraCameraGuard.clear((RendererAPI) (Object) this);
      ci.cancel();
   }

   private boolean fluxvisuals$shouldClear() {
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc.player != null && mc.options != null
            && mc.options.getPerspective().isFirstPerson()
            && this.owner != null && this.owner.equals(mc.player.getUuid());
   }
}
