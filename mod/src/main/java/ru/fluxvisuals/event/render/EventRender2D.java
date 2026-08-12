package ru.fluxvisuals.event.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import ru.fluxvisuals.event.Event;

/**
 * Событие рендера 2D поверх интерфейса (после основного HUD).
 * Используется для анимаций в BetterMinecraft.
 */
@Environment(EnvType.CLIENT)
public final class EventRender2D extends Event {
   private final DrawContext context;

   public EventRender2D(DrawContext context) {
      this.context = context;
   }

   public DrawContext context() {
      return context;
   }
}
