package ru.fluxvisuals.utils.cosmetics.bbmodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Vec2;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Vec3;

/** Draws parsed .bbmodel geometry through Minecraft's entity render pipeline. */
@Environment(EnvType.CLIENT)
public final class BbModelRenderer {
   private static final float PIXELS_PER_BLOCK = 16.0F;
   private final BbTextureCache textures;

   public BbModelRenderer(BbTextureCache textures) {
      this.textures = textures;
   }

   public void render(BbModel model, String modelKey, MatrixStack matrices,
         OrderedRenderCommandQueue queue, int light, int overlay) {
      matrices.push();
      matrices.scale(1.0F / PIXELS_PER_BLOCK, 1.0F / PIXELS_PER_BLOCK, 1.0F / PIXELS_PER_BLOCK);
      for (BbNode root : model.roots()) {
         renderNode(root, model, modelKey, matrices, queue, light, overlay);
      }
      matrices.pop();
   }

   private void renderNode(BbNode node, BbModel model, String modelKey, MatrixStack matrices,
         OrderedRenderCommandQueue queue, int light, int overlay) {
      matrices.push();
      Vec3 origin = node.origin();
      if (origin.x() != 0.0F || origin.y() != 0.0F || origin.z() != 0.0F) {
         matrices.translate(origin.x(), origin.y(), origin.z());
         applyRotation(matrices, node.renderRotation());
         matrices.translate(-origin.x(), -origin.y(), -origin.z());
      } else {
         applyRotation(matrices, node.renderRotation());
      }
      Vec3 position = node.renderPosition();
      if (position.x() != 0.0F || position.y() != 0.0F || position.z() != 0.0F) {
         matrices.translate(position.x(), position.y(), position.z());
      }
      Vec3 scale = node.renderScale();
      if (scale.x() != 1.0F || scale.y() != 1.0F || scale.z() != 1.0F) {
         matrices.scale(scale.x(), scale.y(), scale.z());
      }

      if (node.visible() && node.element() != null) {
         renderElement(node.element(), model, modelKey, matrices, queue, light, overlay);
      }
      for (BbNode child : node.children()) {
         renderNode(child, model, modelKey, matrices, queue, light, overlay);
      }
      matrices.pop();
   }

   private static void applyRotation(MatrixStack matrices, Vec3 rotation) {
      if (rotation.x() == 0.0F && rotation.y() == 0.0F && rotation.z() == 0.0F) {
         return;
      }
      matrices.multiply(new Quaternionf().rotationXYZ(
            (float) Math.toRadians(rotation.x()),
            (float) Math.toRadians(rotation.y()),
            (float) Math.toRadians(rotation.z())));
   }

   private void renderElement(BbElement element, BbModel model, String modelKey, MatrixStack matrices,
         OrderedRenderCommandQueue queue, int light, int overlay) {
      if (!element.visible()) {
         return;
      }
      Map<Integer, List<BbElement.Face>> byTexture = new LinkedHashMap<>();
      for (BbElement.Face face : element.faces()) {
         byTexture.computeIfAbsent(face.texture(), ignored -> new ArrayList<>()).add(face);
      }
      for (Map.Entry<Integer, List<BbElement.Face>> entry : byTexture.entrySet()) {
         int index = entry.getKey();
         Identifier texture = index >= 0 && index < model.textures().size()
               ? textures.texture(modelKey, index, model.textures().get(index))
               : textures.fallback();
         List<BbElement.Face> faces = entry.getValue();
         queue.submitCustom(matrices, RenderLayers.entityCutoutNoCull(texture), (matrixEntry, consumer) -> {
            for (BbElement.Face face : faces) {
               emitFace(consumer, matrixEntry, element, face, light, overlay);
            }
         });
      }
   }

   private static void emitFace(VertexConsumer consumer, MatrixStack.Entry entry, BbElement element,
         BbElement.Face face, int light, int overlay) {
      List<String> ids = face.vertices();
      Vec3 normal = face.direction().equals("mesh") ? meshNormal(element, ids) : directionNormal(face.direction());
      if (ids.size() == 3) {
         emitVertex(consumer, entry, element, face, ids.get(0), normal, light, overlay);
         emitVertex(consumer, entry, element, face, ids.get(1), normal, light, overlay);
         emitVertex(consumer, entry, element, face, ids.get(2), normal, light, overlay);
      } else if (ids.size() >= 4) {
         emitVertex(consumer, entry, element, face, ids.get(0), normal, light, overlay);
         emitVertex(consumer, entry, element, face, ids.get(1), normal, light, overlay);
         emitVertex(consumer, entry, element, face, ids.get(2), normal, light, overlay);
         emitVertex(consumer, entry, element, face, ids.get(0), normal, light, overlay);
         emitVertex(consumer, entry, element, face, ids.get(2), normal, light, overlay);
         emitVertex(consumer, entry, element, face, ids.get(3), normal, light, overlay);
      }
   }

   private static void emitVertex(VertexConsumer consumer, MatrixStack.Entry entry, BbElement element,
         BbElement.Face face, String id, Vec3 normal, int light, int overlay) {
      Vec3 position = element.vertices().get(id);
      if (position == null) {
         return;
      }
      Vec2 uv = face.uvs().getOrDefault(id, Vec2.of(0.0F, 0.0F));
      consumer.vertex(entry, position.x(), position.y(), position.z())
         .color(0xFFFFFFFF)
         .texture(uv.x(), uv.y())
         .overlay(overlay)
         .light(light)
         .normal(entry, normal.x(), normal.y(), normal.z());
   }

   private static Vec3 directionNormal(String direction) {
      return switch (direction) {
         case "down" -> Vec3.of(0.0F, -1.0F, 0.0F);
         case "up" -> Vec3.of(0.0F, 1.0F, 0.0F);
         case "north" -> Vec3.of(0.0F, 0.0F, -1.0F);
         case "south" -> Vec3.of(0.0F, 0.0F, 1.0F);
         case "west" -> Vec3.of(-1.0F, 0.0F, 0.0F);
         case "east" -> Vec3.of(1.0F, 0.0F, 0.0F);
         default -> Vec3.of(0.0F, 1.0F, 0.0F);
      };
   }

   private static Vec3 meshNormal(BbElement element, List<String> ids) {
      if (ids.size() < 3) {
         return Vec3.of(0.0F, 1.0F, 0.0F);
      }
      Vec3 a = element.vertices().get(ids.get(0));
      Vec3 b = element.vertices().get(ids.get(1));
      Vec3 c = element.vertices().get(ids.get(2));
      if (a == null || b == null || c == null) {
         return Vec3.of(0.0F, 1.0F, 0.0F);
      }
      float x1 = b.x() - a.x();
      float y1 = b.y() - a.y();
      float z1 = b.z() - a.z();
      float x2 = c.x() - a.x();
      float y2 = c.y() - a.y();
      float z2 = c.z() - a.z();
      float nx = y1 * z2 - z1 * y2;
      float ny = z1 * x2 - x1 * z2;
      float nz = x1 * y2 - y1 * x2;
      float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
      if (length < 1.0e-6F) {
         return Vec3.of(0.0F, 1.0F, 0.0F);
      }
      return Vec3.of(nx / length, ny / length, nz / length);
   }
}
