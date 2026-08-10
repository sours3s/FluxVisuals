package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

/**
 * Dynamic Lights — динамическое освещение от предметов в руке и горящих сущностей.
 * Реализация через модификацию lightmap в LightmapTextureManagerMixin.
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

   public DynamicLights() {
      this.addSettings(new Setting[]{radius, strength, heldItems, burningEntities});
      INSTANCE = this;
   }

   public static DynamicLights getInstance() { return INSTANCE; }

   /** Проверяет, является ли предмет источником света и возвращает уровень. */
   public int getHeldLightLevel() {
      if (!this.enable || !heldItems.get() || mc.player == null) return 0;
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

   public boolean isDynamicLightsEnabled() { return this.enable; }
   public float getLightRadius() { return radius.get(); }
   public float getLightStrength() { return strength.get(); }
}
