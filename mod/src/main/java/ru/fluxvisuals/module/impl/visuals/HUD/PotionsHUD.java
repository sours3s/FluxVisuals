package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.animation.AnimationMath;
import ru.fluxvisuals.util.render.math.animation.anim.util.Animation2;
import ru.fluxvisuals.util.render.math.animation.anim.util.Easings;
import ru.fluxvisuals.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public class PotionsHUD {
   public static MinecraftClient mc = MinecraftClient.getInstance();

   // === Настройки ===
   public static final SliderSetting scale = new SliderSetting("PotionsHUD Scale", 1.4F, 0.5F, 2.5F, 0.05F, false);

   private static final int HARMFUL_EFFECT_COLOR = 0xFFFF5353;
   private static final float HEIGHT = 14.5F;
   private static final float PADDING_SIDE = 4F;
   private static final float SPACING = 0.5F;
   private static final float TEXT_SIZE = 7.0F;
   private static final float ICON_SIZE = 9.0F;

   private static final Map<RegistryEntry<StatusEffect>, Integer> maxDurations = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, Float> animatedWidths = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, Float> animatedLineX = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, Float> animatedY = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, Animation2> animatedAlphas = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, Animation2> animatedSlide = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, StatusEffectInstance> cachedEffects = new HashMap<>();

   // Общая анимация появления панели при открытии чата (как у Binds): fade + сдвиг сверху.
   private static final Animation2 panelAnim = new Animation2();

   public static enum Style {
      DARK("Dark"),
      GLASS("Glass");

      private final String renderName;

      Style(String renderName) {
         this.renderName = renderName;
      }

      public String getRenderName() {
         return renderName;
      }
   }

   private static Style styleSetting = Style.GLASS;

   /** Обновляет анимацию панели и возвращает текущую прозрачность (0..1). */
   private static float updatePanelAlpha() {
      boolean chat = mc.currentScreen instanceof ChatScreen;
      boolean close = !hasContent() && !chat;
      panelAnim.run(close ? 0.0 : 1.0, 0.15, Easings.QUART_OUT, true);
      panelAnim.update();
      return (float) panelAnim.get();
   }

   /** Есть ли активные эффекты (контент для отображения). */
   public static boolean hasContent() {
      return mc.player != null && mc.player.getStatusEffects() != null && !mc.player.getStatusEffects().isEmpty();
   }

   /**
    * Пустое состояние — такой же хедер-бокс как у Binds, чтобы элемент был
    * виден/перетаскиваем и выглядел одинаково при отсутствии эффектов.
    */
   public static void renderEmpty(Renderer2D r2) {
      float pAnim = updatePanelAlpha();
      float w = 130.0F;
      float h = 44.0F;
      float preferredX = 20.0F;
      float preferredY = 474.0F;
      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag("potionsHUD", preferredX, preferredY, w, h);
      float x = session.positionX();
      float y = session.positionY() - 32.0F * (1.0F - pAnim);
      r2.pushAlpha(pAnim);
      drawStyle(r2, x, y, w, h, pAnim);
      r2.text(FontRegistry.INTER_MEDIUM, x + 14.0F, y + 28.0F, 28.0F, "Potions",
            ru.fluxvisuals.util.color.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), 1.0F));
      float iconX = x + w - 28.0F;
      r2.text(FontRegistry.ICONS, iconX, y + 30.0F, 36.0F, "x",
            ru.fluxvisuals.util.color.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 1.0F));
      r2.rect(x + 10.0F, y + 39.52F, w - 20.0F, 1.0F,
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 25));
      DraggableManager.getInstance().endDrag(session);
      r2.popAlpha();
   }

   public static void potions(Renderer2D r2, DrawContext drawContext) {
      if (mc.player != null) {
         boolean chatOpen = mc.currentScreen instanceof ChatScreen;
         float pAnim = updatePanelAlpha();
         r2.pushAlpha(pAnim);
         Set<RegistryEntry<StatusEffect>> activeEffects = new HashSet<>();
         for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
            activeEffects.add(effect.getEffectType());
         }

         for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
            cachedEffects.put(effect.getEffectType(), effect);
            RegistryEntry<StatusEffect> effectType = effect.getEffectType();
            if (!animatedAlphas.containsKey(effectType)) {
               Animation2 newAnim = new Animation2();
               newAnim.set(0.0);
               animatedAlphas.put(effectType, newAnim);
            }
            // Initialize slide animation for new effects (slide from top like Binds)
            if (!animatedSlide.containsKey(effectType)) {
               Animation2 slideAnim = new Animation2();
               slideAnim.set(-30.0); // start offset from top
               animatedSlide.put(effectType, slideAnim);
            }
         }

         animatedAlphas.forEach((effectTypex, anim) -> {
            double targetValue = activeEffects.contains(effectTypex) ? 1.0 : 0.0;
            anim.run(targetValue, 0.6, Easings.QUART_OUT, true);
            anim.update();
         });

         // Update slide animations (like Binds module animations)
         animatedSlide.forEach((effectTypex, anim) -> {
            double targetValue = activeEffects.contains(effectTypex) ? 0.0 : -30.0;
            anim.run(targetValue, 0.4, Easings.EXPO_OUT, true);
            anim.update();
         });
         Set<RegistryEntry<StatusEffect>> effectsToRender = new HashSet<>(activeEffects);
         animatedAlphas.forEach((effectTypex, anim) -> {
            if (anim.get() > 0.01F) {
               effectsToRender.add(effectTypex);
            }
         });
         float offset = 0.0F;
         List<RegistryEntry<StatusEffect>> sortedEffects = new ArrayList<>(effectsToRender);
         sortedEffects.sort((a, b) -> {
            boolean aActive = activeEffects.contains(a);
            boolean bActive = activeEffects.contains(b);
            if (aActive != bActive) {
               return aActive ? -1 : 1;
            } else {
               return 0;
            }
         });
         int effectIndex = 0;

         // Pre-compute total bounding box for draggability
         float maxRectWidth = 0.0F;
         float totalHeight = 0.0F;
         for (RegistryEntry<StatusEffect> effectType : sortedEffects) {
            StatusEffectInstance effectInst = cachedEffects.get(effectType);
            if (effectInst == null) continue;
            Animation2 alphaAnim = animatedAlphas.get(effectType);
            if (alphaAnim == null) continue;
            float currentAlpha = alphaAnim.get();
            String effectText = effectInst.getTranslationKey().replace("effect.minecraft.", "");
            String text = effectText.substring(0, 1).toUpperCase()
                  + effectText.substring(1).replace("_", " ")
                  + " "
                  + String.valueOf(effectInst.getAmplifier() + 1).replace("1", "");
            float textWidth = r2.measureText(FontRegistry.INTER_MEDIUM, text, 28.0F).width;
            float rectW = 100.0F + textWidth;
            if (rectW > maxRectWidth) maxRectWidth = rectW;
            totalHeight += 45.0F * currentAlpha;
         }
         float s = scale.get();
         float boundingWidth = Math.max(maxRectWidth, 120.0F) * s;
         float boundingHeight = Math.max(totalHeight, 40.64F) * s;

         float preferredX = 20.0F;
         float preferredY = 474.0F;
         DraggableManager.DragSession session = DraggableManager.getInstance()
               .beginDrag("potionsHUD", preferredX, preferredY, boundingWidth, boundingHeight);
         float x = session.positionX();
         float y = session.positionY() - 32.0F * (1.0F - pAnim);

         float guiScale = (float)mc.getWindow().getScaleFactor();
         if (guiScale <= 0.0F) {
            guiScale = 1.0F;
         }
         float hudScale = session.scale();
         // Apply the module scale setting so it actually affects rendered content
         float finalScale = hudScale * s;

         drawContext.getMatrices().pushMatrix();
         drawContext.getMatrices().translate(x / guiScale, y / guiScale);
         drawContext.getMatrices().scale(finalScale, finalScale);
         drawContext.getMatrices().translate(-x / guiScale, -y / guiScale);

         try {
            for (RegistryEntry<StatusEffect> effectType : sortedEffects) {
               StatusEffectInstance effectx = cachedEffects.get(effectType);
               if (effectx != null) {
                  int currentDuration = activeEffects.contains(effectType) ? effectx.getDuration() : 0;
                  Animation2 alphaAnim = animatedAlphas.get(effectType);
                  if (alphaAnim != null) {
                     float currentAlpha = alphaAnim.get();
                     String effectText = effectx.getTranslationKey().replace("effect.minecraft.", "");
                     String text = effectText.substring(0, 1).toUpperCase()
                           + effectText.substring(1).replace("_", " ")
                           + " "
                           + String.valueOf(effectx.getAmplifier() + 1).replace("1", "");
                     float textWidth = r2.measureText(FontRegistry.INTER_MEDIUM, text, 28.0F).width;
                     String timeTextForWidth = formatDuration(currentDuration);
                     float timeWidthForBox = r2.measureText(FontRegistry.INTER_MEDIUM, timeTextForWidth, 22.0F).width;
                     float timeBoxWidthForBox = timeWidthForBox + 8.0F;
                     float mainRectWidth = 75.0F + textWidth + timeBoxWidthForBox;

                     boolean isRightSide = x > mc.getWindow().getWidth() / 2.0F;
                     float baseX = isRightSide ? (x + boundingWidth - mainRectWidth) : x;
                     float x3 = isRightSide ? (80.0F - 80.0F * currentAlpha) : (-80.0F + 80.0F * currentAlpha);
                     float drawX = baseX + x3;

                     float targetY = y + offset;
                     float currentAnimatedY = animatedY.getOrDefault(effectType, targetY);
                     currentAnimatedY = AnimationMath.animation(currentAnimatedY, targetY, 0.1F);
                     animatedY.put(effectType, currentAnimatedY);

                     // Apply slide animation (like Binds)
                     float slideOffset = 0.0F;
                     Animation2 slideAnim = animatedSlide.get(effectType);
                     if (slideAnim != null) {
                        slideOffset = (float) slideAnim.get();
                     }
                     currentAnimatedY += slideOffset;

                     if (currentAlpha <= 0.01F) {
                        offset += 45.0F * currentAlpha;
                        effectIndex++;
                     } else {
                        r2.pushAlpha(currentAlpha);
                        if (!maxDurations.containsKey(effectType) || currentDuration > maxDurations.get(effectType)) {
                           maxDurations.put(effectType, currentDuration);
                        }

                        String timeText = formatDuration(effectx.getDuration());
                        float timeWidth = r2.measureText(FontRegistry.INTER_MEDIUM, timeText, 22.0F).width;
                        float timeBoxWidth = timeWidth + 8.0F;
                        float timeBoxHeight = 16.03F;
                        Hud.drawClientRectLight(r2, drawX, currentAnimatedY, mainRectWidth, 40.64F, 13.0F, 1.0F, 1.0F);
                        int maxDuration = maxDurations.get(effectType);
                        float progress = maxDuration > 0 ? (float) currentDuration / maxDuration : 0.0F;
                        float targetWidth = mainRectWidth * progress;
                        float currentAnimatedWidth = animatedWidths.getOrDefault(effectType, targetWidth);
                        currentAnimatedWidth = AnimationMath.animation(currentAnimatedWidth, targetWidth, 0.1F);
                        animatedWidths.put(effectType, currentAnimatedWidth);
                        int color = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 0);
                        int color2 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 28);
                        if (currentAnimatedWidth > 2.0F) {
                           float gradientWidth = currentAnimatedWidth - 2.0F;
                           float targetLineX = isRightSide ? (drawX + mainRectWidth - gradientWidth) : (drawX + gradientWidth);
                           float currentLineX = animatedLineX.getOrDefault(effectType, targetLineX);
                           currentLineX = AnimationMath.animation(currentLineX, targetLineX, 0.1F);
                           animatedLineX.put(effectType, currentLineX);

                           r2.pushRoundedClipRect(drawX, currentAnimatedY, mainRectWidth, 40.64F,
                                 13.0F, 13.0F, 13.0F, 13.0F);
                           if (isRightSide) {
                               float rw = (drawX + mainRectWidth) - currentLineX;
                               r2.horizontalGradient(currentLineX, currentAnimatedY, rw, 40.64F,
                                     0.0F, 3.0F, 3.0F, 0.0F, color2, color);
                               r2.rect(currentLineX - 2.0F, currentAnimatedY, 2.0F, 40.64F,
                                     Renderer2D.ColorUtil.getMainColor(1, 1));
                           } else {
                               r2.horizontalGradient(drawX, currentAnimatedY, gradientWidth, 40.64F,
                                     3.0F, 0.0F, 0.0F, 3.0F, color, color2);
                               r2.rect(currentLineX, currentAnimatedY, 2.0F, 40.64F,
                                     Renderer2D.ColorUtil.getMainColor(1, 1));
                           }
                           r2.popClipRect();
                        }

                        Identifier effectTexture = getEffectTexture(effectx.getEffectType());
                        float iconSize = 18.0F;
                        float iconX = isRightSide ? (drawX + mainRectWidth - 10.0F - iconSize) : (drawX + 10.0F);
                        float localTexX = iconX / guiScale;
                        float localTexY = (currentAnimatedY + (40.64F - iconSize) / 2.0F) / guiScale;
                        drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, effectTexture,
                              (int) localTexX, (int) localTexY, (int) (iconSize / guiScale), (int) (iconSize / guiScale));

                        float sepX = isRightSide ? (drawX + mainRectWidth - 39.5F) : (drawX + 37.5F);
                        r2.rect(sepX, currentAnimatedY + 15.0F, 2.0F, 11.0F, 4.0F,
                              Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 51));

                        // Determine if effect is beneficial by checking if it's in the harmful color list
                        // For now, use a simple heuristic: check the effect's translation key
                        String effectKey = effectx.getEffectType().getKey().map(k -> k.getValue().getPath()).orElse("");
                        boolean isBeneficial = !effectKey.contains("poison") && !effectKey.contains("wither") &&
                              !effectKey.contains("slowness") && !effectKey.contains("mining_fatigue") &&
                              !effectKey.contains("nausea") && !effectKey.contains("blindness") &&
                              !effectKey.contains("hunger") && !effectKey.contains("weakness") &&
                              !effectKey.contains("darkness") && !effectKey.contains("bad_omen") &&
                              !effectKey.contains("trial_omen") && !effectKey.contains("raid_omen") &&
                              !effectKey.contains("levitation") && !effectKey.contains("fatal_poison");
                        int textColor = isBeneficial ? Renderer2D.ColorUtil.getTextColor(1, 1)
                              : HARMFUL_EFFECT_COLOR;

                        float textX = isRightSide ? (drawX + mainRectWidth - 47.0F - textWidth) : (drawX + 47.0F);
                        r2.text(FontRegistry.INTER_MEDIUM, textX, currentAnimatedY + 25.5F, 28.0F, text, textColor);

                        float timeBoxX = isRightSide ? (drawX + mainRectWidth - 55.0F - textWidth - timeBoxWidth) : (drawX + 55.0F + textWidth);
                        float timeBoxY = currentAnimatedY + 13.0F;
                        r2.rectOutline(timeBoxX, timeBoxY, timeBoxWidth, timeBoxHeight, 3.0F,
                              Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 25), 1.0F);
                        r2.rect(timeBoxX, timeBoxY, timeBoxWidth, timeBoxHeight, 3.0F,
                              Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 25));
                        float timeTextX = timeBoxX + (timeBoxWidth - timeWidth) / 2.0F;
                        r2.text(FontRegistry.INTER_MEDIUM, timeTextX, currentAnimatedY + 25.5F, 22.0F, timeText,
                              Renderer2D.ColorUtil.getMainColor(1, 1));
                        r2.popAlpha();
                        offset += 45.0F * currentAlpha;
                        effectIndex++;
                     }
                  }
               }
            }
         } finally {
            drawContext.getMatrices().popMatrix();
         }

         // Pre-compute active effect types once
         Set<RegistryEntry<StatusEffect>> activeEffectTypes = new HashSet<>();
         for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
            activeEffectTypes.add(effect.getEffectType());
         }

         maxDurations.keySet().removeIf(key -> !activeEffectTypes.contains(key));
         animatedWidths.keySet().removeIf(key -> !activeEffectTypes.contains(key));
         animatedLineX.keySet().removeIf(key -> !activeEffectTypes.contains(key));
         animatedY.keySet().removeIf(key -> !activeEffectTypes.contains(key));
         cachedEffects.keySet().removeIf(key -> !activeEffectTypes.contains(key)
               && (animatedAlphas.get(key) == null || animatedAlphas.get(key).get() <= 0.01F));
         animatedAlphas.keySet().removeIf(key -> {
            Animation2 anim = animatedAlphas.get(key);
            return anim == null || anim.get() <= 0.01F && !activeEffectTypes.contains(key);
         });

         r2.popAlpha();
         DraggableManager.getInstance().endDrag(session);
      }
   }

   private static String formatDuration(int ticks) {
      int seconds = ticks / 20;
      int minutes = seconds / 60;
      int remainingSeconds = seconds % 60;
      return String.format("%02d:%02d", minutes, remainingSeconds);
   }

   private static Identifier getEffectTexture(RegistryEntry<StatusEffect> effect) {
      return effect.getKey().<Identifier>map(RegistryKey::getValue).map(id -> id.withPrefixedPath("mob_effect/"))
            .orElseGet(net.minecraft.client.texture.MissingSprite::getMissingSpriteId);
   }

   private static void drawStyle(Renderer2D r2, float rx, float ry, float rw, float rh, float alpha) {
      float round = 6.0F;
      if (Hud.blur.get("HUD")) {
         r2.prepareBlur(23.0F);
         r2.blur(rx, ry, rw, rh, round, alpha);
      }
      if (styleSetting == Style.DARK) {
         int bgColor = Renderer2D.ColorUtil.replAlpha(0xFF141419, (int) (180 * alpha));
         r2.rect(rx, ry, rw, rh, round, bgColor);
         int outColor = Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, (int) (15 * alpha));
         r2.rectOutline(rx, ry, rw, rh, round, outColor, 0.4F);
      } else {
         int bgColor = Renderer2D.ColorUtil.replAlpha(0xFF3E3E47, 0);
         r2.rect(rx, ry, rw, rh, round, bgColor);
         int outAlpha = (int) Math.min(Math.max(25 * alpha, 0), 255);
         int outColor = Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, outAlpha);
         r2.rectOutline(rx, ry, rw, rh, round, outColor, 0.4F);
      }
   }
}