package ru.fluxvisuals.mixin;

import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.ui.fluxgui.client.gui.hud.HudItemFilter;

@Mixin(GuiRenderState.class)
public class GuiRenderStateMixin {
    @Inject(method = "addItem", at = @At("HEAD"))
    private void captureHudItem(ItemGuiElementRenderState state, CallbackInfo ci) {
        HudItemFilter.capture(state);
    }
}
