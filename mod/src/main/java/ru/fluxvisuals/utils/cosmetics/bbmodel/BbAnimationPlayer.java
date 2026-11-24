package ru.fluxvisuals.utils.cosmetics.bbmodel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Animation;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Animator;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Keyframe;
import ru.fluxvisuals.utils.cosmetics.bbmodel.BbModel.Vec3;

/** Samples Blockbench keyframes and applies them to the parsed node tree. */
@Environment(EnvType.CLIENT)
public final class BbAnimationPlayer {
   private static final String[] ATTACK = { "attack", "swing", "punch", "hit" };
   private static final String[] ELYTRA = { "elytra", "glide" };
   private static final String[] FLY = { "fly", "flying" };
   private static final String[] SPRINT = { "sprint", "run" };
   private static final String[] CROUCH = { "crouch", "sneak" };
   private static final String[] SWIM = { "swim" };
   private static final String[] WALK = { "walk", "move" };
   private static final String[] IDLE = { "idle", "stand" };
   private static final String[] BLINK = { "blink" };

   private final BbModel model;
   private String currentName = "";
   private float time;

   public BbAnimationPlayer(BbModel model) {
      this.model = model;
   }

   /** Chooses the animation for this frame and advances it by {@code delta} seconds. */
   public void tick(BbPoseState state, float delta) {
      String name = select(state);
      if (name == null) {
         reset();
         return;
      }
      if (!name.equals(currentName)) {
         currentName = name;
         time = 0.0F;
      }
      Animation animation = model.animation(name);
      if (animation == null) {
         reset();
         return;
      }
      time += Math.max(0.0F, delta);
      float length = Math.max(animation.length(), 0.0F);
      if (length > 0.0F) {
         if (animation.loop()) {
            time %= length;
         } else {
            time = Math.min(time, length);
         }
      }
      apply(animation, time);
   }

   public void reset() {
      currentName = "";
      time = 0.0F;
      for (BbNode node : model.nodes()) {
         node.resetPose();
      }
   }

   public String currentName() {
      return currentName;
   }

   private String select(BbPoseState state) {
      if (state.attacking()) {
         String found = find(ATTACK);
         if (found != null) {
            return found;
         }
      }
      if (state.blinking()) {
         String found = find(BLINK);
         if (found != null) {
            return found;
         }
      }
      if (state.gliding()) {
         String found = find(ELYTRA);
         if (found != null) {
            return found;
         }
      }
      if (state.flying()) {
         String found = find(FLY);
         if (found != null) {
            return found;
         }
      }
      if (state.sprinting() && state.moving()) {
         String found = find(SPRINT);
         if (found != null) {
            return found;
         }
      }
      if (state.crouching()) {
         String found = find(CROUCH);
         if (found != null) {
            return found;
         }
      }
      if (state.swimming()) {
         String found = find(SWIM);
         if (found != null) {
            return found;
         }
      }
      if (state.moving()) {
         String found = find(WALK);
         if (found != null) {
            return found;
         }
      }
      return find(IDLE);
   }

   private String find(String[] prefixes) {
      List<String> names = new ArrayList<>(model.animations().keySet());
      names.sort(Comparator.naturalOrder());
      for (String name : names) {
         for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
               return name;
            }
         }
      }
      return null;
   }

   private void apply(Animation animation, float time) {
      for (BbNode node : model.nodes()) {
         node.resetPose();
      }
      for (Map.Entry<UUID, Animator> entry : animation.animators().entrySet()) {
         BbNode node = model.node(entry.getKey());
         if (node == null) {
            continue;
         }
         List<Keyframe> rotation = keyframes(entry.getValue(), "rotation");
         List<Keyframe> position = keyframes(entry.getValue(), "position");
         List<Keyframe> scale = keyframes(entry.getValue(), "scale");

         Vec3 baseRotation = node.rotation();
         // Figura negates x/y for animated rotations and x for animated positions
         // to account for the flipped entity render matrix.
         Vec3 animatedRotation = rotation.isEmpty() ? Vec3.ZERO : sample(rotation, time, 0);
         Vec3 renderRotation = Vec3.of(
               baseRotation.x() - animatedRotation.x(),
               baseRotation.y() - animatedRotation.y(),
               baseRotation.z() + animatedRotation.z());
         Vec3 animatedPosition = position.isEmpty() ? Vec3.ZERO : sample(position, time, 1);
         Vec3 renderPosition = Vec3.of(-animatedPosition.x(), animatedPosition.y(), animatedPosition.z());
         Vec3 renderScale = scale.isEmpty() ? Vec3.ONE : sample(scale, time, 2);
         node.applyPose(renderRotation, renderPosition, renderScale);
      }
   }

   private static List<Keyframe> keyframes(Animator animator, String channel) {
      List<Keyframe> result = new ArrayList<>();
      for (Keyframe keyframe : animator.keyframes()) {
         if (channel.equals(keyframe.channel())) {
            result.add(keyframe);
         }
      }
      result.sort(Comparator.comparingDouble(Keyframe::time));
      return result;
   }

   /** channel: 0 = rotation, 1 = position, 2 = scale. */
   private static Vec3 sample(List<Keyframe> keyframes, float time, int channel) {
      Vec3 fallback = channel == 2 ? Vec3.ONE : Vec3.ZERO;
      if (keyframes.isEmpty()) {
         return fallback;
      }
      if (time <= keyframes.get(0).time()) {
         return keyframes.get(0).value();
      }
      int index = 0;
      while (index + 1 < keyframes.size() && keyframes.get(index + 1).time() <= time) {
         index++;
      }
      Keyframe current = keyframes.get(index);
      if (index + 1 >= keyframes.size()) {
         return current.value();
      }
      Keyframe next = keyframes.get(index + 1);
      float span = next.time() - current.time();
      if (span <= 0.0F) {
         return current.value();
      }
      float t = (time - current.time()) / span;
      t = Math.max(0.0F, Math.min(1.0F, t));
      if ("step".equals(next.interpolation()) || "step".equals(current.interpolation())) {
         return current.value();
      }
      if ("catmullrom".equals(next.interpolation()) || "catmullrom".equals(current.interpolation())) {
         Keyframe before = index > 0 ? keyframes.get(index - 1) : current;
         Keyframe after = index + 2 < keyframes.size() ? keyframes.get(index + 2) : next;
         return interpolate(before.value(), current.value(), next.value(), after.value(), t);
      }
      return lerp(current.value(), next.value(), t);
   }

   private static Vec3 interpolate(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
      float t2 = t * t;
      float t3 = t2 * t;
      return Vec3.of(
            catmull(p0.x(), p1.x(), p2.x(), p3.x(), t, t2, t3),
            catmull(p0.y(), p1.y(), p2.y(), p3.y(), t, t2, t3),
            catmull(p0.z(), p1.z(), p2.z(), p3.z(), t, t2, t3));
   }

   private static float catmull(float p0, float p1, float p2, float p3, float t, float t2, float t3) {
      return 0.5F * ((2.0F * p1) + (-p0 + p2) * t
            + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2
            + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3);
   }

   private static Vec3 lerp(Vec3 a, Vec3 b, float t) {
      return Vec3.of(
            a.x() + (b.x() - a.x()) * t,
            a.y() + (b.y() - a.y()) * t,
            a.z() + (b.z() - a.z()) * t);
   }
}
