package ru.fluxvisuals.util.render.shader;

/** Выбранное качество рендера Shader Fog. Вынесено отдельно, чтобы параметр можно было покрыть тестом. */
public final class ShaderFogQuality {
   /** Небо низкочастотное, поэтому 1/3 разрешения даёт тот же вид за ~1/9 стоимости фрагментного шейдера. */
   public static final float RENDER_SCALE = 1.0F / 3.0F;

   private ShaderFogQuality() {}

   public static int internalSize(int size) {
      return Math.max(1, Math.round(size * RENDER_SCALE));
   }
}
