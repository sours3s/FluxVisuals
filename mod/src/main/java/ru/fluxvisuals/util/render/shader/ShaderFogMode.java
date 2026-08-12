package ru.fluxvisuals.util.render.shader;

/** Режимы шейдерного неба (ShaderFog). */
public enum ShaderFogMode {
   CAUSTIC("Caustic"),
   DRAIN("Drain"),
   NEBULA("Nebula"),
   PLASMA("Plasma"),
   BLOOM("Bloom");

   private final String renderName;

   ShaderFogMode(String renderName) {
      this.renderName = renderName;
   }

   public String getRenderName() {
      return renderName;
   }
}
