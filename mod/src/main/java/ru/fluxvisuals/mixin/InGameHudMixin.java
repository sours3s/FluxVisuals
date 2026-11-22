package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.fluxgui.util.render.FluxGlassPipeline;
import ru.fluxvisuals.util.render.HudRenderBridge;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class InGameHudMixin {
    /**
     * Снимок кадра для liquid-glass делается ДО ванильного HUD: стекло должно
     * размывать мир, а не наложенные друг на друга панели.
     */
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void onRenderHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Module hudModule = FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null
              ? FluxVisualsClient.get.manager.getModule(Hud.class)
              : null;
        boolean liquidGlass = FluxVisualsClient.get != null
              && ru.fluxvisuals.ui.fluxgui.module.render.Hud.isLiquidGlassStyle();
        if (FluxVisualsClient.isModInitialized() && hudModule != null && hudModule.enable
              && (liquidGlass || Hud.blur.get("HUD"))) {
            FluxGlassPipeline.captureHudFrame();
        }
    }

    /** Кастомный хотбар рисуется FluxVisuals, поэтому ванильный нужно спрятать. */
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (FluxVisualsClient.isModInitialized() && Hud.isHotbarBindsActive()) {
            ci.cancel();
        }
    }

    /**
     * Обычный ингейм-рендер HUD. ChatScreenMixin рисует оверлей поверх открытого чата,
     * поэтому здесь чат пропускаем, чтобы не дублировать кадр и тики анимаций.
     */
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        if (client.currentScreen instanceof ChatScreen) {
            return;
        }
        HudRenderBridge.renderHudOverlay(context, tickCounter);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(boolean tick, CallbackInfo ci) {
    }
}
