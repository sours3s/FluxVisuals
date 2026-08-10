package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.NoRender;

@Environment(EnvType.CLIENT)
@Mixin(ArmorStandEntityRenderer.class)
public class ArmorStandEntityRendererMixin {
    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/ArmorStandEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRender(ArmorStandEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (NoRender.getInstance().enable && NoRender.elements.get("Armor Stands")) {
            ci.cancel();
        }
    }
}
