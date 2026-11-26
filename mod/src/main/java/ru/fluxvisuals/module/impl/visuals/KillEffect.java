package ru.fluxvisuals.module.impl.visuals;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventChangeWorld;
import ru.fluxvisuals.event.impl.EventScreen;
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
import ru.fluxvisuals.module.api.setting.impl.HueSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectBeamRenderer;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectCameraState;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectDarknessRenderer;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectOverlayRenderer;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectDeathMemoryTracker;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectModes;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectParticleSystem;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectScanRenderer;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectSoulRenderer;
import ru.fluxvisuals.module.impl.visuals.killeffect.KillEffectSoundQueue;
import ru.fluxvisuals.util.render.world.WorldRenderer;

@IModule(name = "Kill Effect", description = "Кастомные эффекты при убийстве: луч, душа, осколки, тьма и другое", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class KillEffect extends Module {
   private static final long KILL_WINDOW_MS = 6500L;
   private static final int MIN_PARTICLES = 5;
   private static final int MAX_PARTICLES = 350;

   public final ModeSetting effectType = new ModeSetting("Effect Type", KillEffectModes.DEFAULT,
         KillEffectModes.ALL.toArray(new String[0]));
   public final BooleanSetting players = new BooleanSetting("Players", true);
   public final BooleanSetting mobs = new BooleanSetting("Mobs", true);
   public final ModeSetting colorMode = new ModeSetting("Color Mode", "Theme", "Theme", "Custom");
   public final HueSetting color = new HueSetting("Color", 42.0F)
         .hidden(() -> !this.colorMode.is("Custom"));
   public final SliderSetting duration = new SliderSetting("Duration", 2.2F, 0.5F, 8.0F, 0.1F, false);
   public final SliderSetting particles = new SliderSetting("Particles", 45.0F, MIN_PARTICLES, MAX_PARTICLES, 1.0F, false);
   public final SliderSetting radius = new SliderSetting("Radius", 5.0F, 0.5F, 20.0F, 0.5F, false);
   public final SliderSetting speed = new SliderSetting("Speed", 1.0F, 0.1F, 5.0F, 0.1F, false)
         .hidden(() -> this.effectType.is("Scan"));
   public final SliderSetting beamHeight = new SliderSetting("Beam Height", 5.2F, 1.5F, 12.0F, 0.5F, false)
         .hidden(() -> !usesBeamSize());
   public final SliderSetting beamWidth = new SliderSetting("Beam Width", 0.42F, 0.08F, 1.2F, 0.02F, false)
         .hidden(() -> !usesBeamSize());
   public final BooleanSetting shockwave = new BooleanSetting("Shockwave", true);
   public final BooleanSetting sound = new BooleanSetting("Sound", true);
   public final BooleanSetting totem = new BooleanSetting("Totem", false);
   public final BooleanSetting hideBody = new BooleanSetting("Hide Body", false)
         .hidden(() -> !this.effectType.is("Body Particles"));
   public final BooleanSetting zoom = new BooleanSetting("Zoom", false)
         .hidden(() -> !this.effectType.is("Scan"));
   public final BooleanSetting shake = new BooleanSetting("Shake", false)
         .hidden(() -> !this.effectType.is("Scan"));
   public final BooleanSetting desaturate = new BooleanSetting("Desaturate", true)
         .hidden(() -> !this.effectType.is("Scan") && !this.effectType.is("Darkness"));
   public final SliderSetting shardSize = new SliderSetting("Shard Size", 0.07F, 0.02F, 0.3F, 0.01F, false)
         .hidden(() -> !this.effectType.is("Body Particles"));
   public final SliderSetting shardForce = new SliderSetting("Shard Force", 0.7F, 0.1F, 3.0F, 0.1F, false)
         .hidden(() -> !this.effectType.is("Body Particles"));
   public final SliderSetting shardLife = new SliderSetting("Shard Life", 1.4F, 0.3F, 5.0F, 0.1F, false)
         .hidden(() -> !this.effectType.is("Body Particles"));

   private final KillEffectDeathMemoryTracker tracker;
   private final KillEffectSoundQueue soundQueue = new KillEffectSoundQueue();
   private final KillEffectParticleSystem particleSystem = new KillEffectParticleSystem();
   private int lastEffectId = -1;
   private long lastEffectAt;
   private int lastTotemId = -1;
   private long lastTotemAt;
   private final Map<Integer, Long> hiddenEntities = new HashMap<>();

   public KillEffect() {
      this.addSettings(new Setting[]{this.effectType, this.players, this.mobs, this.colorMode, this.color,
            this.duration, this.particles, this.radius, this.speed, this.beamHeight, this.beamWidth,
            this.shockwave, this.sound, this.totem, this.hideBody, this.zoom, this.shake, this.desaturate,
            this.shardSize, this.shardForce, this.shardLife});
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

   @Override
   public void load(JsonObject object) {
      super.load(object);
      normalizeMode();
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
      KillEffectBeamRenderer.tick();
      KillEffectSoulRenderer.tick();
      KillEffectDarknessRenderer.tick();
      KillEffectOverlayRenderer.tick();
      KillEffectCameraState.tick();
      long now = System.currentTimeMillis();
      this.hiddenEntities.values().removeIf(expiresAt -> expiresAt <= now);
   }

   @EventInit
   public void onChangeWorld(EventChangeWorld event) {
      reset();
   }

   @EventInit
   public void onScreen(EventScreen event) {
      if (mc == null || mc.currentScreen != null) {
         return;
      }
      KillEffectOverlayRenderer.tick();
      KillEffectOverlayRenderer.render(event.renderer(), event.viewportWidth(), event.viewportHeight());
   }

   @EventInit
   public void onRender(WorldRenderEvent event) {
      WorldRenderer renderer = event.worldRenderer();
      this.particleSystem.render(renderer);
      KillEffectScanRenderer.render(renderer);
      KillEffectBeamRenderer.render(renderer);
      KillEffectSoulRenderer.render(renderer);
      KillEffectDarknessRenderer.render(renderer);
   }

   /** Прямой хук статуса тотема: отдельного события в event bus нет. */
   public static void handleTotem(EntityStatusS2CPacket packet) {
      if (packet == null || packet.getStatus() != 35) {
         return;
      }
      if (mc == null || mc.world == null || mc.player == null
            || FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
         return;
      }
      Module module = FluxVisualsClient.get.manager.getModule(KillEffect.class);
      if (!(module instanceof KillEffect effect) || !module.enable || !effect.totem.get()) {
         return;
      }
      if (!(packet.getEntity(mc.world) instanceof LivingEntity living) || living == mc.player) {
         return;
      }

      long now = System.currentTimeMillis();
      if (living.getId() == effect.lastTotemId && now - effect.lastTotemAt < 750L) {
         return;
      }
      effect.lastTotemId = living.getId();
      effect.lastTotemAt = now;
      effect.playEffectFor(living, false);
   }

   /** Используется EntityCullingMixin, чтобы скрыть модель убитой сущности. */
   public static boolean shouldHide(Entity entity) {
      if (entity == null || mc == null || FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
         return false;
      }
      Module module = FluxVisualsClient.get.manager.getModule(KillEffect.class);
      if (!(module instanceof KillEffect effect) || !module.enable || !effect.hideBody.get()
            || !effect.effectType.is("Body Particles")) {
         return false;
      }
      Long expiresAt = effect.hiddenEntities.get(entity.getId());
      return expiresAt != null && expiresAt > System.currentTimeMillis();
   }

   private void playEffectFor(LivingEntity entity) {
      playEffectFor(entity, true);
   }

   /** allowHideBody=false используется для тотема: сущность остаётся живой. */
   private void playEffectFor(LivingEntity entity, boolean allowHideBody) {
      MinecraftClient client = mc;
      if (client == null || client.world == null || client.player == null || entity == client.player) {
         return;
      }
      if (entity instanceof PlayerEntity) {
         if (!this.players.get()) {
            return;
         }
      } else if (!this.mobs.get()) {
         return;
      }

      long now = System.currentTimeMillis();
      if (entity.getId() == this.lastEffectId && now - this.lastEffectAt < 750L) {
         return;
      }
      this.lastEffectId = entity.getId();
      this.lastEffectAt = now;
      playEffectAt(entity.getEntityPos());
      if (allowHideBody && this.hideBody.get() && mode().equals("Body Particles")) {
         this.hiddenEntities.put(entity.getId(), now + Math.round(this.duration.get() * 1000.0F));
      }
   }

   private void playEffectAt(Vec3d pos) {
      MinecraftClient client = mc;
      if (client == null || client.world == null || client.player == null || pos == null) {
         return;
      }

      normalizeMode();
      int count = Math.round(Math.max(MIN_PARTICLES, Math.min(MAX_PARTICLES, this.particles.get())));
      float radius = Math.max(0.25F, this.radius.get());
      Vec3d center = pos.add(0.0, 0.5, 0.0);

      int color = effectColor();
      String effect = mode();

      // Все режимы используют общие частицы-кирпичики; звук выбирается по режиму.
      spawnParticles(client, ParticleTypes.END_ROD, center, Math.max(8, count / 2), radius, 0.12F);
      String sound = switch (effect) {
         case "Sky Beam" -> "thunder";
         case "Shatter", "Darkness", "Spike Abduction", "Body Particles" -> "explode";
         case "Soul" -> "levelup";
         default -> "impact";
      };
      playSound(sound, sound.equals("thunder") || sound.equals("explode") ? 1.0F : 0.5F);

      switch (effect) {
         case "Beam", "Sky Beam" -> KillEffectBeamRenderer.spawn(pos, color, this.duration.get(),
               this.beamHeight.get(), this.beamWidth.get(), radius, this.speed.get(), count,
               effect.equals("Sky Beam"));
         case "Soul", "Shatter" -> KillEffectSoulRenderer.spawn(pos, color, this.duration.get(),
               this.beamHeight.get(), this.beamWidth.get(), radius, this.speed.get(), count,
               effect.equals("Soul"));
         case "Darkness", "Spike Abduction" -> KillEffectDarknessRenderer.spawn(pos, color,
               this.duration.get(), this.beamHeight.get(), this.beamWidth.get(), radius,
               this.speed.get(), count, effect.equals("Spike Abduction"));
         case "Body Particles" -> {
            int life = Math.round(this.shardLife.get() * 1000.0F);
            float force = this.shardForce.get() * Math.min(1.5F, radius / 5.0F);
            this.particleSystem.setColor(color);
            this.particleSystem.spawnBurst(center, count, force, radius * 0.5F, life,
                  this.shardSize.get(), color);
            this.particleSystem.spawnShards(center, Math.min(120, count), force,
                  this.shardSize.get(), life, color);
         }
         case "Scan" -> {
            KillEffectOverlayRenderer.spawn(color, this.duration.get(), this.desaturate.get(), true);
            KillEffectCameraState.spawn(this.duration.get(), this.zoom.get(), this.shake.get());
         }
         default -> {
            // Все кастомные режимы уже обработаны.
         }
      }

      if (effect.equals("Darkness") && this.desaturate.get()) {
         KillEffectOverlayRenderer.spawn(color, this.duration.get(), true, false);
      }

      if (!effect.equals("Body Particles")) {
         this.particleSystem.setColor(color);
         this.particleSystem.spawnShards(center, Math.min(24, count / 2), radius * 0.35F, 0.07F, 800, color);
      }
      if (this.shockwave.get()) {
         KillEffectScanRenderer.spawn(center, color, radius);
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
      ThreadLocalRandom random = ThreadLocalRandom.current();
      for (int i = 0; i < count; i++) {
         double angle = random.nextDouble(0.0, Math.PI * 2.0);
         double distance = random.nextDouble(0.0, radius);
         double x = pos.x + Math.cos(angle) * distance;
         double y = pos.y + random.nextDouble(0.0, Math.min(radius, 2.0));
         double z = pos.z + Math.sin(angle) * distance;
         double vx = random.nextDouble(-0.5, 0.5) * speed;
         double vy = random.nextDouble(0.0, 1.0) * speed;
         double vz = random.nextDouble(-0.5, 0.5) * speed;
         client.world.addParticleClient(type, x, y, z, vx, vy, vz);
      }
   }

   private int effectColor() {
      return this.colorMode.is("Custom") ? this.color.getRGBA(255) : Theme.VISUAL.accent();
   }

   private boolean usesBeamSize() {
      String current = this.effectType.get();
      return current.equals("Beam") || current.equals("Sky Beam") || current.equals("Soul")
            || current.equals("Shatter") || current.equals("Darkness") || current.equals("Spike Abduction");
   }

   private String mode() {
      normalizeMode();
      return this.effectType.get();
   }

   private void normalizeMode() {
      String fixed = KillEffectModes.normalize(this.effectType.get());
      this.effectType.index = Math.max(0, this.effectType.modes.indexOf(fixed));
      this.effectType.currentMode = fixed;
   }

   private void reset() {
      this.tracker.clear();
      this.soundQueue.clear();
      this.particleSystem.clear();
      KillEffectScanRenderer.clear();
      KillEffectBeamRenderer.clear();
      KillEffectSoulRenderer.clear();
      KillEffectDarknessRenderer.clear();
      KillEffectOverlayRenderer.clear();
      KillEffectCameraState.clear();
      this.hiddenEntities.clear();
      this.lastEffectId = -1;
      this.lastEffectAt = 0L;
      this.lastTotemId = -1;
      this.lastTotemAt = 0L;
   }
}
