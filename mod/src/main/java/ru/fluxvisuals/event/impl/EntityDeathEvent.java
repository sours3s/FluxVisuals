package ru.fluxvisuals.event.impl;

import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import ru.fluxvisuals.event.Event;

@Environment(EnvType.CLIENT)
public class EntityDeathEvent extends Event {
   private final Entity entity;

   @Generated
   public Entity getEntity() {
      return this.entity;
   }

   @Generated
   public EntityDeathEvent(Entity entity) {
      this.entity = entity;
   }
}
