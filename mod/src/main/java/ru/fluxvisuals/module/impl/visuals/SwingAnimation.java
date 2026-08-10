package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Arm;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.render.HandAnimationEvent;
import ru.fluxvisuals.event.render.RenderItemEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

@IModule(name = "Swing Animation", description = "Customizes item swinging animations and weapon positioning", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class SwingAnimation extends Module {
   public static ModeSetting swingMode = new ModeSetting("Animation Mode", "Swipe", "Swipe", "Down", "Smooth", "Power", "Feast", "Stab", "Overhead", "Cross", "Slash", "Thrust", "Lunge", "Sigma", "Combo", "Flux", "Off");

   // Combo state tracking (SwordAttacks: RTL -> LTR -> FWD cycle)
   private static int comboState = 0;
   private static float lastSwingProgress = 0.0F;
   public static SliderSetting animSpeed = new SliderSetting("Swing Speed", 1.0F, 0.1F, 3.0F, 0.1F, false);
   public static SliderSetting animgsize = new SliderSetting("Swing Scale", 3.7F, 1.0F, 10.0F, 0.1F, false);
   public static BooleanSetting auraOnly = new BooleanSetting("Aura Only", false);
   public static BooleanSetting customhands = new BooleanSetting("Custom Hand View", false);
   public static BooleanSetting zemetria = new BooleanSetting("Sync Both Hands", false)
         .hidden(() -> !customhands.get());
   public static SliderSetting x = new SliderSetting("X", 0.0F, -2.0F, 2.0F, 0.01F, false)
         .hidden(() -> !customhands.get() || !zemetria.get());
   public static SliderSetting y = new SliderSetting("Y", 0.0F, -2.0F, 2.0F, 0.01F, false)
         .hidden(() -> !customhands.get() || !zemetria.get());
   public static SliderSetting z = new SliderSetting("Z", 0.0F, -2.0F, 2.0F, 0.01F, false)
         .hidden(() -> !customhands.get() || !zemetria.get());
   public static SliderSetting right_x = new SliderSetting("Right X", 0.0F, -2.0F, 2.0F, 0.01F, false)
         .hidden(() -> !customhands.get() || zemetria.get());
   public static SliderSetting right_y = new SliderSetting("Right Y", 0.0F, -2.0F, 2.0F, 0.01F, false)
         .hidden(() -> !customhands.get() || zemetria.get());
   public static SliderSetting right_z = new SliderSetting("Right Z", 0.0F, -2.0F, 2.0F, 0.01F, false)
         .hidden(() -> !customhands.get() || zemetria.get());
   public static SliderSetting lefvt_x = new SliderSetting("Left X", 0.0F, -2.0F, 2.0F, 0.01F, false)
         .hidden(() -> !customhands.get() || zemetria.get());
   public static SliderSetting lefvt_y = new SliderSetting("Left Y", 0.0F, -2.0F, 2.0F, 0.01F, false)
         .hidden(() -> !customhands.get() || zemetria.get());
   public static SliderSetting lefvt_z = new SliderSetting("Left Z", 0.0F, -2.0F, 2.0F, 0.01F, false)
         .hidden(() -> !customhands.get() || zemetria.get());

   public SwingAnimation() {
      this.addSettings(
            new Setting[] { swingMode, animSpeed, animgsize, auraOnly, customhands, zemetria, x, y, z, right_x, right_y,
                  right_z, lefvt_x, lefvt_y, lefvt_z });
   }

   @EventInit
   public void onEvent(HandAnimationEvent event) {
      if (!this.enable) return;

      if (!isCustomAnimationActive() || event.getHand() != Hand.MAIN_HAND) {
         return;
      }

      MatrixStack matrix = event.getMatrices();
      float swingProgress = event.getSwingProgress();
      float equipProgress = event.getEquipProgress();
      float strength = animgsize.get();
      int armSide = mc.player.getMainArm() == Arm.RIGHT ? 1 : -1;
      float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
      float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
      float sinSmooth = MathHelper.sin(swingProgress * (float) Math.PI) * 0.5F;

      matrix.translate(-0.56F * armSide, 0.52F + equipProgress * 0.6F, 0.72F);

      switch (swingMode.get()) {
         case "Swipe":
            matrix.translate(0.56F * armSide, -0.32F, -0.72F);
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(60.0F * armSide));
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60.0F * armSide));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5.0F * strength));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -120.0F * strength));
            break;
         case "Down":
            matrix.translate(armSide * 0.56F, -0.32F, -0.72F);
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76.0F * armSide));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0F * strength));
            matrix.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0F * strength));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0F * strength));
            break;
         case "Smooth":
            matrix.translate(armSide * 0.56F, -0.42F, -0.72F);
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSide * 45.0F));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSide * -45.0F));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSide * sin1 * -20.0F * strength));
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSide * sin2 * -20.0F * strength));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
            matrix.translate(0.0F, -0.1F * strength, 0.0F);
            break;
         case "Power":
            matrix.translate(armSide * 0.56F, -0.32F, -0.72F);
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61.0F * armSide));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
            matrix.translate((-sinSmooth * sinSmooth * sin1) * armSide * strength, 0.0F, 0.0F);
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * strength));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5.0F * strength));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -30.0F * strength));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60.0F * strength));
            break;
         case "Feast":
            matrix.translate(armSide * 0.56F, -0.32F, -0.72F);
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * armSide));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * armSide));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * armSide));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75.0F * armSide * strength));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45.0F * strength));
            break;
         case "Stab":
            matrix.translate(armSide * 0.56F, -0.52F, -0.72F);
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -60.0F * strength));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -30.0F * armSide * strength));
            break;
         case "Overhead":
            matrix.translate(armSide * 0.56F, equipProgress * -0.2F - 0.5F, -0.7F);
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * armSide));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -85.0F * strength));
            matrix.translate(armSide * -0.1F, 0.28F, 0.2F);
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85.0F));
            break;
         case "Cross":
            applyZenithEquipOffset(matrix, armSide);
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50.0F));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60.0F * armSide));
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((110.0F + 20.0F * sin2 * strength) * armSide));
            break;
         case "Slash":
            applyZenithEquipOffset(matrix, armSide);
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50.0F));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((-30.0F * (1.0F - sin2) - 30.0F) * armSide));
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110.0F * armSide));
            break;
         case "Thrust":
            matrix.translate(armSide * 0.56F, -0.52F, -0.72F);
            matrix.translate(armSide * 0.1F, -0.2F, -0.3F);
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((-30.0F * MathHelper.sin(swingProgress * (float) Math.PI) * strength) - 36.0F));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * MathHelper.sin(swingProgress * (float) Math.PI) * armSide * strength));
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(12.0F * armSide));
            break;
         case "Lunge":
            applyZenithEquipOffset(matrix, armSide);
            matrix.translate(0.0F, -0.2F, -0.4F);
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((-120.0F * sin2 * strength) - 3.0F));
            break;
         case "Sigma": {
            // Based on applyArmMatrices from HeldItemRendererMixin
            float sqrtSwing = MathHelper.sqrt(swingProgress);
            float hOff = -0.3F * MathHelper.sin(sqrtSwing * (float) Math.PI);
            float iOff = 0.4F * MathHelper.sin(sqrtSwing * (float) Math.PI * 2.0F);
            float jOff = -0.4F * MathHelper.sin(swingProgress * (float) Math.PI);

            matrix.translate(armSide * 0.56F, -0.52F, -0.72F);
            matrix.translate(armSide * hOff, iOff, jOff);
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSide * 45.0F));

            float kSigma = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
            float lSigma = MathHelper.sin(sqrtSwing * (float) Math.PI);
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSide * lSigma * 70.0F * strength));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(armSide * kSigma * -20.0F * strength));
            break;
         }
         case "Combo": {
            // Based on SwordAttacks enum: RTL -> LTR -> FWD cycling
            if (swingProgress > 0.0F && swingProgress < 0.15F && lastSwingProgress > 0.5F) {
               comboState = (comboState + 1) % 3;
            }
            lastSwingProgress = swingProgress;

            switch (comboState) {
               case 0: // RTL - Right to Left diagonal swing
                  matrix.translate(armSide * 0.56F, -0.32F, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(60.0F * armSide));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45.0F * armSide));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 80.0F * armSide * strength));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * -30.0F * strength));
                  break;
               case 1: // LTR - Left to Right diagonal swing
                  matrix.translate(armSide * 0.56F, -0.32F, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(60.0F * armSide));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45.0F * armSide));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -80.0F * armSide * strength));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * 30.0F * strength));
                  break;
               case 2: // FWD - Forward thrust
                  matrix.translate(armSide * 0.56F, -0.52F, -0.72F);
                  matrix.translate(armSide * 0.1F, -0.15F, -0.25F);
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -90.0F * strength - 30.0F));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 20.0F * armSide * strength));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(15.0F * armSide));
                  break;
            }
            break;
         }
         case "Flux": {
            // Based on itemPose + mainHandPose positioning from HeldItemRendererMixin
            float sqrtFlux = MathHelper.sqrt(swingProgress);
            float fluxSin = MathHelper.sin(sqrtFlux * (float) Math.PI);
            // easeOutBack overshoot from Easings
            double eased = swingProgress < 1.0F
                  ? 1.0 + 2.70158 * Math.pow(swingProgress - 1.0, 3.0) + 1.70158 * Math.pow(swingProgress - 1.0, 2.0)
                  : 1.0;

            matrix.translate(armSide * 0.56F, -0.42F, -0.72F);
            matrix.translate(0.0F, (float) (-0.15 * eased), (float) (-0.3 * eased));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * armSide));
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(35.0F * armSide * fluxSin * strength));
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-65.0F * fluxSin * strength));
            matrix.translate(0.0F, 0.1F * fluxSin * strength, 0.2F * fluxSin * strength);
            break;
         }
         default:
            break;
      }

      event.cancel();
   }

   @EventInit
   public void onEvent(RenderItemEvent event) {
      if (!this.enable || !customhands.get()) return;

      MatrixStack matrix = event.getMatrix();
      Hand hand = event.getHand();
      ItemStack stack = event.getStack();

      if (hand == Hand.MAIN_HAND && stack != null && stack.getItem() instanceof CrossbowItem) {
         return;
      }

      if (zemetria.get()) {
         if (hand == Hand.MAIN_HAND) {
            matrix.translate(x.get(), y.get(), z.get());
         } else {
            matrix.translate(-x.get(), y.get(), z.get());
         }
      } else {
         if (hand == Hand.MAIN_HAND) {
            matrix.translate(right_x.get(), right_y.get(), right_z.get());
         } else {
            matrix.translate(lefvt_x.get(), lefvt_y.get(), lefvt_z.get());
         }
      }
   }

   public static boolean isCustomAnimationActive() {
      if (net.minecraft.client.MinecraftClient.getInstance().player != null && net.minecraft.client.MinecraftClient.getInstance().player.getMainHandStack().isEmpty()) {
         return false;
      }
      return !swingMode.is("Off") && auraCheck();
   }

   public static boolean auraCheck() {
      if (!auraOnly.get()) {
         return true;
      } else {
         // Легитная проверка: анимация активна, когда игрок навёл прицел на сущность.
         net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
         return mc != null && mc.player != null && mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult;
      }
   }

   private static void applyZenithEquipOffset(MatrixStack matrices, int armSide) {
      matrices.translate(armSide * 0.56F, -0.52F, -0.72F);
   }
}