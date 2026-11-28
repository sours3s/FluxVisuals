package ru.fluxvisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

/** Эталонная анимация хотбара: подъём снизу вокруг ванильного вызова renderHotbar. */
@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class BetterMinecraftHotbarMixin {
    @WrapOperation(
       method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
       at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"
       )
    )
    private void fluxvisuals$wrapHotbar(
       DrawContext context, RenderTickCounter tickCounter, Operation<Void> original
    ) {
       BetterMinecraft mod = BetterMinecraft.INSTANCE;
       boolean applyHotbarAnim = mod.isEnabled() && mod.hotbarAnim.get();
       float hotbarT = mod.getHotbarAnim();
       if (!applyHotbarAnim || hotbarT <= 0.001F) {
          original.call(context, tickCounter);
          return;
       }

       float offsetY = 30F * (1F - hotbarT);
       Matrix3x2fStack matrices = context.getMatrices();
       matrices.pushMatrix();
       matrices.translate(0, offsetY);
       try {
          original.call(context, tickCounter);
       } finally {
          matrices.popMatrix();
       }
    }
}
