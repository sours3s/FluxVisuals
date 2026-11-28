package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/** Эталонная анимация списка игроков: форсированный показ и сдвиг вниз. */
@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class BetterMinecraftTabMixin {

    @Redirect(
       method = "renderPlayerList",
       at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"),
       require = 0
    )
    private boolean fluxvisuals$forceTabDuringAnim(KeyBinding keyBinding) {
       BetterMinecraft mod = BetterMinecraft.INSTANCE;
       if (mod.isEnabled() && mod.tabAnim.get() && mod.getTabAnim() > 0.001F) {
          return true;
       }
       return keyBinding.isPressed();
    }

    @Inject(method = "renderPlayerList", at = @At("HEAD"), cancellable = true)
    private void fluxvisuals$tabHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
       BetterMinecraft mod = BetterMinecraft.INSTANCE;
       if (!mod.isEnabled() || !mod.tabAnim.get()) return;

       float t = mod.getTabAnim();
       if (t <= 0.001F) {
          ci.cancel();
          return;
       }
       float offsetY = -60F * (1F - t);
       context.getMatrices().pushMatrix();
       context.getMatrices().translate(0, offsetY);
    }

    @Inject(method = "renderPlayerList", at = @At("TAIL"))
    private void fluxvisuals$tabTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
       BetterMinecraft mod = BetterMinecraft.INSTANCE;
       if (!mod.isEnabled() || !mod.tabAnim.get()) return;
       if (mod.getTabAnim() <= 0.001F) return;

       context.getMatrices().popMatrix();
    }
}
