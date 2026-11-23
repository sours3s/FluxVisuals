package ru.fluxvisuals.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import ru.fluxvisuals.ui.fluxgui.client.gui.hud.HudItemFilter;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        // Сбрасываем состояние фильтра в начале кадра: инвентарь/креатив не вызывают
        // Hud.render(), поэтому без этого прозрачность HUD может «залипнуть» на иконках.
        HudItemFilter.clearFrame();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // Логика обновления каждый тик
    }
}