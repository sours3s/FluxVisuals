package ru.fluxvisuals.mixin;

import com.mojang.blaze3d.shaders.ShaderType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.shader.ShaderStore;

/**
 * исходныйский ClickGUI хранит часть шейдеров в зашифрованном виде внутри ShaderData
 * и получает их через ShaderStore. Без этой подмены MC не находит исходники
 * (например client:core/grayscale) и ClickGUI рисуется пустым.
 */
@Environment(EnvType.CLIENT)
@Mixin({ShaderLoader.class})
public class ShaderLoaderMixin {
   @Inject(
      method = {"getSource"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void provideShaderSource(Identifier id, ShaderType type, CallbackInfoReturnable<String> cir) {
      String source = ShaderStore.getSource(id, type);
      if (source != null) {
         cir.setReturnValue(source);
      }
   }
}
