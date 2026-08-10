package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.render.CameraPositionEvent;
import ru.fluxvisuals.event.player.EventRotation;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BindSettings;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.keyboard.ScaledResolution;
import ru.fluxvisuals.util.render.math.animation.AnimationMath;

@IModule(name = "CameraCustomer", description = "Настройка камеры: зум, FOV, аспект, трепетание, сглаживание", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class CameraCustomer extends Module {
   // Zoom
   public final ModeSetting zoomMode = new ModeSetting("Zoom Mode", "HOLD", "TOGGLE", "HOLD");
   public final BindSettings zoomBind = new BindSettings("Zoom Key", -1);
   public final SliderSetting zoomAmount = new SliderSetting("Zoom Amount", 6.0F, 1.0F, 100.0F, 0.5F, false);
   public final SliderSetting zoomScroll = new SliderSetting("Zoom Scroll", 1.0F, 0.0F, 10.0F, 0.1F, false);
   public final BooleanSetting zoomSmooth = new BooleanSetting("Zoom Smooth", true);
   public final SliderSetting zoomSmoothSpeed = new SliderSetting("Zoom Smooth Speed", 150.0F, 50.0F, 1000.0F, 10.0F, false);
   public final BooleanSetting zoomCinematic = new BooleanSetting("Zoom Cinematic", false);
   public final BooleanSetting zoomHands = new BooleanSetting("Zoom Hands", false);

   // Camera
   public final SliderSetting cameraDistance = new SliderSetting("Camera Distance", 4.0F, 0.0F, 20.0F, 0.5F, false);
   public final BooleanSetting cameraShake = new BooleanSetting("Camera Shake", false);
   public final SliderSetting shakeIntensity = new SliderSetting("Shake Intensity", 1.0F, 0.1F, 5.0F, 0.1F, false).hidden(() -> !cameraShake.get());

   // FOV
   public final BooleanSetting fovEnabled = new BooleanSetting("Custom FOV", false);
   public final SliderSetting fovValue = new SliderSetting("FOV Value", 90.0F, 30.0F, 160.0F, 1.0F, false).hidden(() -> !fovEnabled.get());
   public final BooleanSetting fovSmooth = new BooleanSetting("Smooth FOV", true).hidden(() -> !fovEnabled.get());

   // Aspect Ratio
   public final ModeSetting aspect = new ModeSetting(
         "Screen Ratio", "16:9", "16:9", "4:3", "1:1", "16:10", "21:9", "32:9", "5:4", "2:1", "Custom"
   );
   public final SliderSetting customAspect = new SliderSetting("Custom Value", 2.0F, 1.0F, 3.0F, 0.1F, false).hidden(() -> !aspect.is("Custom"));

   // State
   private boolean zoomActive = false;
   private boolean prevCinematic = false;
   private double prevSensitivity = 1.0;
   private double zoomAnimCurrent = 1.0;
   private double zoomAnimTarget = 1.0;
   private long zoomAnimLastMs = 0;
   private float fovAnimCurrent = 90.0F;
   private float fovAnimTarget = 90.0F;

   public CameraCustomer() {
      this.addSettings(new Setting[]{
            zoomMode, zoomBind, zoomAmount, zoomScroll, zoomSmooth, zoomSmoothSpeed, zoomCinematic, zoomHands,
            cameraDistance, cameraShake, shakeIntensity,
            fovEnabled, fovValue, fovSmooth,
            aspect, customAspect
      });
   }

   @Override
   public void onEnable() {
      super.onEnable();
      zoomActive = false;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      if (zoomActive) {
         deactivateZoom();
      }
   }

   @EventInit
   public void onTick() {
      if (mc.player == null) return;
      GameOptions options = mc.options;

      // Zoom key handling
      if (zoomBind.isKeyDown(zoomBind.get())) {
         if (!zoomActive) {
            if (zoomMode.is("TOGGLE")) {
               activateZoom();
            } else {
               activateZoom();
            }
         }
      } else if (zoomMode.is("HOLD") && zoomActive) {
         deactivateZoom();
      }

      // Smooth zoom animation
      if (zoomSmooth.get() && (zoomActive || zoomAnimCurrent != zoomAnimTarget)) {
         long now = System.currentTimeMillis();
         double elapsed = (now - zoomAnimLastMs) / (double) zoomSmoothSpeed.get();
         zoomAnimLastMs = now;
         zoomAnimTarget = zoomActive ? zoomAmount.get() : 1.0;
         float speed = (float) Math.min(elapsed * 8.0, 1.0);
         zoomAnimCurrent = AnimationMath.animation((float) zoomAnimCurrent, (float) zoomAnimTarget, speed);
      } else {
         zoomAnimCurrent = zoomActive ? zoomAmount.get() : 1.0;
      }

      // Cinematic camera
      if (zoomActive && zoomCinematic.get()) {
         options.smoothCameraEnabled = true;
      } else if (!zoomActive && zoomCinematic.get()) {
         options.smoothCameraEnabled = prevCinematic;
      }

      // Sensitivity compensation
      if (zoomActive && !zoomCinematic.get()) {
         double scaling = Math.max(getZoomScaling() * 0.5, 1.0);
         options.getMouseSensitivity().setValue(prevSensitivity / scaling);
      }
   }

   @EventInit
   public void onCameraPos(CameraPositionEvent e) {
      if (enable && zoomActive) {
         // Apply camera distance when zoomed
         double distance = cameraDistance.get();
         if (distance > 0) {
            // Move camera back along its direction
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();
            double radYaw = Math.toRadians(yaw);
            double radPitch = Math.toRadians(pitch);
            double dx = -Math.sin(radYaw) * Math.cos(radPitch) * distance;
            double dy = Math.sin(radPitch) * distance;
            double dz = Math.cos(radYaw) * Math.cos(radPitch) * distance;
            Vec3d currentPos = e.getPosition();
            e.setPosition(new Vec3d(currentPos.x + dx, currentPos.y + dy, currentPos.z + dz));
         }
      }
   }

   @EventInit
   public void onCameraRot(EventRotation e) {
      if (enable && zoomActive && cameraShake.get()) {
         // Apply camera shake
         float intensity = shakeIntensity.get();
         float shakeYaw = (float) (Math.sin(System.currentTimeMillis() * 0.01) * intensity * 0.5);
         float shakePitch = (float) (Math.cos(System.currentTimeMillis() * 0.013) * intensity * 0.3);
         e.setYaw(e.getYaw() + shakeYaw);
         e.setPitch(e.getPitch() + shakePitch);
      }
   }

   private void activateZoom() {
      if (zoomActive) return;
      zoomActive = true;
      zoomAnimTarget = zoomAmount.get();
      zoomAnimLastMs = System.currentTimeMillis();
      prevCinematic = mc.options.smoothCameraEnabled;
      prevSensitivity = mc.options.getMouseSensitivity().getValue();
   }

   private void deactivateZoom() {
      if (!zoomActive) return;
      zoomActive = false;
      zoomAnimTarget = 1.0;
      zoomAnimLastMs = System.currentTimeMillis();
      mc.options.smoothCameraEnabled = prevCinematic;
      mc.options.getMouseSensitivity().setValue(prevSensitivity);
   }

   public double getZoomScaling() {
      if (!zoomSmooth.get()) {
         zoomAnimCurrent = zoomActive ? zoomAmount.get() : 1.0;
         return zoomAnimCurrent;
      }
      return zoomAnimCurrent;
   }

   public static float getAspectRatio() {
      if (!FluxVisualsClient.get.manager.getModule(CameraCustomer.class).enable) {
         return 0.0F;
      }
      ScaledResolution sr = new ScaledResolution(mc);
      Module mod = FluxVisualsClient.get.manager.getModule(CameraCustomer.class);
      if (!(mod instanceof CameraCustomer)) {
         return 0.0F;
      }
      CameraCustomer cam = (CameraCustomer) mod;
      String var3 = cam.aspect.get();

      float newAspect = switch (var3) {
         case "16:9" -> 1.7777778F;
         case "4:3" -> 1.3333334F;
         case "1:1" -> 1.0F;
         case "16:10" -> 1.6F;
         case "21:9" -> 2.3333333F;
         case "32:9" -> 3.5555556F;
         case "5:4" -> 1.25F;
         case "2:1" -> 2.0F;
         default -> cam.customAspect.get();
      };

      float aspect1 = (float) sr.getWidth() / sr.getHeight();
      return aspect1 / newAspect;
   }

   public boolean isZoomActive() {
      return zoomActive;
   }

   public double getCurrentZoom() {
      return zoomAnimCurrent;
   }
}