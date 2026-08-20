package ru.fluxvisuals.ui.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.config.GuiManager;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.render.RenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.Manager;
import ru.fluxvisuals.module.api.Theme;
import ru.fluxvisuals.module.api.setting.impl.HueSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.ui.gui.component.main.GuiCharTyped;
import ru.fluxvisuals.ui.gui.component.main.GuiInit;
import ru.fluxvisuals.ui.gui.component.main.GuiKeyPressed;
import ru.fluxvisuals.ui.gui.component.main.GuiMouseReleased;
import ru.fluxvisuals.ui.gui.component.main.GuiMouseScrolled;
import ru.fluxvisuals.ui.gui.component.main.GuiShouldCloseOnEsc;
import ru.fluxvisuals.ui.gui.component.mouse.GuiMouseClicked;
import ru.fluxvisuals.ui.gui.component.render.GuiRender;
import ru.fluxvisuals.ui.gui.theme.ThemeScreen;
import ru.fluxvisuals.util.player.MovementManager;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.ScaleHelper;

@Environment(EnvType.CLIENT)
public class GuiClient extends Screen {
   public ThemeScreen themeScreen;
   public MinecraftClient mc = MinecraftClient.getInstance();
   private static volatile boolean eventsRegistered = false;

   public GuiClient() {
      super(Text.literal("Gui"));
   }

