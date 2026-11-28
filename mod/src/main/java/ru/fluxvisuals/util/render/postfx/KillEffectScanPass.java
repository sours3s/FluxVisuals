package ru.fluxvisuals.util.render.postfx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import ru.fluxvisuals.util.render.backends.gl.GlState;
import ru.fluxvisuals.util.render.backends.gl.ShaderProgram;
import ru.fluxvisuals.util.render.utils.FramebufferUtils;

/**
 * Прямой порт эталонного fullscreen-pass `frag_effect_scan`: тот же шейдер и
 * UBO, глубина основного framebuffer, аддитивный вывод поверх сцены.
 */
@Environment(EnvType.CLIENT)
public final class KillEffectScanPass {
   private static final int UNIFORM_BLOCK_BYTES = 240;
   private static KillEffectScanPass instance;

   private final ShaderProgram program = ShaderProgram.fromResources(
         "assets/fluxvisuals/shaders/effects/frag_effect_scan/frag_effect_scan.vsh",
         "assets/fluxvisuals/shaders/effects/frag_effect_scan/frag_effect_scan.fsh");
   private final ShaderProgram copyProgram = ShaderProgram.fromResources(
         "assets/fluxvisuals/shaders/blur/blur_fullscreen.vert",
         "assets/fluxvisuals/shaders/postfx/fog_copy.frag");
   private final FullScreenQuad quad = new FullScreenQuad();
   private final RenderTarget target = new RenderTarget();
   private final int uniformBuffer;
   private final FloatBuffer uniformData = ByteBuffer.allocateDirect(UNIFORM_BLOCK_BYTES)
         .order(ByteOrder.nativeOrder()).asFloatBuffer();
   private final int depthSamplerLoc;
   private final int copySourceLoc;
   private int compositeFbo;
   private boolean destroyed;

