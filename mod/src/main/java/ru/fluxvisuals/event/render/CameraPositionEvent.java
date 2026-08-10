package ru.fluxvisuals.event.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.event.Event;

@Environment(EnvType.CLIENT)
public class CameraPositionEvent extends Event {
   private Vec3d position;
   private final float tickDelta;

   public CameraPositionEvent(Vec3d position, float tickDelta) {
      this.position = position;
      this.tickDelta = tickDelta;
   }

   public Vec3d getPosition() {
      return this.position;
   }

   public void setPosition(Vec3d position) {
      this.position = position;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }
}
