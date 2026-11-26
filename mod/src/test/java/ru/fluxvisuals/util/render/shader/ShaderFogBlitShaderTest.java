package ru.fluxvisuals.util.render.shader;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ShaderFogBlitShaderTest {
   @Test
   void finalBlitWritesSkyDepthSoEqualDepthTestPasses() throws Exception {
      String source;
      try (InputStream stream = getClass().getResourceAsStream(
            "/assets/fluxvisuals/shaders/core/shader_fog_blit.vsh")) {
         assertNotNull(stream, "shader_fog_blit.vsh must be packaged as a resource");
         source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      }

      assertTrue(source.contains("gl_Position = vec4(pos, 1.0, 1.0);"),
            "Shader Fog blit must keep NDC z = 1.0 for EQUAL_DEPTH_TEST against the sky");
      assertTrue(source.contains("out vec2 texCoord;"),
            "Shader Fog blit must provide UVs for framebuffer_blit.fsh");
   }
}