   private KillEffectScanPass() {
      int blockIndex = GL31.glGetUniformBlockIndex(this.program.id(), "Uniforms");
      if (blockIndex < 0) {
         throw new IllegalStateException("frag_effect_scan: uniform block not found");
      }
      this.uniformBuffer = GL15.glGenBuffers();
      GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, this.uniformBuffer);
      GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, UNIFORM_BLOCK_BYTES, GL15.GL_DYNAMIC_DRAW);
      GL31.glUniformBlockBinding(this.program.id(), blockIndex, 0);
      GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 0, this.uniformBuffer);
      GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
      this.depthSamplerLoc = this.program.getUniformLocation("DepthSampler");
      this.copySourceLoc = this.copyProgram.getUniformLocation("uSource");
   }

   public static void render(
         Framebuffer framebuffer,
         Matrix4f positionMatrix,
         Matrix4f projectionMatrix,
         Camera camera,
         Vec3d center,
         float radius,
         float width,
         int outerColor,
         int midColor,
         int innerColor,
         int scanlineColor,
         float progress,
         float flick
   ) {
      if (instance == null) {
         instance = new KillEffectScanPass();
      }
      instance.renderImpl(framebuffer, positionMatrix, projectionMatrix, camera, center, radius, width,
            outerColor, midColor, innerColor, scanlineColor, progress, flick);
   }

   public static void clear() {
      if (instance != null && !instance.destroyed) {
         instance.destroyed = true;
         instance.quad.destroy();
         instance.target.destroy();
         if (instance.compositeFbo != 0) {
            GL30.glDeleteFramebuffers(instance.compositeFbo);
            instance.compositeFbo = 0;
         }
         GL15.glDeleteBuffers(instance.uniformBuffer);
         instance.program.delete();
         instance.copyProgram.delete();
         instance = null;
      }
   }

   private void renderImpl(
         Framebuffer framebuffer,
         Matrix4f positionMatrix,
         Matrix4f projectionMatrix,
         Camera camera,
         Vec3d center,
         float radius,
         float width,
         int outerColor,
         int midColor,
         int innerColor,
         int scanlineColor,
         float progress,
         float flick
   ) {
      if (this.destroyed || framebuffer == null || camera == null || center == null
            || positionMatrix == null || projectionMatrix == null
            || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
         return;
      }
      int depthTexture = FramebufferUtils.getDepthTextureId(framebuffer);
      int colorTexture = FramebufferUtils.getColorTextureId(framebuffer);
      if (depthTexture <= 0 || colorTexture <= 0) {
         return;
      }
      int widthPx = framebuffer.textureWidth;
      int heightPx = framebuffer.textureHeight;
      this.quad.ensure();
      this.target.ensure(widthPx, heightPx);

      Matrix4f inverseProjection = new Matrix4f(projectionMatrix).invert();
      Matrix4f inverseView = new Matrix4f(positionMatrix).invert();
      GlState.Snapshot snapshot = GlState.push();
      try {
         GL11.glDisable(GL11.GL_CULL_FACE);
         GL11.glDisable(GL11.GL_DEPTH_TEST);
         GL11.glDisable(GL11.GL_BLEND);
         writeUniforms(inverseProjection, inverseView, camera.getCameraPos(), center, radius, width,
               outerColor, midColor, innerColor, scanlineColor, progress, flick);

         GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.target.fbo);
         GL11.glViewport(0, 0, widthPx, heightPx);
         GL11.glDrawBuffer(36064);
         this.program.use();
         if (this.depthSamplerLoc >= 0) {
            GL20.glUniform1i(this.depthSamplerLoc, 0);
         }
         GL13.glActiveTexture(GL13.GL_TEXTURE0);
         GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
         this.quad.bindAndDraw();
         GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
         GL20.glUseProgram(0);

         if (this.compositeFbo == 0) {
            this.compositeFbo = GL30.glGenFramebuffers();
         }
         GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.compositeFbo);
         GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, 36064, GL11.GL_TEXTURE_2D, colorTexture, 0);
         if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE) {
            GL11.glViewport(0, 0, widthPx, heightPx);
            GL11.glDrawBuffer(36064);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFunc(GL14.GL_SRC_ALPHA, GL14.GL_ONE);
            this.copyProgram.use();
            if (this.copySourceLoc >= 0) {
               GL20.glUniform1i(this.copySourceLoc, 0);
            }
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.target.colorTex);
            this.quad.bindAndDraw();
         }
         GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, 36064, GL11.GL_TEXTURE_2D, 0, 0);
      } finally {
         GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
         GL20.glUseProgram(0);
         GlState.pop(snapshot);
      }
   }

   private void writeUniforms(
         Matrix4f inverseProjection,
         Matrix4f inverseView,
         Vec3d cameraPos,
         Vec3d center,
         float radius,
         float width,
         int outerColor,
         int midColor,
         int innerColor,
         int scanlineColor,
         float progress,
         float flick
   ) {
      FloatBuffer data = this.uniformData;
      data.clear();
      inverseProjection.get(data);
      inverseView.get(data);
      data.put((float) cameraPos.x).put((float) cameraPos.y).put((float) cameraPos.z).put(radius);
      data.put((float) center.x).put((float) center.y).put((float) center.z).put(Math.max(0.0001F, width));
      putColor(data, outerColor);
      putColor(data, midColor);
      putColor(data, innerColor);
      putColor(data, scanlineColor);
      data.put(16.0F).put(progress).put(flick).put(0.0F);
      data.flip();
      GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, this.uniformBuffer);
      GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, data);
      GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
   }

   private static void putColor(FloatBuffer data, int argb) {
      data.put(((argb >> 16) & 0xFF) / 255.0F);
      data.put(((argb >> 8) & 0xFF) / 255.0F);
      data.put((argb & 0xFF) / 255.0F);
      data.put(((argb >>> 24) & 0xFF) / 255.0F);
   }
}
