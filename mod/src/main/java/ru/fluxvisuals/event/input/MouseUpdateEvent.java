package ru.fluxvisuals.event.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import ru.fluxvisuals.event.Event;

@Environment(EnvType.CLIENT)
public final class MouseUpdateEvent extends Event {
   private final MinecraftClient client;

   public MouseUpdateEvent(MinecraftClient client) {
      this.client = client;
   }

   public MinecraftClient client() {
      return this.client;
   }
}
