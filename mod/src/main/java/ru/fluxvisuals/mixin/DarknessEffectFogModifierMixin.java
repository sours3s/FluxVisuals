package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.fog.DarknessEffectFogModifier;
import net.minecraft.client.render.fog.StatusEffectFogModifier;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fluxvisuals.module.impl.visuals.NoRender;

@Environment(EnvType.CLIENT)
@Mixin(StatusEffectFogModifier.class)
public class DarknessEffectFogModifierMixin {
    @Inject(
        method = "shouldApply",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onShouldApply(CameraSubmersionType submersionType, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof DarknessEffectFogModifier && NoRender.getInstance().enable && NoRender.elements.get("Warden Darkness")) {
            cir.setReturnValue(false);
        }
    }
}
