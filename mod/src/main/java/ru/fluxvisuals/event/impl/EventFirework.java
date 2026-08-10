package ru.fluxvisuals.event.impl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import ru.fluxvisuals.event.Event;

@Environment(EnvType.CLIENT)
public class EventFirework extends Event {
   private final Entity boostedEntity;
   private float speed;

   public EventFirework(Entity boostedEntity, float speed) {
      this.boostedEntity = boostedEntity;
      this.speed = speed;
   }

   public Entity getBoostedEntity() {
      return this.boostedEntity;
   }

   public float getSpeed() {
      return this.speed;
   }

   public void setSpeed(float speed) {
      this.speed = speed;
   }
}
