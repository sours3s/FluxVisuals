package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.fog.LavaFogModifier;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fluxvisuals.module.impl.visuals.NoRender;

@Environment(EnvType.CLIENT)
@Mixin(LavaFogModifier.class)
public class LavaFogModifierMixin {
    @Inject(
        method = "shouldApply",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onShouldApply(CameraSubmersionType submersionType, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (NoRender.getInstance().enable && NoRender.elements.get("Lava")) {
            cir.setReturnValue(false);
        }
    }
}