   public static void registerEventHandlers() {
      if (!eventsRegistered) {
         eventsRegistered = true;
         // Event handlers are now registered through Screen lifecycle methods (render, mouseClicked, etc.)
         // No need for separate event registration since GuiClient extends Screen
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      if (!FluxVisualsClient.isModInitialized()) {
         return;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.getWindow() == null) {
         return;
      }

      Renderer2D renderer = FluxVisualsClient.getRenderer();
      if (renderer != null) {
         int width = client.getWindow().getFramebufferWidth();
         int height = client.getWindow().getFramebufferHeight();
         if (width > 0 && height > 0) {
            try {
               renderer.begin(width, height);
               // Render background blur and GUI
               GuiRender.render(renderer, context, mouseX, mouseY, deltaTicks);
            } finally {
               renderer.end();
            }
         }
      }
      // Do NOT call super.render() - this is a fully custom GUI, no vanilla widgets
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public void renderInGameBackground(DrawContext context) {
   }

   @Override
   public boolean mouseClicked(Click click, boolean bl) {
      Renderer2D renderer = FluxVisualsClient.getRenderer();
      return renderer != null && GuiMouseClicked.mouseClicked(renderer, click.comp_4798(), click.comp_4799(), click.button()) ? true : super.mouseClicked(click, bl);
   }

   @Override
   public boolean mouseReleased(Click click) {
      GuiMouseReleased.mouseReleased();
      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseDragged(Click click, double pDragX, double pDragY) {
      return this.handleMouseDragged(click.comp_4798(), click.comp_4799()) ? true : super.mouseDragged(click, pDragX, pDragY);
   }

   public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
      return GuiMouseScrolled.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY) ? true : super.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      return GuiKeyPressed.keyPressed(input.comp_4795(), input.comp_4796(), input.comp_4797()) ? true : super.keyPressed(input);
   }

   @Override
   public boolean charTyped(CharInput input) {
      return GuiCharTyped.charTyped((char)input.comp_4793(), input.comp_4794()) ? true : super.charTyped(input);
   }

   public boolean shouldCloseOnEsc() {
      return GuiShouldCloseOnEsc.shouldCloseOnEsc();
   }

   private boolean handleMouseDragged(double pMouseX, double pMouseY) {
      int mouseX = (int)ScaleHelper.calc((float)pMouseX, (float)pMouseY)[0];
      int mouseY = (int)ScaleHelper.calc((float)pMouseX, (float)pMouseY)[1];
      if (GuiScreen.activeColorPicker instanceof HueSetting hueSetting) {
         float pickerX = GuiScreen.colorPickerX;
         float pickerY = GuiScreen.colorPickerY;
         if (pickerX != 0.0F || pickerY != 0.0F) {
            float paletteWidth = 63.92F;
            float paletteHeight = 47.02F;
            float paletteX = pickerX + 5.0F;
            float paletteY = pickerY + 5.0F;
            if (GuiScreen.pickingSaturationBrightness) {
               float x = Math.max(0.0F, Math.min(mouseX - paletteX, paletteWidth));
               float y = Math.max(0.0F, Math.min(mouseY - paletteY, paletteHeight));
               hueSetting.saturation = x / paletteWidth;
               hueSetting.brightness = 1.0F - y / paletteHeight;
               if (FluxVisualsClient.get.configManager != null) {
                  FluxVisualsClient.get.configManager.autoSave();
               }

               return true;
            }

            if (GuiScreen.pickingHue) {
               float hueSliderWidth = 64.0F;
               float hueSliderHeight = 2.59F;
               float hueSliderX = pickerX + 5.0F;
               float hueSliderY = paletteY + paletteHeight + 5.0F;
               float huePos = Math.max(0.0F, Math.min(mouseX - hueSliderX, hueSliderWidth));
               hueSetting.current = huePos / hueSliderWidth * 106.0F;
               if (FluxVisualsClient.get.configManager != null) {
                  FluxVisualsClient.get.configManager.autoSave();
               }

               return true;
            }
         }
      }

      if (GuiScreen.activeSliderSetting instanceof SliderSetting sliderSetting) {
         float progress = (mouseX - GuiScreen.sliderX) / GuiScreen.sliderWidth;
         progress = Math.max(0.0F, Math.min(1.0F, progress));
         sliderSetting.current = sliderSetting.minimum + (sliderSetting.maximum - sliderSetting.minimum) * progress;
         if (FluxVisualsClient.get.configManager != null) {
            FluxVisualsClient.get.configManager.autoSave();
         }

         return true;
      }

      return false;
   }

   public void close() {
      MovementManager.getInstance().unlockMovement("Search");
      GuiScreen.activeSearch = false;
      GuiScreen.searchText = "";
      FluxVisualsClient.get.guiManager.setGuiCategory(GuiScreen.selectedCategories);
      this.releaseMovementKeys();
      super.close();
   }

   public void tick() {
      super.tick();
      this.syncMovementKeys();
      if (GuiScreen.exit && GuiScreen.alphaPC.isFinished()) {
         this.close();
         GuiScreen.exit = false;
      }
   }

   private void syncMovementKeys() {
      if (this.mc == null || this.mc.options == null || this.mc.getWindow() == null) {
         return;
      }
      long handle = this.mc.getWindow().getHandle();
      if (handle == 0L) {
         return;
      }
      KeyBinding[] keys = new KeyBinding[]{
         this.mc.options.forwardKey,
         this.mc.options.backKey,
         this.mc.options.leftKey,
         this.mc.options.rightKey,
         this.mc.options.jumpKey,
         this.mc.options.sprintKey
      };
      for (KeyBinding key : keys) {
         int code = key.getDefaultKey().getCode();
         boolean pressed = InputUtil.isKeyPressed(this.mc.getWindow(), code);
         key.setPressed(pressed);
      }
   }

   private void releaseMovementKeys() {
      if (this.mc == null || this.mc.options == null) {
         return;
      }
      KeyBinding[] keys = new KeyBinding[]{
         this.mc.options.forwardKey,
         this.mc.options.backKey,
         this.mc.options.leftKey,
         this.mc.options.rightKey,
         this.mc.options.jumpKey,
         this.mc.options.sprintKey
      };
      for (KeyBinding key : keys) {
         key.setPressed(false);
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void init() {
      super.init();
      this.themeScreen = new ThemeScreen();
      GuiInit.init();
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.mouse != null) {
         client.mouse.unlockCursor();
      }

      GuiScreen.categories = Category.values();
      GuiScreen.themes = Theme.values();
      GuiScreen.width = ru.fluxvisuals.ui.gui.GuiLayout.WIDTH;
      GuiScreen.height = ru.fluxvisuals.ui.gui.GuiLayout.HEIGHT;
      GuiScreen.x = 480.0F - GuiScreen.width / 2.0F;
      GuiScreen.y = 260.0F - GuiScreen.height / 2.0F;
      GuiScreen.mainAnimation.reset();
      GuiScreen.categoryAnimation.reset();
      if (FluxVisualsClient.get.guiManager == null) {
         FluxVisualsClient.get.guiManager = new GuiManager();
         FluxVisualsClient.get.guiManager.init();
      }

      GuiScreen.selectedTheme = FluxVisualsClient.get.guiManager.getCurrentTheme();
      GuiScreen.preSelectedTheme = FluxVisualsClient.get.guiManager.getCurrentTheme();
      GuiScreen.selectedCategories = FluxVisualsClient.get.guiManager.getCurrentCategory();
      if (FluxVisualsClient.get.manager == null) {
         FluxVisualsClient.get.manager = new Manager();
      }

      GuiScreen.modules = FluxVisualsClient.get.manager.getType(GuiScreen.selectedCategories);
   }
}
