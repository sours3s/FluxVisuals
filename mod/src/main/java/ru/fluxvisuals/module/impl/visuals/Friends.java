package ru.fluxvisuals.module.impl.visuals;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.config.friend.FriendManager;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.render.WorldRenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.render.world.WorldRenderer;

@IModule(name = "Friends", description = "Друзья: зелёный ESP (не сквозь стены), тэг [F], защита от френдли-файра.", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class Friends extends Module {
   public final BooleanSetting esp = new BooleanSetting("Friend ESP", true);
   public final BooleanSetting nameTag = new BooleanSetting("Show [F] Tag", true);
   public final BooleanSetting friendlyFire = new BooleanSetting("Friendly Fire Protection", false);

   @EventInit
   public void onWorldRender(WorldRenderEvent e) {
      if (!enable || !esp.get() || mc.player == null || mc.world == null) return;
      List<AbstractClientPlayerEntity> players = mc.world.getPlayers();
      WorldRenderer wr = e.worldRenderer();
      Matrix4f matrix = e.positionMatrix();

      for (AbstractClientPlayerEntity p : players) {
         if (p == mc.player) continue;
         String name = p.getName().getString();
         if (!FriendManager.isFriend(name)) continue;

         // Check visibility - don't render through walls
         if (!isVisible(p, e.frameDepth())) continue;

         drawFriendBox(wr, matrix, p, e.frameDepth());
      }
   }

   private boolean isVisible(AbstractClientPlayerEntity target, float partialTicks) {
      if (mc.world == null || mc.player == null) {
         return true;
      }
      Vec3d eye = mc.player.getCameraPosVec(partialTicks);
      Vec3d[] points = new Vec3d[] {
              new Vec3d(target.getX(), target.getY() + 0.1, target.getZ()),
              target.getBoundingBox().getCenter(),
              new Vec3d(target.getX(), target.getEyeY(), target.getZ())
      };
      for (Vec3d point : points) {
         RaycastContext ctx = new RaycastContext(eye, point,
                 RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player);
         BlockHitResult hit = mc.world.raycast(ctx);
         if (hit.getType() == HitResult.Type.MISS) {
            return true;
         }
         // Ray reached the point at the target (hit its box) - entity not behind wall.
         if (hit.getPos().squaredDistanceTo(point) < 1.0) {
            return true;
         }
      }
      return false;
   }

   private void drawFriendBox(WorldRenderer wr, Matrix4f matrix, AbstractClientPlayerEntity target, float tickDelta) {
      double x = MathHelper.lerp(tickDelta, target.lastRenderX, target.getX());
      double y = MathHelper.lerp(tickDelta, target.lastRenderY, target.getY());
      double z = MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ());

      Box box = target.getBoundingBox().expand(0.05).offset(x - target.getX(), y - target.getY(), z - target.getZ());

      // WorldRenderer рисует АБСОЛЮТНЫЕ мировые координаты (камера вычитается внутри эмиттера),
      // поэтому передаём box без поправки на позицию камеры.
      int fillColor = ColorUtil.getColor(40, 255, 40, 160);
      int outlineColor = ColorUtil.getColor(0, 255, 0, 255);

      if (wr != null) {
         wr.drawCube(new Vec3d(box.minX, box.minY, box.minZ), new Vec3d(box.maxX, box.maxY, box.maxZ), fillColor, true);
         drawEdges(wr, box, outlineColor);
      }
   }

   private void drawEdges(WorldRenderer wr, Box box, int color) {
      wr.drawLine(new Vec3d(box.minX, box.minY, box.minZ), new Vec3d(box.maxX, box.minY, box.minZ), 2.0, color, true);
      wr.drawLine(new Vec3d(box.maxX, box.minY, box.minZ), new Vec3d(box.maxX, box.minY, box.maxZ), 2.0, color, true);
      wr.drawLine(new Vec3d(box.maxX, box.minY, box.maxZ), new Vec3d(box.minX, box.minY, box.maxZ), 2.0, color, true);
      wr.drawLine(new Vec3d(box.minX, box.minY, box.maxZ), new Vec3d(box.minX, box.minY, box.minZ), 2.0, color, true);
      wr.drawLine(new Vec3d(box.minX, box.maxY, box.minZ), new Vec3d(box.maxX, box.maxY, box.minZ), 2.0, color, true);
      wr.drawLine(new Vec3d(box.maxX, box.maxY, box.minZ), new Vec3d(box.maxX, box.maxY, box.maxZ), 2.0, color, true);
      wr.drawLine(new Vec3d(box.maxX, box.maxY, box.maxZ), new Vec3d(box.minX, box.maxY, box.maxZ), 2.0, color, true);
      wr.drawLine(new Vec3d(box.minX, box.maxY, box.maxZ), new Vec3d(box.minX, box.maxY, box.minZ), 2.0, color, true);
      wr.drawLine(new Vec3d(box.minX, box.minY, box.minZ), new Vec3d(box.minX, box.maxY, box.minZ), 2.0, color, true);
      wr.drawLine(new Vec3d(box.maxX, box.minY, box.minZ), new Vec3d(box.maxX, box.maxY, box.minZ), 2.0, color, true);
      wr.drawLine(new Vec3d(box.maxX, box.minY, box.maxZ), new Vec3d(box.maxX, box.maxY, box.maxZ), 2.0, color, true);
      wr.drawLine(new Vec3d(box.minX, box.minY, box.maxZ), new Vec3d(box.minX, box.maxY, box.maxZ), 2.0, color, true);
   }
}