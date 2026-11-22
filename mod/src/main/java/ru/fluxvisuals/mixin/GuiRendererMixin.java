package ru.fluxvisuals.mixin;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.ui.fluxgui.client.gui.hud.HudItemFilter;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @ModifyArg(
        method = "prepareItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/state/TexturedQuadGuiElementRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/texture/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/ScreenRect;Lnet/minecraft/client/gui/ScreenRect;)V"
        ),
        index = 11
    )
    private int fadeHudItem(int color) {
        return HudItemFilter.preparedColor(color);
    }

    @Inject(method = "prepareItem", at = @At("HEAD"))
    private void beginHudItemFilter(ItemGuiElementRenderState state, float x, float y, int atlasX, int atlasY, CallbackInfo ci) {
        HudItemFilter.beginPreparing(state);
    }

    @Inject(method = "prepareItem", at = @At("RETURN"))
    private void endHudItemFilter(ItemGuiElementRenderState state, float x, float y, int atlasX, int atlasY, CallbackInfo ci) {
        HudItemFilter.endPreparing();
    }
}
