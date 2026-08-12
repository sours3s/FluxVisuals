package ru.fluxvisuals.util.render.shader;

import net.minecraft.util.Identifier;

/**
 * Хелпер для кастомных шейдеров FluxVisuals: возвращает Identifier вида
 * {@code fluxvisuals:core/<name>}, по которому Minecraft грузит шейдеры
 * из {@code assets/fluxvisuals/shaders/core/<name>.vsh/.fsh}.
 */
public final class ResourceProvider {

   private ResourceProvider() {}

   public static Identifier getShaderIdentifier(String name) {
      return Identifier.of("fluxvisuals", "core/" + name);
   }
}
