package ru.fluxvisuals.mixin.accessor;

import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractTexture.class)
public interface AbstractTextureAccessor {
    @Accessor("sampler")
    void fluxvisuals$setSampler(GpuSampler sampler);
}
