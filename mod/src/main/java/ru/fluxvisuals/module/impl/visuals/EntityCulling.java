package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

/**
 * Entity Culling — умная отсечка невидимых за блоками сущностей для повышения FPS.
 */
@IModule(name = "Entity Culling", description = "Не рендерит сущности за стенами (повышение FPS)", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class EntityCulling extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static EntityCulling INSTANCE;

   public final SliderSetting maxDistance = new SliderSetting("Max Distance", 32.0F, 8.0F, 64.0F, 4.0F, false);
   public final BooleanSetting skipPlayers = new BooleanSetting("Skip Players", false);
   public final BooleanSetting skipHostile = new BooleanSetting("Skip Hostile", true);
   public final BooleanSetting skipPassive = new BooleanSetting("Skip Passive", true);

   public EntityCulling() {
      this.addSettings(new Setting[]{maxDistance, skipPlayers, skipHostile, skipPassive});
      INSTANCE = this;
   }

   public static EntityCulling getInstance() { return INSTANCE; }

   /** Проверяет, должен ли быть отрендерен этот entity. */
   public boolean shouldRender(Entity entity) {
      if (!this.enable || mc.player == null || mc.world == null) return true;
      if (entity == mc.player) return true;
      if (!(entity instanceof LivingEntity le)) return true;

      double dist = mc.player.distanceTo(entity);
      if (dist > maxDistance.get()) return false;

      if (le instanceof PlayerEntity && !skipPlayers.get()) return true;

      // Check if entity is behind opaque blocks
      Vec3d eyePos = mc.player.getEyePos();
      Vec3d entityCenter = le.getBoundingBox().getCenter();
      BlockHitResult result = mc.world.raycast(new RaycastContext(
         eyePos, entityCenter,
         RaycastContext.ShapeType.COLLIDER,
         RaycastContext.FluidHandling.NONE,
         mc.player
      ));
      if (result.getType() == HitResult.Type.BLOCK) {
         // Blocked by a block — skip render
         return false;
      }
      return true;
   }
}
