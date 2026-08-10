package ru.fluxvisuals.module.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EntityDeathEvent;
import ru.fluxvisuals.event.render.WorldRenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderer;

@IModule(name = "Kill Effect", description = "Добавляет анимацию гибели противника", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class KillEffect extends Module {
   private static final int PARTICLE_COUNT = 60;
   private static final Random RANDOM = new Random();

   public final BooleanSetting mobs = new BooleanSetting("Mobs", false);
   public final ModeSetting effectType = new ModeSetting("Effect Type", "Soul", "Soul", "Shatter");
   private final List<Particle> particles = new ArrayList<>();

   public KillEffect() {
      this.addSettings(new Setting[]{this.mobs, this.effectType});
   }

   @EventInit
   public void onDeath(EntityDeathEvent e) {
      Entity entity = e.getEntity();
      if (!(entity instanceof LivingEntity)) {
         return;
      }
      if (!this.mobs.get() && !(entity instanceof PlayerEntity)) {
         return;
      }
      if (entity == mc.player || mc.world == null) {
         return;
      }
      boolean soul = this.effectType.get().equals("Soul");
      Vec3d origin = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.4, entity.getZ());
      long now = System.currentTimeMillis();
      long duration = soul ? 4000L : 2500L;
      for (int i = 0; i < PARTICLE_COUNT; i++) {
         Vec3d vel;
         if (soul) {
            vel = new Vec3d((RANDOM.nextDouble() - 0.5) * 0.12, 0.08 + RANDOM.nextDouble() * 0.18, (RANDOM.nextDouble() - 0.5) * 0.12);
         } else {
            double ang = RANDOM.nextDouble() * Math.PI * 2.0;
            double spd = 0.18 + RANDOM.nextDouble() * 0.45;
            vel = new Vec3d(Math.cos(ang) * spd, 0.15 + RANDOM.nextDouble() * 0.35, Math.sin(ang) * spd);
         }
         this.particles.add(new Particle(origin, vel, now + RANDOM.nextLong() % 400L, duration));
      }
   }

   @EventInit
   public void onWorld(WorldRenderEvent e) {
      if (this.particles.isEmpty()) {
         return;
      }
      this.particles.removeIf(Particle::isExpired);
      WorldRenderer wr = e.worldRenderer();
      for (Particle p : this.particles) {
         p.render(wr);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Particle {
      private Vec3d pos;
      private final Vec3d velocity;
      private final long startTime;
      private final long duration;

      Particle(Vec3d pos, Vec3d velocity, long startTime, long duration) {
         this.pos = pos;
         this.velocity = velocity;
         this.startTime = startTime;
         this.duration = duration;
      }

      boolean isExpired() {
         return System.currentTimeMillis() - this.startTime > this.duration;
      }

      void render(WorldRenderer wr) {
         long elapsed = System.currentTimeMillis() - this.startTime;
         if (elapsed < 0L) {
            return;
         }
         float t = (float) elapsed / (float) this.duration;
         this.pos = this.pos.add(this.velocity);
         float alpha = (1.0F - t);
         alpha = alpha * alpha;
         if (alpha <= 0.02F) {
            return;
         }
         int color = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int) (alpha * 220.0F));
         float s = 0.12F;
         wr.drawCube(this.pos.add(-s, -s, -s), this.pos.add(s, s, s), color, false);
      }
   }
}
