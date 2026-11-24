package ru.fluxvisuals.utils.cosmetics.bbmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

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

   private static List<BbNode> collectNodes(List<BbNode> roots) {
      java.util.List<BbNode> result = new java.util.ArrayList<>();
      for (BbNode node : roots) {
         result.add(node);
         result.addAll(collectNodes(node.children()));
      }
      return result;
   }
}
