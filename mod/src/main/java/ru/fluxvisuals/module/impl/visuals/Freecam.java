package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.lifecycle.ClientTickEvent;
import ru.fluxvisuals.event.render.CameraPositionEvent;
import ru.fluxvisuals.event.player.EventRotation;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.keyboard.ScaledResolution;
import ru.fluxvisuals.util.render.math.animation.AnimationMath;

@IModule(name = "Freecam", description = "Отсоединяет камеру от игрока; движение ограничено — камера не проходит сквозь твёрдые блоки.", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class Freecam extends Module {
   public final SliderSetting speed = new SliderSetting("Speed", 10.0F, 0.5F, 50.0F, 0.5F, false);
   public final SliderSetting smooth = new SliderSetting("Smooth", 10.0F, 1.0F, 30.0F, 0.5F, false);
   public final BooleanSetting renderPlayer = new BooleanSetting("Render Player", true);

   private Vec3d camPos = Vec3d.ZERO;
   private float yaw = 0.0F;
   private float pitch = 0.0F;
   private double velX = 0.0;
   private double velY = 0.0;
   private double velZ = 0.0;

   @Override
   public void onEnable() {
      super.onEnable();
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null) {
         camPos = mc.player.getEyePos();
         yaw = mc.player.getYaw();
         pitch = mc.player.getPitch();
         velX = velY = velZ = 0.0;
         // Блокировать ввод игрока во время фрикама
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
         mc.options.sneakKey.setPressed(false);
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      // Камера вернётся к игроку автоматически через CameraMixin
   }

   @EventInit
   public void onTick(ClientTickEvent e) {
      if (mc.player == null || mc.world == null) return;

      // Вращение камеры следует за поворотом игрока: в игре курсор захвачен,
      // поэтому glfwGetCursorPos не даёт дельт. Игрок поворачивается обычной мышью,
      // а фрикам-камера использует его yaw/pitch.
      yaw = mc.player.getYaw();
      pitch = mc.player.getPitch();

      // WASD + space/shift относительно yaw камеры
      double spd = speed.get() / 20.0; // на тик
      double forward = 0, right = 0, up = 0;
      if (mc.options.forwardKey.isPressed()) forward -= 1;
      if (mc.options.backKey.isPressed()) forward += 1;
      if (mc.options.leftKey.isPressed()) right -= 1;
      if (mc.options.rightKey.isPressed()) right += 1;
      if (mc.options.jumpKey.isPressed()) up += 1;
      if (mc.options.sneakKey.isPressed()) up -= 1;

      double rad = Math.toRadians(yaw);
      double sinYaw = Math.sin(rad);
      double cosYaw = Math.cos(rad);

      // Перемещение в плоскости камеры (игнорируем pitch для XY)
      double moveX = (forward * sinYaw + right * cosYaw) * spd;
      double moveZ = (forward * cosYaw - right * sinYaw) * spd;
      double moveY = up * spd;

      // Интерполяция скорости (плавное ускорение/замедление). onTick — раз в тик (~0.05с).
      double sm = smooth.get() * 0.05 * 0.1;
      velX = AnimationMath.animation((float) velX, (float) moveX, (float) sm);
      velY = AnimationMath.animation((float) velY, (float) moveY, (float) sm);
      velZ = AnimationMath.animation((float) velZ, (float) moveZ, (float) sm);

      // Предложенная новая позиция
      Vec3d proposed = camPos.add(velX, velY, velZ);

      // Коллизия: луч от текущей позиции камеры к предложенной
      RaycastContext ctx = new RaycastContext(
         camPos, proposed, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player
      );
      var hit = mc.world.raycast(ctx);
      if (hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
         // Упёрлись в блок — останавливаемся у грани с небольшим отступом
         Vec3d hitPos = hit.getPos();
         Vec3d dir = proposed.subtract(camPos).normalize();
         camPos = hitPos.subtract(dir.multiply(0.15));
      } else {
         camPos = proposed;
      }

      // Доп. проверка: если внутри блока — отталкиваем к глазам игрока
      BlockPos camBlock = BlockPos.ofFloored(camPos);
      if (!mc.world.getBlockState(camBlock).isAir()) {
         Vec3d toPlayer = mc.player.getEyePos().subtract(camPos).normalize();
         camPos = camPos.add(toPlayer.multiply(0.2));
      }
   }

   @EventInit
   public void onCameraPos(CameraPositionEvent e) {
      if (enable) {
         e.setPosition(camPos);
      }
   }

   @EventInit
   public void onCameraRot(EventRotation e) {
      if (enable) {
         e.setYaw(yaw);
         e.setPitch(pitch);
      }
   }

   // Показать тело игрока на его месте (рендер 3D)
   @EventInit
   public void onRender3D(ru.fluxvisuals.event.render.EventRender3D e) {
      if (!enable || !renderPlayer.get() || mc.player == null) return;
      // В vanila-пейнте рисуется уже через EntityRenderDispatcher; не нужно дублировать.
      // Настройка оставлена как placeholder, если понадобится свой рендер.
   }
}