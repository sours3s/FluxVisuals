package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.render.WorldRenderEvent;
import ru.fluxvisuals.util.render.world.WorldRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Lights world renderer — рендерит сферы света вокруг источников
 * (игрок с факелом, горящие сущности) через WorldRenderEvent.
 *
 * Использует полупрозрачные квады-сферы для визуального освещения.
 */
@Environment(EnvType.CLIENT)
public class DynamicLightsWorldRenderer {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static DynamicLightsWorldRenderer INSTANCE;

   public DynamicLightsWorldRenderer() {
      INSTANCE = this;
   }

   public static DynamicLightsWorldRenderer getInstance() { return INSTANCE; }

   @EventInit
   public void onWorld(WorldRenderEvent e) {
      DynamicLights module = null;
      if (ru.fluxvisuals.client.FluxVisualsClient.get != null
            && ru.fluxvisuals.client.FluxVisualsClient.get.manager != null) {
         module = ru.fluxvisuals.client.FluxVisualsClient.get.manager.get(DynamicLights.class);
      }
      if (module == null || !module.enable) return;
      if (mc.player == null || mc.world == null) return;

      WorldRenderer wr = e.worldRenderer();
      Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

      // Collect light sources
      List<Vec3d> lightSources = new ArrayList<>();

      // Held light items
      if (module.heldItems.get()) {
         int lightLevel = module.getHeldLightLevel();
         if (lightLevel > 0) {
            lightSources.add(new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
         }
      }

      // Burning entities
      if (module.burningEntities.get()) {
         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity le && le.isOnFire() && entity != mc.player) {
               double dist = mc.player.distanceTo(entity);
               if (dist < module.getLightRadius() * 2) {
                  lightSources.add(new Vec3d(entity.getX(), entity.getY(), entity.getZ()));
               }
            }
         }
      }

      if (lightSources.isEmpty()) return;

      float radius = module.getLightRadius() * 0.15F;
      float str = module.getLightStrength();

      for (Vec3d pos : lightSources) {
         Vec3d rel = pos.add(0, 0.5, 0).subtract(cam);

         // Render a simple cross of translucent quads for visual glow
         int alpha = (int) (120 * str);
         int color = (alpha << 24) | 0xFFDDAA00;

         double r = radius;
         // Horizontal cross
         wr.drawQuad(
            rel.add(-r, 0, 0), rel.add(r, 0, 0),
            rel.add(r, 0, 0.01), rel.add(-r, 0, 0.01),
            color, false);
         wr.drawQuad(
            rel.add(0, 0, -r), rel.add(0, 0, r),
            rel.add(0.01, 0, r), rel.add(0.01, 0, -r),
            color, false);
         // Vertical cross
         wr.drawQuad(
            rel.add(0, -r, 0), rel.add(0, r, 0),
            rel.add(0.01, r, 0), rel.add(0.01, -r, 0),
            color, false);
         wr.drawQuad(
            rel.add(-r, 0, 0), rel.add(r, 0, 0),
            rel.add(r, 0.01, 0), rel.add(-r, 0.01, 0),
            color, false);
      }
   }
}
