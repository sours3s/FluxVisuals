package ru.fluxvisuals.mixin.accessor;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.PostEffectPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Environment(EnvType.CLIENT)
@Mixin(PostEffectPass.class)
public interface PostEffectPassAccessor {
    @Accessor("uniformBuffers")
    Map<String, GpuBuffer> fluxvisuals$uniformBuffers();
}
