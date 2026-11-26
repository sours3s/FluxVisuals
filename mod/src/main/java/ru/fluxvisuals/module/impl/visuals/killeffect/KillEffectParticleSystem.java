package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.util.render.world.WorldRenderer;

/** Легкая система частиц для эффектов гибели: искры и разлетающиеся осколки. */
@Environment(EnvType.CLIENT)
public final class KillEffectParticleSystem {
   private static final int MAX_PARTICLES = 400;

   private final List<Particle> particles = new ArrayList<>();
   private int color = 0xFFFFFFFF;
   private long lastTick = System.currentTimeMillis();

   public void setColor(int color) {
      this.color = color;
   }

   public void spawnBurst(Vec3d pos, int count, float speed, float spread, int life, float size) {
      this.spawnBurst(pos, count, speed, spread, life, size, this.color);
   }

   public void spawnBurst(Vec3d pos, int count, float speed, float spread, int life, float size, int color) {
      if (pos == null || count <= 0) {
         return;
      }
      ThreadLocalRandom random = ThreadLocalRandom.current();
      float base = Math.max(0.0F, speed);
      float variance = Math.max(0.0F, spread);
      for (int i = 0; i < count; i++) {
         double angle = random.nextDouble(0.0, Math.PI * 2.0);
         double pitch = random.nextDouble(-0.35, 1.0);
         float magnitude = base * (0.65F + (float) random.nextDouble(0.0, variance));
         Vec3d velocity = new Vec3d(
               Math.cos(angle) * magnitude,
               pitch * magnitude,
               Math.sin(angle) * magnitude
         );
         addParticle(new Particle(pos, velocity, 0.0F, life, size, color, false));
      }
   }

   public void spawnShards(Vec3d pos, int count, float force, float size, int life, int color) {
      if (pos == null || count <= 0) {
         return;
      }
      ThreadLocalRandom random = ThreadLocalRandom.current();
      float power = Math.max(0.1F, force);
      for (int i = 0; i < count; i++) {
         double angle = random.nextDouble(0.0, Math.PI * 2.0);
         double up = random.nextDouble(0.25, 1.15);
         float magnitude = power * (0.7F + (float) random.nextDouble(0.0, 0.7F));
         Vec3d velocity = new Vec3d(
               Math.cos(angle) * magnitude,
               up * magnitude,
               Math.sin(angle) * magnitude
         );
         addParticle(new Particle(pos, velocity, -12.0F, life, size * 0.45F, color, true));
      }
   }

   private void addParticle(Particle particle) {
      if (this.particles.size() >= MAX_PARTICLES) {
         this.particles.remove(0);
      }
      this.particles.add(particle);
   }

   public void tick() {
      long now = System.currentTimeMillis();
      float delta = Math.min(0.1F, Math.max(0.0F, (now - this.lastTick) / 1000.0F));
      this.lastTick = now;
      Iterator<Particle> iterator = this.particles.iterator();
      while (iterator.hasNext()) {
         Particle particle = iterator.next();
         if (particle.isExpired(now)) {
            iterator.remove();
            continue;
         }
         particle.velocity = particle.velocity.add(0.0, particle.gravity * delta, 0.0);
         particle.position = particle.position.add(particle.velocity.multiply(delta));
      }
   }

   public void render(WorldRenderer renderer) {
      if (renderer == null || this.particles.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      for (Particle particle : this.particles) {
         float remaining = particle.remaining(now);
         if (remaining <= 0.0F) {
            continue;
         }
         int alpha = Math.max(0, Math.min(255, Math.round(220.0F * remaining)));
         int color = withAlpha(particle.color, alpha);
         if (particle.shard) {
            Vec3d tail = particle.position.subtract(particle.velocity.multiply(0.035));
            renderer.drawLine(particle.position, tail, 1.5, color, false);
         } else {
            Vec3d min = particle.position.add(-particle.size, -particle.size, -particle.size);
            Vec3d max = particle.position.add(particle.size, particle.size, particle.size);
            renderer.drawCube(min, max, color, false);
         }
      }
   }

   public void clear() {
      this.particles.clear();
      this.lastTick = System.currentTimeMillis();
   }

   private static int withAlpha(int color, int alpha) {
      return alpha << 24 | color & 0x00FFFFFF;
   }

   private static final class Particle {
      private Vec3d position;
      private Vec3d velocity;
      private final float gravity;
      private final long expiresAt;
      private final long duration;
      private final float size;
      private final int color;
      private final boolean shard;

      private Particle(Vec3d position, Vec3d velocity, float gravity, int life, float size, int color, boolean shard) {
         this.position = position;
         this.velocity = velocity;
         this.gravity = gravity;
         this.duration = Math.max(1L, life);
         this.expiresAt = System.currentTimeMillis() + this.duration;
         this.size = Math.max(0.01F, size);
         this.color = color;
         this.shard = shard;
      }

      private boolean isExpired(long now) {
         return now >= this.expiresAt;
      }

      private float remaining(long now) {
         long left = this.expiresAt - now;
         if (left <= 0L) {
            return 0.0F;
         }
         float value = (float) left / (float) this.duration;
         return value * value;
      }
   }
}
