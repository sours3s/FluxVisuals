package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.keyboard.Keyboard;
import ru.fluxvisuals.util.render.animation.util.Animation;
import ru.fluxvisuals.util.render.animation.util.Easings;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public class KeyBindHUD {
   public static MinecraftClient mc = MinecraftClient.getInstance();
   private static final List<Module> bindings = new ArrayList<>();
   private static final Animation widthAnimation = new Animation();
   private static final Animation heightAnimation = new Animation();
   public static Animation anim2 = new Animation();

   private static boolean shouldDisplay(Module module) {
      boolean isActiveOrAnimating = module.enable || module.mAnim.get() != 0.0;
      boolean hasBind = module.bind > 0;
      return isActiveOrAnimating && hasBind;
   }

   private static void sortModules() {
      bindings.clear();
      for (Module module : FluxVisualsClient.get.manager.getModules()) {
         if (shouldDisplay(module)) {
            bindings.add(module);
         }
      }
      bindings.sort(Comparator.comparing(module -> module.name));
   }

   public static void keybind(Renderer2D r2) {
      sortModules();
      anim2.update();
      boolean expand = !bindings.isEmpty();
      String headerName = "Binds";
      float headerTextWidth = r2.measureText(FontRegistry.INTER_MEDIUM, headerName, 28.0F).width;
      float minWidth = 90.0F + headerTextWidth;
      float calculatedWidth = minWidth;
      if (expand) {
         for (Module module : bindings) {
            module.mAnim.update();
            float animPC = module.mAnim.get();
            if (animPC > 0.0F) {
               String keyName = Keyboard.keyName(module.bind);
               String moduleName = module.name;
               float moduleNameWidth = r2.measureText(FontRegistry.INTER_MEDIUM, moduleName, 28.0F).width;
               float keyNameWidth = r2.measureText(FontRegistry.INTER_MEDIUM, "[" + keyName + "]", 28.0F).width;
               float totalWidth = 45.0F + moduleNameWidth + keyNameWidth + 20.0F;
               calculatedWidth = Math.max(calculatedWidth, totalWidth * animPC);
            }
         }
      }

      calculatedWidth = Math.max(calculatedWidth, 130.0F);
      float headerHeight = 14.0F;
      float calculatedHeight = headerHeight;
      if (expand) {
         float offset = 0.0F;

         for (Module modulex : bindings) {
            modulex.mAnim.update();
            float animPC = modulex.mAnim.get();
            offset += 20.0F * animPC;
         }

         if (offset > 0.0F) {
            calculatedHeight = 45.0F + headerHeight + offset + 2.0F;
         }
      }

      calculatedHeight = Math.max(calculatedHeight, 45.0F);
      boolean isEmpty = bindings.isEmpty();
      boolean closeCondition = isEmpty && !(mc.currentScreen instanceof ChatScreen);
      anim2.run(closeCondition ? 0.0 : 1.0, 0.15F, Easings.QUAD_OUT);
      widthAnimation.update();
      heightAnimation.update();
      widthAnimation.run(calculatedWidth, 0.15, Easings.QUART_OUT);
      heightAnimation.run(Math.max(headerHeight, calculatedHeight - 4.0F), 0.15, Easings.QUART_OUT);
      float animatedWidth = widthAnimation.get();
      float animatedHeight = heightAnimation.get();
      if (!closeCondition || anim2.get() != 0.0F) {
         float preferredX = (mc.getWindow().getWidth() - animatedWidth) / 2.0F;
         float preferredY = (mc.getWindow().getHeight() - animatedHeight) / 2.0F;
         DraggableManager.DragSession session = DraggableManager.getInstance().beginDrag("keybinds", preferredX, preferredY, animatedWidth, animatedHeight);
         float x = session.positionX();
         float y = session.positionY() - 40.0F + 40.0F * anim2.get();
         Hud.drawClientRect(r2, x, y, animatedWidth, animatedHeight, 13.0F, 1.0F * anim2.get(), 1.0F);
         r2.text(FontRegistry.INTER_MEDIUM, x + 14.0F, y + 28.0F, 28.0F, "Binds", ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), anim2.get()));
         float iconX = x + animatedWidth - 28.0F;
         r2.text(FontRegistry.ICONS, iconX, y + 30.0F, 36.0F, "x", ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), anim2.get()));
         r2.shadow(
            iconX + 7.0F,
            y + 20.0F,
            0.3F,
            0.3F,
            10.0F,
            7.5F,
            0.5F,
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(75.0F * anim2.get()))
         );
         r2.rect(x + 10.0F, y + 39.52F, animatedWidth - 20.0F, 1.0F, Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(25.0F * anim2.get())));
         if (expand) {
            float offset = 0.0F;

            for (Module modulex : bindings) {
               modulex.mAnim.update();
               float animPC = modulex.mAnim.get();
               if (animPC > 0.0F) {
                  String keyName = Keyboard.keyName(modulex.bind);
                  String moduleName = modulex.name;
                  float keyNameWidth = r2.measureText(FontRegistry.INTER_MEDIUM, keyName, 28.0F).width;
                  int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(animPC * 255.0F));
                  int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(animPC * 255.0F));
                  float addx = 40.0F - 40.0F * animPC;
                  r2.text(FontRegistry.INTER_MEDIUM, x + 14.0F + addx, y + 50.0F + headerHeight + offset, 28.0F, moduleName, textColor);
                  r2.text(
                     FontRegistry.INTER_MEDIUM, x + animatedWidth - 14.0F - keyNameWidth - addx, y + 50.0F + headerHeight + offset, 28.0F, keyName, mainColor
                  );
                  offset += 20.0F * animPC;
               }
            }
         }

         DraggableManager.getInstance().endDrag(session);
      }
   }
}
