package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Хранит недавно атакованных сущностей и подтверждает их смерть. */
@Environment(EnvType.CLIENT)
public final class KillEffectDeathMemoryTracker {
   private static final int MAX_MEMORIES = 12;

   private final long maxMemoryMs;
   private final Consumer<LivingEntity> onDeath;
   private final List<Memory> memories = new ArrayList<>();

   public KillEffectDeathMemoryTracker(long maxMemoryMs, Consumer<LivingEntity> onDeath) {
      this.maxMemoryMs = Math.max(1L, maxMemoryMs);
      this.onDeath = onDeath;
   }

   public void remember(LivingEntity entity, boolean allowAnyEntity) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (entity == null || entity == client.player || !entity.isAlive()) {
         return;
      }
      for (Memory memory : this.memories) {
         if (memory.matches(entity)) {
            memory.refresh(this.maxMemoryMs);
            return;
         }
      }
      if (!allowAnyEntity && !(entity instanceof PlayerEntity)) {
         return;
      }
      if (this.memories.size() >= MAX_MEMORIES) {
         this.memories.remove(0);
      }
      this.memories.add(new Memory(entity, this.maxMemoryMs));
   }

   /** Подтверждает смерть уже атакованной сущности из события смерти. */
   public boolean trigger(LivingEntity entity) {
      if (entity == null) {
         return false;
      }
      Iterator<Memory> iterator = this.memories.iterator();
      while (iterator.hasNext()) {
         Memory memory = iterator.next();
         if (memory.matches(entity) && !memory.triggered) {
            memory.triggered = true;
            iterator.remove();
            this.onDeath.accept(entity);
            return true;
         }
      }
      return false;
   }

   public void forget(int id) {
      this.memories.removeIf(memory -> memory.entity.getId() == id);
   }

   /** Резервный путь: проверяет состояние мира, если пакет смерти не пришёл. */
   public void tick() {
      Iterator<Memory> iterator = this.memories.iterator();
      while (iterator.hasNext()) {
         Memory memory = iterator.next();
         if (!memory.isActive()) {
            iterator.remove();
            continue;
         }
         if (!memory.triggered && isDead(memory.entity)) {
            memory.triggered = true;
            iterator.remove();
            this.onDeath.accept(memory.entity);
         }
      }
   }

   public void clear() {
      this.memories.clear();
   }

   private static boolean isDead(LivingEntity entity) {
      return !entity.isAlive() || entity.isRemoved() || entity.getHealth() <= 0.0F;
   }

   private static final class Memory {
      private final LivingEntity entity;
      private long expiresAt;
      private boolean triggered;

      private Memory(LivingEntity entity, long maxMemoryMs) {
         this.entity = entity;
         this.refresh(maxMemoryMs);
      }

      private boolean matches(LivingEntity other) {
         return this.entity == other || this.entity.getId() == other.getId();
      }

      private void refresh(long maxMemoryMs) {
         this.expiresAt = System.currentTimeMillis() + maxMemoryMs;
      }

      private boolean isActive() {
         return System.currentTimeMillis() < this.expiresAt;
      }
   }
}
