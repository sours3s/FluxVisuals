package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EntityDeathEvent;
import ru.fluxvisuals.event.lifecycle.ClientTickEvent;
import ru.fluxvisuals.event.player.AttackEvent;
import ru.fluxvisuals.event.render.WorldRenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.Theme;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectDeathMemoryTracker;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectParticleSystem;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectScanRenderer;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectSoundQueue;
import ru.fluxvisuals.util.render.world.WorldRenderer;

@IModule(name = "Kill Effect", description = "Эффект при убийстве: молния, кровь, взрыв, сердце или дым", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class KillEffect extends Module {
   private static final long KILL_WINDOW_MS = 6500L;
   private static final int MIN_PARTICLES = 5;
   private static final int MAX_PARTICLES = 120;

   public final ModeSetting effectType = new ModeSetting("Effect Type", "Lightning",
         "Lightning", "Blood", "Explosion", "Heart", "Smoke");
   public final BooleanSetting mobs = new BooleanSetting("Mobs", true);
   public final SliderSetting particles = new SliderSetting("Particles", 45.0F, MIN_PARTICLES, MAX_PARTICLES, 1.0F, false);
   public final SliderSetting radius = new SliderSetting("Radius", 2.0F, 0.5F, 6.0F, 0.1F, false);
   public final BooleanSetting sound = new BooleanSetting("Sound", true);
   public final BooleanSetting shockwave = new BooleanSetting("Shockwave", true);

   private final KillEffectDeathMemoryTracker tracker;
   private final KillEffectSoundQueue soundQueue = new KillEffectSoundQueue();
   private final KillEffectParticleSystem particleSystem = new KillEffectParticleSystem();

   public KillEffect() {
      this.addSettings(new Setting[]{this.effectType, this.mobs, this.particles, this.radius, this.sound, this.shockwave});
      this.tracker = new KillEffectDeathMemoryTracker(KILL_WINDOW_MS, this::playEffectFor);
   }

   @Override
   public void onEnable() {
      super.onEnable();
      normalizeMode();
      reset();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      reset();
   }

   @EventInit
   public void onAttack(AttackEvent event) {
      Entity target = event.getTarget();
      if (!(target instanceof LivingEntity living) || living == mc.player) {
         return;
      }
      this.tracker.remember(living, this.mobs.get());
   }

   @EventInit
   public void onDeath(EntityDeathEvent event) {
      Entity entity = event.getEntity();
      if (entity instanceof LivingEntity living) {
         this.tracker.trigger(living);
      }
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      this.tracker.tick();
      this.soundQueue.tick();
      this.particleSystem.tick();
      KillEffectScanRenderer.tick();
   }

   @EventInit
   public void onRender(WorldRenderEvent event) {
      WorldRenderer renderer = event.worldRenderer();
      this.particleSystem.render(renderer);
      KillEffectScanRenderer.render(renderer);
   }

   private void playEffectFor(LivingEntity entity) {
      MinecraftClient client = mc;
      if (client == null || client.world == null || client.player == null || entity == client.player) {
         return;
      }
      if (!this.mobs.get() && !(entity instanceof PlayerEntity)) {
         return;
      }
      playEffectAt(entity.getEntityPos());
   }

   private void playEffectAt(Vec3d pos) {
      MinecraftClient client = mc;
      if (client == null || client.world == null || client.player == null || pos == null) {
         return;
      }

      String effect = mode();
      int count = Math.round(Math.max(MIN_PARTICLES, Math.min(MAX_PARTICLES, this.particles.get())));
      float radius = Math.max(0.25F, this.radius.get());
      Vec3d center = pos.add(0.0, 0.5, 0.0);

      switch (effect) {
         case "Lightning" -> {
            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, client.world);
            lightning.setPosition(pos.x, pos.y, pos.z);
            lightning.setCosmetic(true);
            client.world.addEntity(lightning);
            spawnParticles(client, ParticleTypes.EXPLOSION, center, Math.min(count, 20), radius, 0.12F);
            playSound("thunder", 2.0F);
         }
         case "Blood" -> {
            BlockPos blockPos = BlockPos.ofFloored(pos.x, pos.y, pos.z);
            client.world.syncWorldEvent(client.player, 2001, blockPos,
                  Block.getRawIdFromState(Blocks.REDSTONE_BLOCK.getDefaultState()));
            playSound("impact", 0.4F);
         }
         case "Explosion" -> {
            spawnParticles(client, ParticleTypes.EXPLOSION, center, count, radius, 0.18F);
            playSound("explode", 1.0F);
         }
         case "Heart" -> {
            spawnParticles(client, ParticleTypes.HEART, center, Math.max(8, count / 2), radius * 0.6F, 0.08F);
            playSound("levelup", 0.5F);
         }
         case "Smoke" -> {
            spawnParticles(client, ParticleTypes.LARGE_SMOKE, center, count, radius, 0.06F);
            playSound("extinguish", 1.0F);
         }
         default -> {
            spawnParticles(client, ParticleTypes.EXPLOSION, center, count, radius, 0.18F);
            playSound("explode", 1.0F);
         }
      }

      int color = Theme.VISUAL.accent();
      this.particleSystem.setColor(color);
      this.particleSystem.spawnShards(center, Math.min(24, count / 2), radius * 0.35F, 0.07F, 800, color);
      if (this.shockwave.get()) {
         KillEffectScanRenderer.spawn(center, color);
      }
   }

   private void playSound(String name, float volume) {
      if (this.sound.get()) {
         this.soundQueue.schedule(name, volume, 0L);
      }
   }

   private static void spawnParticles(MinecraftClient client, ParticleEffect type, Vec3d pos, int count,
         float radius, float speed) {
      if (client == null || client.world == null || type == null || pos == null || count <= 0) {
         return;
      }
      for (int i = 0; i < count; i++) {
         double angle = Math.random() * Math.PI * 2.0;
         double distance = Math.random() * radius;
         double x = pos.x + Math.cos(angle) * distance;
         double y = pos.y + Math.random() * Math.min(radius, 2.0);
         double z = pos.z + Math.sin(angle) * distance;
         double vx = (Math.random() - 0.5) * speed;
         double vy = Math.random() * speed;
         double vz = (Math.random() - 0.5) * speed;
         client.world.addParticleClient(type, x, y, z, vx, vy, vz);
      }
   }

   private String mode() {
      normalizeMode();
      return this.effectType.get();
   }

   private void normalizeMode() {
      String current = this.effectType.get();
      if (this.effectType.modes.contains(current)) {
         return;
      }
      String fixed = switch (current) {
         case "Soul" -> "Heart";
         case "Shatter" -> "Explosion";
         default -> "Lightning";
      };
      this.effectType.index = Math.max(0, this.effectType.modes.indexOf(fixed));
      this.effectType.currentMode = fixed;
   }

   private void reset() {
      this.tracker.clear();
      this.soundQueue.clear();
      this.particleSystem.clear();
      KillEffectScanRenderer.clear();
   }
}
