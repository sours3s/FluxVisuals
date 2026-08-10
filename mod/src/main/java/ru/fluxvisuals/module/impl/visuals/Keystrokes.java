package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Keystrokes — оверлей нажатий WASD + ЛКМ/ПКМ + пробел/шифт.
 */
@IModule(name = "Keystrokes", description = "Показывает нажатия клавиш WASD, мыши и пробела", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class Keystrokes extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public final ModeSetting style = new ModeSetting("Style", "Dark", "Dark", "Glass");
   public final BooleanSetting showMouse = new BooleanSetting("Show Mouse", true);
   public final BooleanSetting showSpace = new BooleanSetting("Show Space", true);
   public final BooleanSetting showSprint = new BooleanSetting("Show Sprint", false);
   public final SliderSetting keySize = new SliderSetting("Key Size", 1.0F, 0.5F, 2.0F, 0.05F, false);
   public final SliderSetting spacing = new SliderSetting("Spacing", 3.0F, 1.0F, 8.0F, 0.5F, false);

   public Keystrokes() {
      this.addSettings(new Setting[]{style, showMouse, showSpace, showSprint, keySize, spacing});
   }

   @EventInit
   public void onRender(EventScreen e) {
      if (!this.enable || mc.player == null) return;
      if (mc.currentScreen instanceof ChatScreen) return;
      Renderer2D r2 = e.renderer();
      if (r2 == null) return;

      float s = keySize.get();
      float sp = spacing.get();
      float keyW = 22.0F * s;
      float keyH = 22.0F * s;
      float fontSize = 10.0F * s;

      float startX = 20.0F;
      float startY = 20.0F;
      int mainColor = Renderer2D.ColorUtil.getMainColor(1, 1);
      int textColor = Renderer2D.ColorUtil.getTextColor(1, 1);
      boolean isGlass = style.is("Glass");

      // WASD
      drawKey(r2, "W", mc.options.forwardKey, startX + keyW + sp, startY, keyW, keyH, fontSize, mainColor, textColor, isGlass);
      drawKey(r2, "A", mc.options.leftKey, startX, startY + keyH + sp, keyW, keyH, fontSize, mainColor, textColor, isGlass);
      drawKey(r2, "S", mc.options.backKey, startX + keyW + sp, startY + keyH + sp, keyW, keyH, fontSize, mainColor, textColor, isGlass);
      drawKey(r2, "D", mc.options.rightKey, startX + (keyW + sp) * 2, startY + keyH + sp, keyW, keyH, fontSize, mainColor, textColor, isGlass);

      float mouseY = startY + (keyH + sp) * 2 + sp;
      if (showMouse.get()) {
         drawKey(r2, "L", mc.options.attackKey, startX, mouseY, keyW, keyH, fontSize, mainColor, textColor, isGlass);
         drawKey(r2, "R", mc.options.useKey, startX + keyW + sp, mouseY, keyW, keyH, fontSize, mainColor, textColor, isGlass);
      }

      if (showSpace.get()) {
         float spaceW = keyW * 3 + sp * 2;
         float spaceY = mouseY + (showMouse.get() ? keyH + sp : 0);
         drawKey(r2, "_", mc.options.jumpKey, startX, spaceY, spaceW, keyH * 0.6F, fontSize, mainColor, textColor, isGlass);
      }
   }

   private void drawKey(Renderer2D r2, String label, KeyBinding key, float x, float y, float w, float h,
                          float fontSize, int mainColor, int textColor, boolean glass) {
      boolean pressed = key.isPressed();
      int bgColor;
      if (pressed) {
         bgColor = glass
            ? Renderer2D.ColorUtil.replAlpha(mainColor, 60)
            : Renderer2D.ColorUtil.replAlpha(0xFF333333, 200);
      } else {
         bgColor = glass
            ? Renderer2D.ColorUtil.replAlpha(0xFF3E3E47, 80)
            : Renderer2D.ColorUtil.replAlpha(0xFF1A1A1F, 180);
      }
      r2.rect(x, y, w, h, 4.0F, bgColor);
      if (!glass) {
         r2.rectOutline(x, y, w, h, 4.0F, Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, 15), 0.5F);
      }
      int c = pressed ? mainColor : textColor;
      float tw = r2.measureText(FontRegistry.INTER_MEDIUM, label, fontSize).width;
      r2.text(FontRegistry.INTER_MEDIUM, x + (w - tw) / 2.0F, y + (h - fontSize / 2.0F) / 2.0F, fontSize, label, c);
   }
}
