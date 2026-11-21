package ru.fluxvisuals.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // Получаем экземпляр InGameHud
        InGameHud hud = (InGameHud)(Object)this;

        // Здесь можно добавить вашу логику рендеринга HUD
        // Например, рендеринг кастомных элементов интерфейса
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(boolean tick, CallbackInfo ci) {
        // Получаем экземпляр InGameHud
        InGameHud hud = (InGameHud)(Object)this;

        // Здесь можно добавить логику обновления HUD каждый тик
        // Например, обновление кастомных элементов интерфейса
    }
}