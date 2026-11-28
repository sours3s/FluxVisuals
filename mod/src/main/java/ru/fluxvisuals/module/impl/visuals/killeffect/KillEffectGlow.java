package ru.fluxvisuals.module.impl.visuals.killeffect;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderer;
import ru.fluxvisuals.util.render.world.WorldRenderLayers;

/** Прямой порт appendGlow из эталонного KillEffect$LaserEffect. */
@Environment(EnvType.CLIENT)
public final class KillEffectGlow {
   /** BLOOM_TEX эталона: fluxvisuals:textures/bloom.png. */
   private static final Identifier BLOOM_TEX = Identifier.of("fluxvisuals", "textures/bloom.png");

   private KillEffectGlow() {}

   public static VertexConsumer buffer(WorldRenderer renderer) {
      return renderer.getBuffer(WorldRenderLayers.POS_TEX_COLOR_QUADS_ADD(BLOOM_TEX));
   }

   /** Glow-квад, повёрнутый к камере: те же 4 вершины и UV, что в эталоне. */
   public static void appendGlow(WorldRenderer renderer, VertexConsumer buffer, Vec3d pos,
         float size, int r, int g, int b, float alpha) {
      if (alpha <= 0.01F || buffer == null || pos == null) {
         return;
      }
      Vec3d cam = renderer.camera().getCameraPos();
      renderer.matrixStack().push();
      try {
         renderer.matrixStack().translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
         renderer.matrixStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-renderer.camera().getYaw()));
         renderer.matrixStack().multiply(RotationAxis.POSITIVE_X.rotationDegrees(renderer.camera().getPitch()));
         Matrix4f matrix = renderer.matrixStack().peek().getPositionMatrix();
         float half = size * 0.5F;
         int ai = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
         buffer.vertex(matrix, -half, -half, 0.0F).texture(0.0F, 1.0F).color(r, g, b, ai);
         buffer.vertex(matrix, -half, half, 0.0F).texture(0.0F, 0.0F).color(r, g, b, ai);
         buffer.vertex(matrix, half, half, 0.0F).texture(1.0F, 0.0F).color(r, g, b, ai);
         buffer.vertex(matrix, half, -half, 0.0F).texture(1.0F, 1.0F).color(r, g, b, ai);
      } finally {
         renderer.matrixStack().pop();
      }
   }

   public static void draw(WorldRenderer renderer, VertexConsumer buffer, Vec3d pos,
         float size, int color, float alpha) {
      appendGlow(renderer, buffer, pos, size,
            Renderer2D.ColorUtil.red(color),
            Renderer2D.ColorUtil.green(color),
            Renderer2D.ColorUtil.blue(color), alpha);
   }
}
