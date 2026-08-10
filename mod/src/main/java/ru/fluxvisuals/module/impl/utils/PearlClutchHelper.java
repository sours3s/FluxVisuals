package ru.fluxvisuals.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Pearl Clutch Helper — предсказание приземления эндер-перла и безопасность клатча.
 */
@IModule(name = "Pearl Clutch Helper", description = "Помощь при бросках эндер-перлов и спасительных клачах", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class PearlClutchHelper extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public final BooleanSetting showLanding = new BooleanSetting("Show Landing", true);
   public final BooleanSetting showSafety = new BooleanSetting("Show Safety", true);

   public PearlClutchHelper() {
      this.addSettings(new Setting[]{showLanding, showSafety});
   }

   @EventInit
   public void onRender(EventScreen e) {
      if (!this.enable || mc.player == null || mc.world == null) return;
      if (!(mc.player.getMainHandStack().getItem() instanceof EnderPearlItem) &&
          !(mc.player.getOffHandStack().getItem() instanceof EnderPearlItem)) return;

      Renderer2D r2 = e.renderer();
      if (r2 == null) return;

      PlayerEntity player = mc.player;
      Vec3d eyePos = player.getEyePos();
      Vec3d look = player.getRotationVec(1.0F);

      // Raycast forward to find landing spot
      Vec3d end = eyePos.add(look.multiply(48.0));
      HitResult result = mc.world.raycast(new RaycastContext(
         eyePos, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
      ));

      if (result.getType() != HitResult.Type.BLOCK) return;

      BlockPos landPos = ((BlockHitResult) result).getBlockPos();
      double landY = landPos.getY() + 1.0;
      double fallDistance = player.getY() - landY;

      float centerX = mc.getWindow().getScaledWidth() / 2.0F;
      float baseY = mc.getWindow().getScaledHeight() / 2.0F + 20.0F;

      if (showSafety.get()) {
         boolean safe = fallDistance < 23.0; // Safe fall distance
         String label = safe ? "SAFE CLUTCH" : "DANGER!";
         int color = safe ? 0xFF55FF55 : 0xFFFF5555;
         r2.text(FontRegistry.INTER_SEMIBOLD, centerX - 30.0F, baseY, 10.0F, label, color);

         String distStr = String.format("Fall: %.0f blocks", fallDistance);
         r2.text(FontRegistry.INTER_MEDIUM, centerX - 35.0F, baseY + 12.0F, 8.0F, distStr, 0xFFAAAAAA);
      }

      if (showLanding.get()) {
         String landStr = String.format("Landing: %.0f, %.0f, %.0f", landPos.getX(), landY, landPos.getZ());
         r2.text(FontRegistry.INTER_MEDIUM, centerX - 50.0F, baseY + 22.0F, 8.0F, landStr, 0xFF8888FF);
      }
   }
}
