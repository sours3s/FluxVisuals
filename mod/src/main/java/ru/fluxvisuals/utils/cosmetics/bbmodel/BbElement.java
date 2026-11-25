package ru.fluxvisuals.utils.cosmetics.bbmodel;

import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Vec2;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Vec3;

/** Cubes and mesh elements. Both are stored as faces over precomputed vertices. */
@Environment(EnvType.CLIENT)
public final class BbElement {
   public record Face(String direction, int texture, List<String> vertices, Map<String, Vec2> uvs) {
      public Face {
         vertices = List.copyOf(vertices);
         uvs = Map.copyOf(uvs);
      }
   }

   private final String name;
   private final String type;
   private final Vec3 from;
   private final Vec3 to;
   private final Vec3 origin;
   private final Vec3 rotation;
   private boolean visible;
   private final Map<String, Vec3> vertices;
   private final List<Face> faces;

   public BbElement(String name, String type, Vec3 from, Vec3 to, Vec3 origin, Vec3 rotation,
         boolean visible, Map<String, Vec3> vertices, List<Face> faces) {
      this.name = name == null ? "" : name;
      this.type = type == null ? "cube" : type;
      this.from = from;
      this.to = to;
      this.origin = origin;
      this.rotation = rotation;
      this.visible = visible;
      this.vertices = Map.copyOf(vertices);
      this.faces = List.copyOf(faces);
   }

   public String name() {
      return name;
   }

   public String type() {
      return type;
   }

   public Vec3 from() {
      return from;
   }

   public Vec3 to() {
      return to;
   }

   public Vec3 origin() {
      return origin;
   }

   public Vec3 rotation() {
      return rotation;
   }

   public boolean visible() {
      return visible;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   public Map<String, Vec3> vertices() {
      return vertices;
   }

   public List<Face> faces() {
      return faces;
   }
}
