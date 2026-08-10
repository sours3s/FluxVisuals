package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fluxvisuals.module.impl.visuals.NoRender;

@Environment(EnvType.CLIENT)
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(
        method = "renderFireOverlay",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onRenderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
        if (NoRender.getInstance().enable && NoRender.elements.get("Fire")) {
            ci.cancel();
        }
    }

    @Inject(
        method = "renderUnderwaterOverlay",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onRenderUnderwaterOverlay(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (NoRender.getInstance().enable && NoRender.elements.get("Water")) {
            ci.cancel();
        }
    }

    @Inject(
        method = "getInWallBlockState",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onGetInWallBlockState(PlayerEntity player, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null && NoRender.getInstance().enable) {
            if (NoRender.elements.get("Block Overlay")) {
                cir.setReturnValue(null);
            } else {
                Block block = state.getBlock();
                if ((block == Blocks.LAVA && NoRender.elements.get("Lava")) ||
                    (block == Blocks.WATER && NoRender.elements.get("Water"))) {
                    cir.setReturnValue(null);
                }
            }
        }
    }
}
