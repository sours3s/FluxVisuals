package ru.fluxvisuals.module.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.lifecycle.ClientTickEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

/**
 * Dynamic Lights — динамическое освещение от предметов в руке и горящих сущностей.
 * Каждый тик пересобирается кэш источников света, а LightmapTextureManagerMixin
 * добавляет буст блок-света для блоков рядом с источником.
 */
@IModule(name = "Dynamic Lights", description = "Динамическое освещение от факелов и горящих сущностей", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class DynamicLights extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static DynamicLights INSTANCE;

   public final SliderSetting radius = new SliderSetting("Radius", 8.0F, 3.0F, 15.0F, 1.0F, false);
   public final SliderSetting strength = new SliderSetting("Strength", 1.0F, 0.3F, 2.0F, 0.1F, false);
   public final BooleanSetting heldItems = new BooleanSetting("Held Items", true);
   public final BooleanSetting burningEntities = new BooleanSetting("Burning Entities", true);

   public record LightSource(Vec3d pos, int level) {
   }

   private final List<LightSource> cachedSources = new ArrayList<>();

   public DynamicLights() {
      this.addSettings(new Setting[]{radius, strength, heldItems, burningEntities});
      INSTANCE = this;
   }

   public static DynamicLights getInstance() {
      return INSTANCE;
   }

   /** Уровень света предмета в руке (0 = не источник). */
   public int getHeldLightLevel() {
      if (mc.player == null) return 0;
      var stack = mc.player.getMainHandStack();
      if (stack.isOf(Items.TORCH) || stack.isOf(Items.SOUL_TORCH)) return 14;
      if (stack.isOf(Items.LANTERN)) return 15;
      if (stack.isOf(Items.SOUL_LANTERN)) return 10;
      if (stack.isOf(Items.GLOWSTONE)) return 15;
      if (stack.isOf(Items.SHROOMLIGHT)) return 15;
      if (stack.isOf(Items.JACK_O_LANTERN)) return 15;
      if (stack.isOf(Items.CRYING_OBSIDIAN)) return 10;
      if (stack.isOf(Items.GLOW_BERRIES)) return 14;
      return 0;
   }

   @EventInit
   public void onTick(ClientTickEvent e) {
      cachedSources.clear();
      if (!this.enable || mc.player == null || mc.world == null) {
         return;
      }
      if (heldItems.get()) {
         int lvl = getHeldLightLevel();
         if (lvl > 0) {
            cachedSources.add(new LightSource(mc.player.getEyePos(), lvl));
         }
      }
      if (burningEntities.get()) {
         float r = radius.get();
         double rSq = (double) r * r;
         for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (entity.isOnFire() && entity.squaredDistanceTo(mc.player) < rSq * 4.0) {
               cachedSources.add(new LightSource(entity.getEyePos(), 12));
            }
         }
      }
   }

   /**
    * Буст блок-света (0..15) для позиции, зависящий от расстояния до источников.
    * Вызывается из LightmapTextureManagerMixin на горячем пути рендера — поэтому
    * используем только кэш, без скана мира.
    */
   public int getBlockLightBoost(BlockPos pos) {
      if (cachedSources.isEmpty()) {
         return 0;
      }
      float r = radius.get();
      float maxDistSq = r * r;
      int best = 0;
      for (LightSource src : cachedSources) {
         double dx = src.pos().x - (pos.getX() + 0.5);
         double dy = src.pos().y - (pos.getY() + 0.5);
         double dz = src.pos().z - (pos.getZ() + 0.5);
         double distSq = dx * dx + dy * dy + dz * dz;
         if (distSq >= maxDistSq) {
            continue;
         }
         double dist = Math.sqrt(distSq);
         float falloff = (float) (1.0 - dist / r);
         int lvl = (int) (src.level() * falloff * strength.get());
         if (lvl > best) {
            best = lvl;
         }
      }
      return Math.min(15, best);
   }

   public boolean isDynamicLightsEnabled() {
      return this.enable;
   }

   public float getLightRadius() {
      return radius.get();
   }

   public float getLightStrength() {
      return strength.get();
   }
}
