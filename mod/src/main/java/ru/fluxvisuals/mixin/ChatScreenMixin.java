package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.util.render.HudRenderBridge;

/**
 * Рисует HUD-элементы FluxVisuals ПОВЕРХ открытого чата.
 *
 * <p>Иначе элементы (бинды, потионсы, аррайлист и т.п.) рисуются на этапе InGameHud,
 * то есть ДО отрисовки ChatScreen, и оказываются спрятаны за тёмным фоном панели чата.
 * Хук на ChatScreen.render(RETURN) рисует их поверх панели.
 */
@Environment(EnvType.CLIENT)
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
   @Inject(method = "render", at = @At("RETURN"))
   private void fluxvisuals$renderHudOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.player == null || client.world == null) {
         return;
      }
      HudRenderBridge.renderHudOverlay(context, client.getRenderTickCounter());
   }
}
