package ru.fluxvisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Анимация букв в поле ввода (чат): каждая новая буква «выпрыгивает»
 * (easeOutBack) при наборе. Перенесено из GodWeer.
 */
@Environment(EnvType.CLIENT)
@Mixin(TextFieldWidget.class)
public abstract class BetterMinecraftTextFieldMixin {
   @Shadow abstract String getText();
   @Shadow private int firstCharacterIndex;

   @Unique private String fluxvisuals$prevText = "";
   @Unique private final Map<Integer, Long> fluxvisuals$charTimestamps = new HashMap<>();

   private static BetterMinecraft bmc() {
      if (FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) return null;
      return FluxVisualsClient.get.manager.get(BetterMinecraft.class);
   }

   @Inject(method = "renderWidget", at = @At("HEAD"))
   private void fluxvisuals$onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      BetterMinecraft mod = bmc();
      if (mod == null || !mod.enable || !BetterMinecraft.chatCharAnim.get()) return;

      String currentText = this.getText();
      if (!currentText.equals(fluxvisuals$prevText)) {
         long now = System.currentTimeMillis();
         for (int i = 0; i < currentText.length(); i++) {
            if (i >= fluxvisuals$prevText.length() || currentText.charAt(i) != fluxvisuals$prevText.charAt(i)) {
               fluxvisuals$charTimestamps.put(i, now);
            }
         }
         fluxvisuals$prevText = currentText;
      }
   }

   @WrapOperation(
      method = "renderWidget",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V"
      )
   )
   private void fluxvisuals$wrapDrawText(DrawContext context, TextRenderer textRenderer, OrderedText orderedText, int x, int y, int color, boolean shadow, Operation<Void> original) {
      BetterMinecraft mod = bmc();
      if (mod == null || !mod.enable || !BetterMinecraft.chatCharAnim.get()) {
         original.call(context, textRenderer, orderedText, x, y, color, shadow);
         return;
      }
      fluxvisuals$renderAnimatedOrderedText(context, textRenderer, orderedText, x, y, color, shadow, original);
   }

   @Unique
   private void fluxvisuals$renderAnimatedOrderedText(DrawContext context, TextRenderer textRenderer, OrderedText orderedText, int x, int y, int color, boolean shadow, Operation<Void> original) {
      List<CharInfo> chars = new ArrayList<>();
      orderedText.accept((charIndex, style, codePoint) -> {
         chars.add(new CharInfo(charIndex, style, codePoint));
         return true;
      });

      int currentX = x;
      long now = System.currentTimeMillis();
      float animDuration = BetterMinecraft.animSpeed.get();
      int fontHeight = textRenderer.fontHeight;

      for (CharInfo ci : chars) {
         String charStr = new String(Character.toChars(ci.codePoint));
         int charWidth = textRenderer.getWidth(charStr);
         int globalIndex = this.firstCharacterIndex + ci.charIndex;

         Long spawnTime = fluxvisuals$charTimestamps.get(globalIndex);
         float progress = spawnTime == null ? 1.0f : Math.min(1.0f, (now - spawnTime) / animDuration);

         OrderedText singleCharText = OrderedText.styledBackwardsVisitedString(charStr, ci.style);

         if (progress < 1.0f) {
            float scale = fluxvisuals$easeOutBack(progress);

            float centerX = currentX + charWidth / 2.0f;
            float centerY = y + fontHeight / 2.0f;

            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();

            matrices.translate(centerX, centerY);
            matrices.scale(scale, scale);
            matrices.translate(-centerX, -centerY);

            original.call(context, textRenderer, singleCharText, currentX, y, color, shadow);

            matrices.popMatrix();
         } else {
            original.call(context, textRenderer, singleCharText, currentX, y, color, shadow);
         }

         currentX += charWidth;
      }
   }

   @Unique
   private static float fluxvisuals$easeOutBack(float x) {
      float c1 = 1.70158f;
      float c3 = c1 + 1.0f;
      return 1.0f + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
   }

   @Unique
   private static class CharInfo {
      final int charIndex;
      final Style style;
      final int codePoint;

      CharInfo(int charIndex, Style style, int codePoint) {
         this.charIndex = charIndex;
         this.style = style;
         this.codePoint = codePoint;
      }
   }
}
