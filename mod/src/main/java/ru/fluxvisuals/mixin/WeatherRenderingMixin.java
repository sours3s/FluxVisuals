package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.NoRender;

@Environment(EnvType.CLIENT)
@Mixin(WeatherRendering.class)
public class WeatherRenderingMixin {
    @Inject(
        method = "renderPrecipitation",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRenderPrecipitation(VertexConsumerProvider vertexConsumers, Vec3d cameraPos, WeatherRenderState state, CallbackInfo ci) {
        if (NoRender.getInstance().enable && NoRender.elements.get("Rain")) {
            ci.cancel();
        }
    }

    @Inject(
        method = "addParticlesAndSound",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onAddParticlesAndSound(ClientWorld world, Camera camera, int weatherRadius, ParticlesMode particlesMode, int tick, CallbackInfo ci) {
        if (NoRender.getInstance().enable && NoRender.elements.get("Rain")) {
            ci.cancel();
        }
    }
}
