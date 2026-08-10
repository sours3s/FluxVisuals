package ru.fluxvisuals.module.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.player.AttackEvent;
import ru.fluxvisuals.event.render.WorldRenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderer;

@IModule(name = "Hit Effect", description = "Добавляет анимацию попадания по противнику", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class HitEffect extends Module {
   private final List<WaveEffect> waves = new ArrayList<>();

   @EventInit
   public void onAttack(AttackEvent e) {
      Entity target = e.getTarget();
      if (target != null && mc.world != null) {
         this.waves.add(new WaveEffect(new Vec3d(target.getX(), target.getY() + 0.05, target.getZ()), System.currentTimeMillis()));
      }
   }

   @EventInit
   public void onWorld(WorldRenderEvent e) {
      if (this.waves.isEmpty()) {
         return;
      }
      this.waves.removeIf(WaveEffect::isExpired);
      WorldRenderer wr = e.worldRenderer();
      for (WaveEffect wave : this.waves) {
         wave.render(wr);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class WaveEffect {
      private static final long DURATION = 2000L;
      private static final float MAX_RADIUS = 8.0F;
      private static final int SEGMENTS = 48;
      private final Vec3d center;
      private final long startTime;

      WaveEffect(Vec3d center, long startTime) {
         this.center = center;
         this.startTime = startTime;
      }

      boolean isExpired() {
         return System.currentTimeMillis() - this.startTime > DURATION;
      }

      void render(WorldRenderer wr) {
         long elapsed = System.currentTimeMillis() - this.startTime;
         float progress = (float) elapsed / (float) DURATION;
         float radius = HitEffect.easeOutCubic(progress) * MAX_RADIUS;
         float alpha = HitEffect.smoothAlpha(progress);
         if (alpha <= 0.01F) {
            return;
         }
         int color = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int) (alpha * 250.0F));
         double y = this.center.y;
         double cx = this.center.x;
         double cz = this.center.z;
         for (int i = 0; i < SEGMENTS; i++) {
            double a1 = (double) i / (double) SEGMENTS * Math.PI * 2.0;
            double a2 = (double) (i + 1) / (double) SEGMENTS * Math.PI * 2.0;
            Vec3d p1 = new Vec3d(cx + radius * Math.cos(a1), y, cz + radius * Math.sin(a1));
            Vec3d p2 = new Vec3d(cx + radius * Math.cos(a2), y, cz + radius * Math.sin(a2));
            wr.drawLine(p1, p2, 2.0, color, true);
         }
      }
   }

   private static float easeOutCubic(float t) {
      float x = 1.0F - t;
      return 1.0F - x * x * x;
   }

   private static float smoothAlpha(float progress) {
      float fadeIn = Math.min(1.0F, progress / 0.1F);
      fadeIn = fadeIn * fadeIn * (3.0F - 2.0F * fadeIn);
      float fadeOut = (float) Math.pow(1.0F - progress, 4.8);
      return fadeIn * fadeOut;
   }
}
