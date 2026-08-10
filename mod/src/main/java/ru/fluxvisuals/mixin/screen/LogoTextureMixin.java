package ru.fluxvisuals.mixin.screen;

import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.MipmapStrategy;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;

/**
 * Custom logo texture loader for FluxVisuals splash screen
 */
@Mixin(targets = "net.minecraft.client.gui.screen.SplashOverlay$LogoTexture")
public class LogoTextureMixin {

    @Inject(
            method = "loadContents(Lnet/minecraft/resource/ResourceManager;)Lnet/minecraft/client/texture/TextureContents;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void loadCustomLogo(ResourceManager resourceManager, CallbackInfoReturnable<TextureContents> cir) {
        String path = "/assets/fluxvisuals/images/ui/pic/cute/cutie2.png";
        System.out.println("[FluxVisuals] Loading splash logo: " + path);

        try (InputStream inputStream = LogoTextureMixin.class.getResourceAsStream(path)) {
            if (inputStream != null) {
                NativeImage nativeImage = NativeImage.read(inputStream);

                TextureResourceMetadata metadata = new TextureResourceMetadata(true, true, MipmapStrategy.AUTO, 0.0F);

                System.out.println("[FluxVisuals] Splash logo loaded: " + nativeImage.getWidth() + "x" + nativeImage.getHeight());
                cir.setReturnValue(new TextureContents(nativeImage, metadata));
            } else {
                System.err.println("[FluxVisuals] Splash logo not found in classpath");
            }
        } catch (IOException e) {
            System.err.println("[FluxVisuals] Error reading splash logo PNG");
            e.printStackTrace();
        }
    }
}
