package ru.fluxvisuals.util.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.ui.draggable.DraggableManager;
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
      FluxVisualsClient.ensureRendererInitialized();
      int width = client.getWindow().getFramebufferWidth();
      int height = client.getWindow().getFramebufferHeight();
      if (width <= 0 || height <= 0) {
         return;
      }

      Framebuffer mainFramebuffer = client.getFramebuffer();
      int tempFbo = 0;
      int savedDrawFbo = GL11.glGetInteger(36009);
      int savedReadFbo = GL11.glGetInteger(36008);
      int savedFbo = GL11.glGetInteger(36006);
      if (mainFramebuffer != null) {
         int mainFramebufferTextureId = ru.fluxvisuals.util.render.utils.FramebufferUtils.getColorTextureId(mainFramebuffer);
         if (mainFramebufferTextureId > 0) {
            tempFbo = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(36160, tempFbo);
            GL30.glFramebufferTexture2D(36160, 36064, 3553, mainFramebufferTextureId, 0);
            GL11.glDrawBuffer(36064);
            int status = GL30.glCheckFramebufferStatus(36160);
            if (status != 36053) {
               GL30.glDeleteFramebuffers(tempFbo);
               tempFbo = 0;
               GL30.glBindFramebuffer(36160, savedFbo);
            }
         } else {
            GL30.glBindFramebuffer(36160, 0);
         }
      } else {
         GL30.glBindFramebuffer(36160, 0);
      }

      GL11.glColorMask(true, true, true, true);
      GL11.glDisable(2929);
      GL11.glEnable(3042);
      GlState.Snapshot snapshot = GlState.push();

      try {
         AnimationSystem.getInstance().tick();
         Renderer2D renderer = FluxVisualsClient.getRenderer();
         if (renderer != null) {
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
            }
         }
      } finally {
         if (snapshot != null) {
            GL20.glUseProgram(snapshot.program);
            GL30.glBindVertexArray(snapshot.vao);
            GL15.glBindBuffer(34962, snapshot.arrayBuffer);
            GL15.glBindBuffer(34963, snapshot.elementArrayBuffer);
            GL13.glActiveTexture(snapshot.activeTexture);
            GL11.glBindTexture(3553, snapshot.texture2D);
            GL11.glPixelStorei(3317, snapshot.unpackAlignment);
            setEnabled(3089, snapshot.scissorEnabled);
            setEnabled(2929, snapshot.depthTestEnabled);
            setEnabled(2884, snapshot.cullFaceEnabled);
            setEnabled(3042, snapshot.blendEnabled);
            setEnabled(36281, snapshot.framebufferSrgbEnabled);
            GL14.glBlendFuncSeparate(snapshot.blendSrcRGB, snapshot.blendDstRGB, snapshot.blendSrcAlpha, snapshot.blendDstAlpha);
            GL11.glColorMask(snapshot.colorMaskR, snapshot.colorMaskG, snapshot.colorMaskB, snapshot.colorMaskA);
            GL11.glDepthMask(snapshot.depthMask);
            GL11.glViewport(snapshot.viewport[0], snapshot.viewport[1], snapshot.viewport[2], snapshot.viewport[3]);
            GL11.glScissor(snapshot.scissorBox[0], snapshot.scissorBox[1], snapshot.scissorBox[2], snapshot.scissorBox[3]);
         }

         if (tempFbo != 0) {
            GL30.glBindFramebuffer(36160, tempFbo);
            GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
         }

         GL30.glBindFramebuffer(36009, savedDrawFbo);
         GL30.glBindFramebuffer(36008, savedReadFbo);
         GL30.glBindFramebuffer(36160, savedFbo);
         if (tempFbo != 0) {
            GL30.glDeleteFramebuffers(tempFbo);
         }
      }
   }

   private static void setEnabled(int cap, boolean enabled) {
      if (enabled) {
         GL11.glEnable(cap);
      } else {
         GL11.glDisable(cap);
      }
   }
}
