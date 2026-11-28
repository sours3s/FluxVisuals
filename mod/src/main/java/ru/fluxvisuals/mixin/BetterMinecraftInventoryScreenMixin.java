package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/** Экранная анимация личного инвентаря из эталонной реализации. */
@Environment(EnvType.CLIENT)
@Mixin(InventoryScreen.class)
public abstract class BetterMinecraftInventoryScreenMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void fluxvisuals$invHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
       BetterMinecraft mod = BetterMinecraft.INSTANCE;
       if (!mod.isEnabled() || !mod.inventoryAnim.get()) return;

       float t = mod.getInventoryAnim();
       if (t <= 0.001F) {
          ci.cancel();
          return;
       }
       float scale = 0.92F + 0.08F * t;
       float cx = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2F;
       float cy = MinecraftClient.getInstance().getWindow().getScaledHeight() / 2F;
       context.getMatrices().pushMatrix();
       context.getMatrices().translate(cx, cy);
       context.getMatrices().scale(scale, scale);
       context.getMatrices().translate(-cx, -cy);
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void fluxvisuals$invTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
       BetterMinecraft mod = BetterMinecraft.INSTANCE;
       if (!mod.isEnabled() || !mod.inventoryAnim.get()) return;
       if (mod.getInventoryAnim() <= 0.001F) return;

       context.getMatrices().popMatrix();
    }
}
