package ru.fluxvisuals.mixin.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    protected TitleScreenMixin() {
        super(Text.empty());
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        if (FluxVisualsClient.get == null) {
            return;
        }

        // Ensure renderer is initialized before showing custom title screen
        ru.fluxvisuals.client.FluxVisualsClient.ensureRendererInitialized();

        MinecraftClient.getInstance().setScreen(new ru.fluxvisuals.screen.screens.main.TitleScreen());
    }
}