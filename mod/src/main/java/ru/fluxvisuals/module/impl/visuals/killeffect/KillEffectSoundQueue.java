package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/** Очередь ванильных звуков с задержкой, привязанная к реальному времени. */
@Environment(EnvType.CLIENT)
public final class KillEffectSoundQueue {
   private final List<Pending> pending = new ArrayList<>();

   public void schedule(String sound, float volume, long delay) {
      RegistryEntry<SoundEvent> event = resolve(sound);
      if (event == null) {
         return;
      }
      float safeVolume = Math.max(0.0F, Math.min(2.0F, volume));
      if (delay <= 0L) {
         play(event, safeVolume);
      } else {
         this.pending.add(new Pending(event, safeVolume, System.currentTimeMillis() + delay));
      }
   }

   public void tick() {
      long now = System.currentTimeMillis();
      Iterator<Pending> iterator = this.pending.iterator();
      while (iterator.hasNext()) {
         Pending next = iterator.next();
         if (now >= next.playAt) {
            play(next.sound, next.volume);
            iterator.remove();
         }
      }
   }

   public void clear() {
      this.pending.clear();
   }

   private static RegistryEntry<SoundEvent> resolve(String sound) {
      if (sound == null) {
         return null;
      }
      return switch (sound) {
         case "impact" -> RegistryEntry.of(SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT);
         case "pickup" -> RegistryEntry.of(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
         case "thunder" -> RegistryEntry.of(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER);
         case "explode" -> SoundEvents.ENTITY_GENERIC_EXPLODE;
         case "levelup" -> RegistryEntry.of(SoundEvents.ENTITY_PLAYER_LEVELUP);
         case "extinguish" -> RegistryEntry.of(SoundEvents.BLOCK_FIRE_EXTINGUISH);
         default -> null;
      };
   }

   private static void play(RegistryEntry<SoundEvent> sound, float volume) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null) {
         client.getSoundManager().play(PositionedSoundInstance.ui(sound, volume));
      }
   }

   private record Pending(RegistryEntry<SoundEvent> sound, float volume, long playAt) {}
}
