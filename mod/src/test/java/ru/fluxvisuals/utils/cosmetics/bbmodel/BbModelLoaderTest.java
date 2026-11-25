package ru.fluxvisuals.utils.cosmetics.bbmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Animation;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Keyframe;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Vec3;

class BbModelLoaderTest {
   private static final Path ROOT = Path.of("src", "main", "resources", "fluxvisuals", "cosmetics");

   @Test
   void parsesEveryBundledModel() throws IOException {
      BbModelLoader loader = new BbModelLoader();
      List<Path> models;
      try (Stream<Path> stream = Files.walk(ROOT)) {
         models = stream.filter(path -> path.toString().endsWith(".bbmodel")).sorted().toList();
      }
      assertFalse(models.isEmpty(), "No bundled .bbmodel files found");

      int cubes = 0;
      int meshes = 0;
      int meshesWithFaces = 0;
      for (Path path : models) {
         BbModel model = loader.load(path);
         assertNotNull(model, path.toString());
         assertFalse(model.roots().isEmpty(), "No outliner roots in " + path);
         for (BbNode node : collectNodes(model.roots())) {
            BbElement element = node.element();
            if (element == null) {
               continue;
            }
            if ("mesh".equals(element.type())) {
               meshes++;
               if (!element.faces().isEmpty()) {
                  meshesWithFaces++;
               }
            } else {
               cubes++;
            }
         }
      }
      assertEquals(68, models.size());
      assertEquals(2892, cubes);
      assertEquals(258, meshes);
      assertTrue(meshesWithFaces > 200, "Mesh faces were not parsed");
   }

   @Test
   void decodesEmbeddedTexturesAndAnimations() throws IOException {
      BbModelLoader loader = new BbModelLoader();
      Path scythe = ROOT.resolve("Cool scythe").resolve("model_1.bbmodel");
      assertTrue(Files.isRegularFile(scythe));
      BbModel model = loader.load(scythe);

      assertNotNull(model.animation("idle"));
      assertNotNull(model.animation("walk"));
      assertTrue(model.textures().stream().anyMatch(texture -> texture.png().length > 0));

      boolean foundMeshFace = false;
      for (BbNode node : collectNodes(model.roots())) {
         BbElement element = node.element();
         if (element == null || !"mesh".equals(element.type())) {
            continue;
         }
         for (BbElement.Face face : element.faces()) {
            assertEquals("mesh", face.direction());
            assertTrue(face.vertices().size() >= 3);
            assertEquals(face.vertices().size(), face.uvs().size());
            foundMeshFace = true;
         }
      }
      assertTrue(foundMeshFace);
   }

   @Test
   void parsesObjectKeyframePoints() {
      String json = """
            {
              "resolution": {"width": 16, "height": 16},
              "textures": [], "elements": [], "outliner": [],
              "animations": [{
                "name": "blink", "length": 0.25, "loop": false,
                "animators": {
                  "00000000-0000-0000-0000-000000000001": {
                    "keyframes": [
                      {"channel": "scale", "time": 0.125,
                       "data_points": [{"x": "1", "y": "0.75", "z": "1"}]},
                      {"channel": "position", "time": 0.125,
                       "data_points": [{"x": "2", "y": "-3", "z": "4"}]},
                      {"channel": "scale", "time": 0.5, "data_points": []}
                    ]
                  }
                }
              }]
            }
            """;
      BbModel model = new BbModelLoader().parse(JsonParser.parseString(json).getAsJsonObject());
      Animation animation = model.animation("blink");
      assertNotNull(animation);
      List<Keyframe> frames = animation.animators()
            .get(UUID.fromString("00000000-0000-0000-0000-000000000001")).keyframes();
      assertEquals(3, frames.size());
      assertEquals(Vec3.of(1.0F, 0.75F, 1.0F), frames.get(0).value());
      assertEquals(Vec3.of(2.0F, -3.0F, 4.0F), frames.get(1).value());
      assertEquals(Vec3.ONE, frames.get(2).value(), "Empty scale frames must not collapse the node");
   }

   private static List<BbNode> collectNodes(List<BbNode> roots) {
      java.util.List<BbNode> result = new java.util.ArrayList<>();
      for (BbNode node : roots) {
         result.add(node);
         result.addAll(collectNodes(node.children()));
      }
      return result;
   }
}
