package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/** Эталонная анимация открытия контейнеров. */
@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class BetterMinecraftHandleScreenMixin extends Screen {

    protected BetterMinecraftHandleScreenMixin(Text title) {
       super(title);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void fluxvisuals$animHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
       BetterMinecraft mod = BetterMinecraft.INSTANCE;
       if (!mod.isEnabled() || !mod.inventoryAnim.get()) return;

       float t = mod.getInventoryAnim();
       if (t <= 0.001F) {
          ci.cancel();
          return;
       }
       float scale = 0.92F + 0.08F * t;
       float cx = this.width / 2F;
       float cy = this.height / 2F;
       context.getMatrices().pushMatrix();
       context.getMatrices().translate(cx, cy);
       context.getMatrices().scale(scale, scale);
       context.getMatrices().translate(-cx, -cy);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void fluxvisuals$animTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
       BetterMinecraft mod = BetterMinecraft.INSTANCE;
       if (!mod.isEnabled() || !mod.inventoryAnim.get()) return;
       if (mod.getInventoryAnim() <= 0.001F) return;

       context.getMatrices().popMatrix();
    }
}
