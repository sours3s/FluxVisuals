package ru.fluxvisuals.utils.cosmetics.bbmodel;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbElement.Face;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Animation;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Animator;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Keyframe;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Texture;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Vec2;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Vec3;

/** Reads Blockbench .bbmodel files into the runtime representation. */
@Environment(EnvType.CLIENT)
public final class BbModelLoader {
   private static final Gson GSON = new Gson();
   private static final Base64.Decoder BASE64 = Base64.getDecoder();

   public BbModelLoader() {
   }

   public BbModel load(Path path) throws IOException {
      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         return parse(JsonParser.parseReader(reader).getAsJsonObject());
      }
   }

   public BbModel parse(JsonObject root) {
      Vec2 resolution = vec2(root.get("resolution"), 16.0F, 16.0F);
      List<Texture> textures = parseTextures(root.getAsJsonArray("textures"));
      Map<UUID, BbElement> elements = parseElements(root.getAsJsonArray("elements"), textures, resolution);
      List<Animation> animations = parseAnimations(root.getAsJsonArray("animations"));

      List<BbNode> roots = new ArrayList<>();
      Map<UUID, BbNode> nodes = new HashMap<>();
      for (JsonElement item : arrayOrEmpty(root.get("outliner"))) {
         BbNode node = parseOutlinerItem(item, elements, nodes);
         if (node != null) {
            roots.add(node);
         }
      }
      for (Map.Entry<UUID, BbElement> entry : elements.entrySet()) {
         if (!nodes.containsKey(entry.getKey())) {
            BbElement element = entry.getValue();
            BbNode node = new BbNode(element.name(), entry.getKey(), element.origin(), element.rotation(),
                  element.visible(), List.of(), element);
            nodes.put(entry.getKey(), node);
            roots.add(node);
         }
      }

      String id = string(root.get("model_identifier"), root.get("name"), "");
      return new BbModel(id, resolution, textures, animations, roots, nodes);
   }

   private static List<Texture> parseTextures(JsonArray array) {
      List<Texture> result = new ArrayList<>();
      for (JsonElement element : arrayOrEmpty(array)) {
         if (!element.isJsonObject()) {
            continue;
         }
         JsonObject object = element.getAsJsonObject();
         int width = (int) number(object.get("width"), 1.0F);
         int height = (int) number(object.get("height"), 1.0F);
         byte[] png = decodeSource(string(object.get("source"), ""));
         result.add(new Texture(Math.max(width, 1), Math.max(height, 1), png));
      }
      return result;
   }

   private static byte[] decodeSource(String source) {
      if (source == null) {
         return new byte[0];
      }
      String value = source.trim();
      if (!value.startsWith("data:")) {
         return new byte[0];
      }
      int comma = value.indexOf(',');
      if (comma < 0) {
         return new byte[0];
      }
      try {
         return BASE64.decode(value.substring(comma + 1));
      } catch (IllegalArgumentException ignored) {
         return new byte[0];
      }
   }

   private static Map<UUID, BbElement> parseElements(JsonArray array, List<Texture> textures, Vec2 resolution) {
      Map<UUID, BbElement> result = new LinkedHashMap<>();
      for (JsonElement element : arrayOrEmpty(array)) {
         if (!element.isJsonObject()) {
            continue;
         }
         JsonObject object = element.getAsJsonObject();
         UUID uuid = uuid(object.get("uuid"));
         if (uuid == null) {
            continue;
         }
         String type = string(object.get("type"), "cube").toLowerCase(Locale.ROOT);
         Vec3 from = vec3(object.get("from"), Vec3.ZERO);
         Vec3 to = vec3(object.get("to"), Vec3.ZERO);
         Vec3 origin = vec3(object.get("origin"), Vec3.ZERO);
         Vec3 rotation = vec3(object.get("rotation"), Vec3.ZERO);
         boolean visible = bool(object.get("visibility"), object.get("visible"), true);

         Map<String, Vec3> vertices = new LinkedHashMap<>();
         List<Face> faces = new ArrayList<>();
         if ("mesh".equals(type)) {
            parseMesh(object, textures, resolution, vertices, faces);
         } else {
            parseCube(object, from, to, textures, resolution, vertices, faces);
         }
         result.put(uuid, new BbElement(string(object.get("name"), ""), type, from, to, origin, rotation,
               visible, vertices, faces));
      }
      return result;
   }

   private static void parseCube(JsonObject object, Vec3 from, Vec3 to, List<Texture> textures, Vec2 resolution,
         Map<String, Vec3> vertices, List<Face> faces) {
      float inflate = number(object.get("inflate"), 0.0F);
      float x0 = from.x() - inflate;
      float y0 = from.y() - inflate;
      float z0 = from.z() - inflate;
      float x1 = to.x() + inflate;
      float y1 = to.y() + inflate;
      float z1 = to.z() + inflate;
      vertices.put("000", Vec3.of(x0, y0, z0));
      vertices.put("001", Vec3.of(x0, y0, z1));
      vertices.put("010", Vec3.of(x0, y1, z0));
      vertices.put("011", Vec3.of(x0, y1, z1));
      vertices.put("100", Vec3.of(x1, y0, z0));
      vertices.put("101", Vec3.of(x1, y0, z1));
      vertices.put("110", Vec3.of(x1, y1, z0));
      vertices.put("111", Vec3.of(x1, y1, z1));

      // Same corner order as Minecraft's CubeFace, so vanilla-style UV mapping applies.
      String[][] corners = {
            { "001", "000", "100", "101" }, // down
            { "010", "011", "111", "110" }, // up
            { "110", "100", "000", "010" }, // north
            { "011", "001", "101", "111" }, // south
            { "010", "000", "001", "011" }, // west
            { "111", "101", "100", "110" }  // east
      };
      JsonObject faceObject = object.getAsJsonObject("faces");
      if (faceObject == null) {
         return;
      }
      for (Map.Entry<String, JsonElement> entry : faceObject.entrySet()) {
         String direction = entry.getKey().toLowerCase(Locale.ROOT);
         int cornerIndex = switch (direction) {
            case "down" -> 0;
            case "up" -> 1;
            case "north" -> 2;
            case "south" -> 3;
            case "west" -> 4;
            case "east" -> 5;
            default -> -1;
         };
         if (cornerIndex < 0 || !entry.getValue().isJsonObject()) {
            continue;
         }
         JsonObject face = entry.getValue().getAsJsonObject();
         float[] uv = floats(face.get("uv"), 4, 0.0F);
         int texture = (int) number(face.get("texture"), -1.0F);
         Vec2 size = textureSize(textures, resolution, texture);
         String[] keys = corners[cornerIndex];
         List<String> faceVertices = List.of(keys);
         Map<String, Vec2> faceUvs = new LinkedHashMap<>();
         for (int i = 0; i < 4; i++) {
            float u = (i == 0 || i == 1) ? uv[0] : uv[2];
            float v = (i == 0 || i == 3) ? uv[3] : uv[1];
            faceUvs.put(keys[i], Vec2.of(u / size.x(), v / size.y()));
         }
         faces.add(new Face(direction, texture, faceVertices, faceUvs));
      }
   }

   private static void parseMesh(JsonObject object, List<Texture> textures, Vec2 resolution,
         Map<String, Vec3> vertices, List<Face> faces) {
      JsonObject vertexObject = object.getAsJsonObject("vertices");
      if (vertexObject != null) {
         for (Map.Entry<String, JsonElement> entry : vertexObject.entrySet()) {
            vertices.put(entry.getKey(), vec3(entry.getValue(), Vec3.ZERO));
         }
      }
      JsonObject faceObject = object.getAsJsonObject("faces");
      if (faceObject == null) {
         return;
      }
      for (Map.Entry<String, JsonElement> entry : faceObject.entrySet()) {
         if (!entry.getValue().isJsonObject()) {
            continue;
         }
         JsonObject face = entry.getValue().getAsJsonObject();
         int texture = (int) number(face.get("texture"), -1.0F);
         Vec2 size = textureSize(textures, resolution, texture);
         List<String> faceVertices = strings(face.get("vertices"));
         if (faceVertices.size() < 3 || faceVertices.size() > 4) {
            continue;
         }
         Map<String, Vec2> faceUvs = new LinkedHashMap<>();
         JsonObject uvObject = face.getAsJsonObject("uv");
         if (uvObject == null) {
            continue;
         }
         for (String vertex : faceVertices) {
            JsonElement uvElement = uvObject.get(vertex);
            if (uvElement == null) {
               continue;
            }
            float[] values = floats(uvElement, 2, 0.0F);
            faceUvs.put(vertex, Vec2.of(values[0] / size.x(), values[1] / size.y()));
         }
         if (faceUvs.size() == faceVertices.size()) {
            faces.add(new Face("mesh", texture, faceVertices, faceUvs));
         }
      }
   }

   private static Vec2 textureSize(List<Texture> textures, Vec2 resolution, int texture) {
      if (texture >= 0 && texture < textures.size()) {
         Texture value = textures.get(texture);
         return Vec2.of(value.width(), value.height());
      }
      return resolution;
   }

   private static List<Animation> parseAnimations(JsonArray array) {
      List<Animation> result = new ArrayList<>();
      for (JsonElement element : arrayOrEmpty(array)) {
         if (!element.isJsonObject()) {
            continue;
         }
         JsonObject object = element.getAsJsonObject();
         Map<UUID, Animator> animators = new LinkedHashMap<>();
         JsonObject animatorObject = object.getAsJsonObject("animators");
         if (animatorObject != null) {
            for (Map.Entry<String, JsonElement> entry : animatorObject.entrySet()) {
               UUID uuid = uuid(entry.getKey());
               if (uuid == null || !entry.getValue().isJsonObject()) {
                  continue;
               }
               List<Keyframe> keyframes = new ArrayList<>();
               for (JsonElement keyframeElement : arrayOrEmpty(entry.getValue().getAsJsonObject().get("keyframes"))) {
                  Keyframe keyframe = parseKeyframe(keyframeElement);
                  if (keyframe != null) {
                     keyframes.add(keyframe);
                  }
               }
               animators.put(uuid, new Animator(uuid, keyframes));
            }
         }
         result.add(new Animation(string(object.get("name"), ""), number(object.get("length"), 0.0F),
               bool(object.get("loop"), false), animators));
      }
      return result;
   }

   private static Keyframe parseKeyframe(JsonElement element) {
      if (!element.isJsonObject()) {
         return null;
      }
      JsonObject object = element.getAsJsonObject();
      String channel = string(object.get("channel"), "").toLowerCase(Locale.ROOT);
      if (!channel.equals("rotation") && !channel.equals("position") && !channel.equals("scale")) {
         return null;
      }
      JsonArray points = object.getAsJsonArray("data_points");
      JsonElement point = points != null && points.size() > 0 ? points.get(0) : null;
      Vec3 fallback = "scale".equals(channel) ? Vec3.ONE : Vec3.ZERO;
      Vec3 value = keyframeVector(point, fallback);
      return new Keyframe(channel, number(object.get("time"), 0.0F), value,
            string(object.get("interpolation"), "linear").toLowerCase(Locale.ROOT));
   }

   /** Blockbench stores keyframe points as objects with x/y/z members, not arrays. */
   private static Vec3 keyframeVector(JsonElement element, Vec3 fallback) {
      if (element == null) {
         return fallback;
      }
      if (element.isJsonArray()) {
         return vec3(element, fallback);
      }
      if (!element.isJsonObject()) {
         return fallback;
      }
      JsonObject object = element.getAsJsonObject();
      return Vec3.of(
            number(object.get("x"), fallback.x()),
            number(object.get("y"), fallback.y()),
            number(object.get("z"), fallback.z()));
   }

   private static BbNode parseOutlinerItem(JsonElement element, Map<UUID, BbElement> elements,
         Map<UUID, BbNode> nodes) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
         UUID uuid = uuid(element.getAsString());
         BbElement cosmeticElement = uuid == null ? null : elements.get(uuid);
         if (cosmeticElement == null) {
            return null;
         }
         BbNode node = new BbNode(cosmeticElement.name(), uuid, cosmeticElement.origin(),
               cosmeticElement.rotation(), cosmeticElement.visible(), List.of(), cosmeticElement);
         nodes.putIfAbsent(uuid, node);
         return node;
      }
      if (!element.isJsonObject()) {
         return null;
      }
      JsonObject object = element.getAsJsonObject();
      UUID uuid = uuid(object.get("uuid"));
      List<BbNode> children = new ArrayList<>();
      for (JsonElement child : arrayOrEmpty(object.get("children"))) {
         BbNode node = parseOutlinerItem(child, elements, nodes);
         if (node != null) {
            children.add(node);
         }
      }
      BbNode node = new BbNode(string(object.get("name"), ""), uuid,
            vec3(object.get("origin"), Vec3.ZERO), vec3(object.get("rotation"), Vec3.ZERO),
            bool(object.get("visibility"), true), children, null);
      if (uuid != null) {
         nodes.putIfAbsent(uuid, node);
      }
      return node;
   }

   private static JsonArray arrayOrEmpty(JsonElement element) {
      return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
   }

   private static UUID uuid(JsonElement element) {
      if (element == null || !element.isJsonPrimitive()) {
         return null;
      }
      try {
         return UUID.fromString(element.getAsString());
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }

   private static UUID uuid(String value) {
      if (value == null) {
         return null;
      }
      try {
         return UUID.fromString(value);
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }

   private static String string(JsonElement element, String fallback) {
      return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
   }

   private static String string(JsonElement first, JsonElement second, String fallback) {
      String value = string(first, null);
      return value != null && !value.isBlank() ? value : string(second, fallback);
   }

   private static float number(JsonElement element, float fallback) {
      if (element == null || !element.isJsonPrimitive()) {
         return fallback;
      }
      try {
         return element.getAsFloat();
      } catch (NumberFormatException ignored) {
         return fallback;
      }
   }

   private static boolean bool(JsonElement element, boolean fallback) {
      if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
         return fallback;
      }
      return element.getAsBoolean();
   }

   private static boolean bool(JsonElement first, JsonElement second, boolean fallback) {
      if (first != null && first.isJsonPrimitive() && first.getAsJsonPrimitive().isBoolean()) {
         return first.getAsBoolean();
      }
      return bool(second, fallback);
   }

   private static Vec2 vec2(JsonElement element, float fallbackX, float fallbackY) {
      float[] values = floats(element, 2, 0.0F);
      return Vec2.of(values[0] == 0.0F ? fallbackX : values[0], values[1] == 0.0F ? fallbackY : values[1]);
   }

   private static Vec3 vec3(JsonElement element, Vec3 fallback) {
      float[] values = floats(element, 3, Float.NaN);
      return Vec3.of(Float.isNaN(values[0]) ? fallback.x() : values[0],
            Float.isNaN(values[1]) ? fallback.y() : values[1],
            Float.isNaN(values[2]) ? fallback.z() : values[2]);
   }

   private static float[] floats(JsonElement element, int count, float fallback) {
      float[] result = new float[count];
      java.util.Arrays.fill(result, fallback);
      if (element == null || !element.isJsonArray()) {
         return result;
      }
      JsonArray array = element.getAsJsonArray();
      for (int i = 0; i < Math.min(count, array.size()); i++) {
         JsonElement value = array.get(i);
         if (value == null || !value.isJsonPrimitive()) {
            continue;
         }
         try {
            result[i] = Float.parseFloat(value.getAsString());
         } catch (NumberFormatException ignored) {
            // Keep the fallback value.
         }
      }
      return result;
   }

   private static List<String> strings(JsonElement element) {
      List<String> result = new ArrayList<>();
      for (JsonElement value : arrayOrEmpty(element)) {
         if (value.isJsonPrimitive()) {
            result.add(value.getAsString());
         }
      }
      return result;
   }

   /** Exposed for tools and tests that need the same Gson configuration. */
   public static Gson gson() {
      return GSON;
   }
}
