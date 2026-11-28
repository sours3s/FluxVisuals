package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

/** Очередь ванильных и кастомных звуков с задержкой, привязанная к реальному времени. */
@Environment(EnvType.CLIENT)
public final class KillEffectSoundQueue {
   private static final Identifier KILL_IMPACT = Identifier.of("fluxvisuals", "kill_impact");
   private static final Identifier KILL_CRYSTAL = Identifier.of("fluxvisuals", "kill_crystal");
   private static final Identifier KILL_SOUL = Identifier.of("fluxvisuals", "kill2");

   private final List<Pending> pending = new ArrayList<>();

   public void schedule(String sound, float volume, long delay) {
      this.schedule(sound, volume, delay, 1.0F);
   }

   public void schedule(String sound, float volume, long delay, float pitch) {
      Resolved resolved = resolve(sound);
      if (resolved == null) {
         return;
      }
      float safeVolume = Math.max(0.0F, Math.min(2.0F, volume));
      float safePitch = Math.max(0.5F, Math.min(2.0F, resolved.pitch() * pitch));
      if (delay <= 0L) {
         play(resolved.sound(), safeVolume, safePitch);
      } else {
         this.pending.add(new Pending(resolved.sound(), safeVolume, safePitch, System.currentTimeMillis() + delay));
      }
   }

   public void tick() {
      long now = System.currentTimeMillis();
      Iterator<Pending> iterator = this.pending.iterator();
      while (iterator.hasNext()) {
         Pending next = iterator.next();
         if (now >= next.playAt()) {
            play(next.sound(), next.volume(), next.pitch());
            iterator.remove();
         }
      }
   }

   public void clear() {
      this.pending.clear();
   }

   /** Старые ключи продолжают работать; добавлены кастомные kill-звуки fluxvisuals. */
   private static Resolved resolve(String sound) {
      if (sound == null || sound.isBlank()) {
         return null;
      }
      String key = sound.toLowerCase(Locale.ROOT).trim();
      return switch (key) {
         case "impact" -> new Resolved(SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 1.15F);
         case "pickup" -> new Resolved(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.85F);
         case "thunder" -> new Resolved(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.1F);
         case "explode" -> new Resolved(SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0F);
         case "levelup" -> new Resolved(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F);
         case "extinguish" -> new Resolved(SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.0F);
         case "spike_teleport" -> new Resolved(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.62F);
         case "spike_wind" -> new Resolved(SoundEvents.ENTITY_BREEZE_WIND_BURST.comp_349(), 0.55F);
         case "kill", "kill_impact", "fluxvisuals:kill_impact" ->
               new Resolved(SoundEvent.of(KILL_IMPACT), 1.0F);
         case "kill2", "fluxvisuals:kill2" -> new Resolved(SoundEvent.of(KILL_SOUL), 1.0F);
         case "kill_crystal", "shatter", "fluxvisuals:kill_crystal" ->
               new Resolved(SoundEvent.of(KILL_CRYSTAL), 1.05F);
         default -> custom(key);
      };
   }

   private static Resolved custom(String key) {
      String prefix = "fluxvisuals:";
      if (!key.startsWith(prefix)) {
         return null;
      }
      String path = key.substring(prefix.length());
      if (path.isEmpty() || !path.matches("[a-z0-9_/.-]+")) {
         return null;
      }
      return new Resolved(SoundEvent.of(Identifier.of("fluxvisuals", path)), 1.0F);
   }

   private static void play(SoundEvent sound, float volume, float pitch) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.getSoundManager() != null) {
         client.getSoundManager().play(PositionedSoundInstance.ui(sound, volume, pitch));
      }
   }

   private record Resolved(SoundEvent sound, float pitch) {}

   private record Pending(SoundEvent sound, float volume, float pitch, long playAt) {}
}
