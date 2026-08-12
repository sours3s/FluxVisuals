package ru.fluxvisuals.module.impl.visuals;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.HueSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.shader.ClientPipelines;
import ru.fluxvisuals.util.render.shader.ShaderFogMode;
import ru.fluxvisuals.util.render.shader.ShaderFogPipeline;

import java.awt.Color;

/**
 * Заменяет обычное небо шейдерным туманом (перенесено из GodWeer).
 * Рендер вызывается из WorldRendererMixin.renderSky.
 */
@IModule(
   name = "Shader Fog",
   description = "Заменяет небо шейдерным туманом",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class ShaderFog extends Module {
   public static final ModeSetting mode = new ModeSetting(
      "Mode", "Caustic", "Caustic", "Drain", "Nebula", "Plasma", "Bloom"
   );
   public static final SliderSetting speed = new SliderSetting("Speed", 1.0F, 0.1F, 5.0F, 0.1F, false);
   public static final SliderSetting scale = new SliderSetting("Scale", 5.0F, 1.0F, 20.0F, 0.5F, false);
   public static final SliderSetting intensity = new SliderSetting("Intensity", 0.01F, 0.001F, 0.05F, 0.001F, false);
   public static final SliderSetting alpha = new SliderSetting("Alpha", 1.0F, 0.3F, 1.0F, 0.05F, false);
   public static final HueSetting color = new HueSetting("Color", 0.6F, 1.0F, 1.0F);

   private final ShaderFogPipeline pipeline = new ShaderFogPipeline();
   private long startMillis = -1;

   public ShaderFog() {
      // Обращение к одному статическому полю инициализирует весь класс —
      // так все SKY-пайплайны регистрируются в RenderPipelines при старте.
      ClientPipelines.SKY_CAUSTIC_PIPELINE.toString();
      this.addSettings(new Setting[]{mode, speed, scale, intensity, alpha, color});
   }

   @Override
   public void onDisable() {
      startMillis = -1;
      if (mc.worldRenderer != null) mc.worldRenderer.reload();
   }

   public void renderShader() {
      if (!enable || mc.player == null || mc.world == null) return;
      if (startMillis < 0) startMillis = System.currentTimeMillis();

      float time = (System.currentTimeMillis() - startMillis) / 1000.0f;
      float fw = mc.getWindow().getFramebufferWidth();
      float fh = mc.getWindow().getFramebufferHeight();

      Color c = color.getColor();
      Camera cam = mc.gameRenderer.getCamera();
      float yawRad = (float) Math.toRadians(-cam.getYaw());
      float pitchRad = (float) Math.toRadians(cam.getPitch());
      float fov = (float) mc.options.getFov().getValue().intValue();

      ShaderFogMode m = switch (mode.get()) {
         case "Drain" -> ShaderFogMode.DRAIN;
         case "Nebula" -> ShaderFogMode.NEBULA;
         case "Plasma" -> ShaderFogMode.PLASMA;
         case "Bloom" -> ShaderFogMode.BLOOM;
         default -> ShaderFogMode.CAUSTIC;
      };

      GpuTextureView screenTexture = mc.getFramebuffer().getColorAttachmentView();
      if (screenTexture != null) {
         pipeline.render(screenTexture, m, (int) fw, (int) fh, yawRad, pitchRad, c,
               time, alpha.get(), speed.get(), scale.get(), intensity.get(), fov);
      }
   }
}
