package ru.fluxvisuals.mixin.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Custom splash screen for FluxVisuals
 */
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

    @Shadow @Final @Mutable
    public static Identifier LOGO;

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;
    @Shadow @Final private Consumer<Optional<Throwable>> exceptionHandler;
    @Shadow @Final private boolean reloading;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void modifyLogoIdentifier(CallbackInfo ci) {
        LOGO = Identifier.of("fluxvisuals", "images/ui/pic/cute/cutie2.png");
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void customRender(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        long time = Util.getMeasuringTimeMs();

        if (this.reloading && this.reloadStartTime == -1L) {
            this.reloadStartTime = time;
        }

        float f = this.reloadCompleteTime > -1L ? (float)(time - this.reloadCompleteTime) / 1000.0F : -1.0F;
        float g = this.reloadStartTime > -1L ? (float)(time - this.reloadStartTime) / 500.0F : -1.0F;
        float opacity;
        int backgroundColor = ColorHelper.getArgb(255, 8, 10, 18);

        if (f >= 1.0F) {
            if (this.client.currentScreen != null) {
                this.client.currentScreen.renderWithTooltip(context, 0, 0, deltaTicks);
            }
            int alpha = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
            context.createNewRootLayer();
            context.fill(0, 0, width, height, withAlphaLocal(backgroundColor, alpha));
            opacity = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
        } else if (this.reloading) {
            if (this.client.currentScreen != null && g < 1.0F) {
                this.client.currentScreen.renderWithTooltip(context, mouseX, mouseY, deltaTicks);
            }
            int alpha = MathHelper.ceil(MathHelper.clamp((double)g, 0.15, 1.0) * 255.0);
            context.createNewRootLayer();
            context.fill(0, 0, width, height, withAlphaLocal(backgroundColor, alpha));
            opacity = MathHelper.clamp(g, 0.0F, 1.0F);
        } else {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(
                    this.client.getFramebuffer().getColorAttachment(),
                    backgroundColor
            );
            opacity = 1.0F;
        }

        float centerX = width / 2.0F;
        float centerY = height / 2.0F;

        double d = Math.min((double)width * 0.75D, (double)height) * 0.25D;
        float size = (float) (d * 1.8D);
        float halfSize = size / 2.0F;

        int drawX = (int) (centerX - halfSize);
        int drawY = (int) (centerY - halfSize);
        int intSize = (int) size;

        int alphaValue = MathHelper.ceil(opacity * 255.0F);
        int tintColor = ColorHelper.getArgb(alphaValue, 255, 255, 255);

        context.drawTexture(
                RenderPipelines.MOJANG_LOGO,
                LOGO,
                drawX,
                drawY,
                0.0F,
                0.0F,
                intSize,
                intSize,
                256, 256,
                256, 256,
                tintColor
        );

        if (f >= 2.0F) {
            this.client.setOverlay(null);
        }

        if (this.reloadCompleteTime == -1L && this.reload.isComplete() && (!this.reloading || g >= 2.0F)) {
            try {
                this.reload.throwException();
                this.exceptionHandler.accept(Optional.empty());
            } catch (Throwable throwable) {
                this.exceptionHandler.accept(Optional.of(throwable));
            }

            this.reloadCompleteTime = Util.getMeasuringTimeMs();
            if (this.client.currentScreen != null) {
                this.client.currentScreen.init(context.getScaledWindowWidth(), context.getScaledWindowHeight());
            }
        }

        ci.cancel();
    }

    private int withAlphaLocal(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }
}
