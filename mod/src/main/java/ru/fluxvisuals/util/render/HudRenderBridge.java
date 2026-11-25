package ru.fluxvisuals.util.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.opengl.GL11;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.ui.fluxgui.util.render.FluxGlassPipeline;
import ru.fluxvisuals.util.render.animation.AnimationSystem;
import ru.fluxvisuals.util.render.backends.gl.GlState;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Единая обвязка для отрисовки HUD-оверлея FluxVisuals поверх кадра.
 *
 * <p>Используется из двух точек:
 * <ul>
 *   <li>{@code InGameHudMixin} — обычный ингейм-рендер (когда чат закрыт);</li>
 *   <li>{@code ChatScreenMixin} — чтобы HUD-элементы (бинды, потионсы, аррайлист…)
 *       были видны ПОВЕРХ чата, а не прятались за его тёмным фоном.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class HudRenderBridge {
   private HudRenderBridge() {
   }

   public static void renderHudOverlay(DrawContext context, RenderTickCounter tickCounter) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.player == null || client.world == null || client.getWindow() == null) {
         return;
      }
      if (!FluxVisualsClient.isModInitialized()) {
         return;
      }
      // Не рисуем HUD-оверлей поверх инвентаря/креатива/ClickGUI: это только мешает
      // отрисовке слотов и создаёт лишние render pass'ы. Чат — исключение, там HUD нужен.
      if (client.currentScreen != null && !(client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) {
         return;
      }
      FluxVisualsClient.ensureRendererInitialized();
      int width = client.getWindow().getFramebufferWidth();
      int height = client.getWindow().getFramebufferHeight();
      if (width <= 0 || height <= 0) {
         return;
      }

      // Раньше здесь создавался временный legacy-FBO с текстурой основного кадрбуфера.
      // В 1.21.11 иконки предметов рисуются из отложенной очереди DrawContext уже после
      // выхода из InGameHud.render, поэтому смена/удаление FBO между постановкой и
      // отрисовкой ломала состояние цели и приводила к пропаже иконок. Рисуем в текущий
      // кадрбуфер, который на этом этапе уже является основным.
      GL11.glColorMask(true, true, true, true);
      GL11.glDisable(2929);
      GL11.glEnable(3042);
      GlState.Snapshot snapshot = GlState.push();

      try {
         AnimationSystem.getInstance().tick();
         Renderer2D renderer = FluxVisualsClient.getRenderer();
         if (renderer != null) {
            // Снимок кадра для liquid-glass делает InGameHudMixin на HEAD render,
            // чтобы стекло размывало мир, а не уже нарисованный ванильный HUD.
            DraggableManager draggableManager = DraggableManager.getInstance();
            draggableManager.beginFrame(client, renderer, width, height);
            boolean rendererBegun = false;

            try {
               renderer.begin(width, height);
               rendererBegun = true;
               EventManager.call(new EventScreen(client, renderer, FontRegistry.INTER_MEDIUM, width, height, context));
            } finally {
               if (rendererBegun) {
                  renderer.end();
               }
               draggableManager.endFrame();
               // Разблокируем захват кадра для ClickGUI после отрисовки HUD.
               FluxGlassPipeline.beginRenderFrame();
            }
         }
      } finally {
         // Возвращаем состояние GL, включая привязку кадрбуфера (это чинит иконки),
         // но НЕ трогаем read/draw buffer: в 1.21.11 это ломает последующий GUI-проход
         // и даёт чёрный экран с жирным текстом.
         GlState.pop(snapshot, false);
      }
   }
}
