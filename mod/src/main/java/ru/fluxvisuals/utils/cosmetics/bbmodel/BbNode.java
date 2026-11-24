package ru.fluxvisuals.utils.cosmetics.bbmodel;

import java.util.List;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Vec3;

/** One outliner group or one geometry element in a .bbmodel tree. */
@Environment(EnvType.CLIENT)
public final class BbNode {
   private final String name;
   private final UUID uuid;
   private final Vec3 origin;
   private final Vec3 rotation;
   private final boolean visible;
   private final List<BbNode> children;
   private final BbElement element;

   private boolean poseOverridden;
   private Vec3 animatedRotation = Vec3.ZERO;
   private Vec3 animatedPosition = Vec3.ZERO;
   private Vec3 animatedScale = Vec3.ONE;

   public BbNode(String name, UUID uuid, Vec3 origin, Vec3 rotation, boolean visible,
         List<BbNode> children, BbElement element) {
      this.name = name == null ? "" : name;
      this.uuid = uuid;
      this.origin = origin;
      this.rotation = rotation;
      this.visible = visible;
      this.children = List.copyOf(children);
      this.element = element;
   }

   public String name() {
      return name;
   }

   public UUID uuid() {
      return uuid;
   }

   public Vec3 origin() {
      return origin;
   }

   public Vec3 rotation() {
      return rotation;
   }

   public boolean visible() {
      return visible;
   }

   public List<BbNode> children() {
      return children;
   }

   public BbElement element() {
      return element;
   }

   /** Rotation used for rendering: the animation value, or the authored group rotation. */
   public Vec3 renderRotation() {
      return poseOverridden ? animatedRotation : rotation;
   }

   public Vec3 renderPosition() {
      return animatedPosition;
   }

   public Vec3 renderScale() {
      return animatedScale;
   }

   public void applyPose(Vec3 rotation, Vec3 position, Vec3 scale) {
      this.poseOverridden = true;
      this.animatedRotation = rotation;
      this.animatedPosition = position;
      this.animatedScale = scale;
   }

   public void resetPose() {
      this.poseOverridden = false;
      this.animatedRotation = Vec3.ZERO;
      this.animatedPosition = Vec3.ZERO;
      this.animatedScale = Vec3.ONE;
   }
}
