package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.render.WorldRenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderer;

/**
 * Hitboxes — полностью настраиваемые хитбоксы сущностей.
 */
@IModule(name = "Hitboxes", description = "Отображение хитбоксов сущностей", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class Hitboxes extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public final SliderSetting lineWidth = new SliderSetting("Line Width", 2.0F, 0.5F, 5.0F, 0.5F, false);
   public final SliderSetting alpha = new SliderSetting("Alpha", 180.0F, 50.0F, 255.0F, 5.0F, false);
   public final BooleanSetting showPlayers = new BooleanSetting("Players", true);
   public final BooleanSetting showMobs = new BooleanSetting("Mobs", true);
   public final BooleanSetting showAnimals = new BooleanSetting("Animals", true);
   public final BooleanSetting onlyTarget = new BooleanSetting("Only Target", false);
   public final BooleanSetting showEyeLine = new BooleanSetting("Eye Line", false);

   public Hitboxes() {
      this.addSettings(new Setting[]{lineWidth, alpha, showPlayers, showMobs, showAnimals, onlyTarget, showEyeLine});
   }

   @EventInit
   public void onWorld(WorldRenderEvent e) {
      if (!this.enable || mc.player == null || mc.world == null) return;

      WorldRenderer wr = e.worldRenderer();
      Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
      int color = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int) alpha.get());
      double lw = lineWidth.get();

      LivingEntity targetEntity = null;
      if (mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult ehr
            && ehr.getEntity() instanceof LivingEntity le) {
         targetEntity = le;
      }

      for (Entity entity : mc.world.getEntities()) {
         if (entity == mc.player) continue;
         if (!(entity instanceof LivingEntity le)) continue;
         if (!shouldShow(le)) continue;
         if (onlyTarget.get() && le != targetEntity) continue;

         Box box = le.getBoundingBox();
         double ox = -cam.x;
         double oy = -cam.y;
         double oz = -cam.z;

         Vec3d min = new Vec3d(box.minX + ox, box.minY + oy, box.minZ + oz);
         Vec3d max = new Vec3d(box.maxX + ox, box.maxY + oy, box.maxZ + oz);

         // 12 edges
         wr.drawLine(new Vec3d(min.x, min.y, min.z), new Vec3d(max.x, min.y, min.z), lw, color, true);
         wr.drawLine(new Vec3d(max.x, min.y, min.z), new Vec3d(max.x, min.y, max.z), lw, color, true);
         wr.drawLine(new Vec3d(max.x, min.y, max.z), new Vec3d(min.x, min.y, max.z), lw, color, true);
         wr.drawLine(new Vec3d(min.x, min.y, max.z), new Vec3d(min.x, min.y, min.z), lw, color, true);

         wr.drawLine(new Vec3d(min.x, max.y, min.z), new Vec3d(max.x, max.y, min.z), lw, color, true);
         wr.drawLine(new Vec3d(max.x, max.y, min.z), new Vec3d(max.x, max.y, max.z), lw, color, true);
         wr.drawLine(new Vec3d(max.x, max.y, max.z), new Vec3d(min.x, max.y, max.z), lw, color, true);
         wr.drawLine(new Vec3d(min.x, max.y, max.z), new Vec3d(min.x, max.y, min.z), lw, color, true);

         wr.drawLine(new Vec3d(min.x, min.y, min.z), new Vec3d(min.x, max.y, min.z), lw, color, true);
         wr.drawLine(new Vec3d(max.x, min.y, min.z), new Vec3d(max.x, max.y, min.z), lw, color, true);
         wr.drawLine(new Vec3d(max.x, min.y, max.z), new Vec3d(max.x, max.y, max.z), lw, color, true);
         wr.drawLine(new Vec3d(min.x, min.y, max.z), new Vec3d(min.x, max.y, max.z), lw, color, true);

         // Eye line
         if (showEyeLine.get()) {
            double eyeY = box.minY + (box.maxY - box.minY) * le.getStandingEyeHeight() / le.getHeight();
            Vec3d eyePos = new Vec3d((box.minX + box.maxX) / 2.0 + ox, eyeY + oy, (box.minZ + box.maxZ) / 2.0 + oz);
            float yawRad = (float) Math.toRadians(le.getYaw());
            Vec3d lookDir = new Vec3d(-Math.sin(yawRad) * 2.0, 0, Math.cos(yawRad) * 2.0);
            wr.drawLine(eyePos, eyePos.add(lookDir), lw, Renderer2D.ColorUtil.replAlpha(0xFFFF0000, (int) alpha.get()), true);
         }
      }
   }

   private boolean shouldShow(LivingEntity entity) {
      if (entity instanceof PlayerEntity) return showPlayers.get();
      if (entity instanceof net.minecraft.entity.mob.Monster) return showMobs.get();
      return showAnimals.get();
   }
}
