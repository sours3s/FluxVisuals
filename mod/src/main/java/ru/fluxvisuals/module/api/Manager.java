package ru.fluxvisuals.module.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.module.impl.visuals.AspectRation;
import ru.fluxvisuals.module.impl.visuals.CameraCustomer;
import ru.fluxvisuals.module.impl.visuals.CustomWorld;
import ru.fluxvisuals.module.impl.visuals.ESP;
import ru.fluxvisuals.module.impl.visuals.Gamma;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.module.impl.visuals.ItemESP;
import ru.fluxvisuals.module.impl.visuals.JumpCircle;
import ru.fluxvisuals.module.impl.visuals.NameTags;
import ru.fluxvisuals.module.impl.visuals.NoRender;
import ru.fluxvisuals.module.impl.visuals.GlowCubes;
import ru.fluxvisuals.module.impl.visuals.SwingAnimation;
import ru.fluxvisuals.module.impl.visuals.TargetESP;
import ru.fluxvisuals.module.impl.visuals.Particles;
import ru.fluxvisuals.module.impl.visuals.ProjectilePrediction;
import ru.fluxvisuals.module.impl.visuals.SkinManager;
import ru.fluxvisuals.module.impl.visuals.MenuSettingsModule;
import ru.fluxvisuals.module.impl.visuals.SmoothCamera;
import ru.fluxvisuals.module.impl.visuals.BetterWorld;
import ru.fluxvisuals.module.impl.visuals.BlockOutline;
import ru.fluxvisuals.module.impl.visuals.HitEffect;
import ru.fluxvisuals.module.impl.visuals.ItemPhysics;
import ru.fluxvisuals.module.impl.visuals.KillEffect;
import ru.fluxvisuals.module.impl.visuals.ShaderHand;
import ru.fluxvisuals.module.impl.visuals.GlassHand;
import ru.fluxvisuals.module.impl.visuals.Freecam;
import ru.fluxvisuals.module.impl.visuals.InventoryHUD;
import ru.fluxvisuals.module.impl.visuals.Friends;
import ru.fluxvisuals.module.impl.utils.LagDetector;
import ru.fluxvisuals.module.impl.utils.SoundFX;
import ru.fluxvisuals.module.impl.utils.ChatSounds;
import ru.fluxvisuals.module.impl.visuals.Cosmetic;
import ru.fluxvisuals.module.impl.utils.ClientSound;
import ru.fluxvisuals.module.impl.utils.HitSound;
import ru.fluxvisuals.module.impl.utils.DiscordRCP;

@Environment(EnvType.CLIENT)
public class Manager {
   public ArrayList<Module> module = new ArrayList<>();

   // Cache for fast lookups
   private final Map<Class<? extends Module>, Module> moduleByClass = new HashMap<>();
   private final Map<Integer, List<Module>> modulesByBind = new HashMap<>();

   public Manager() {
      // Только легитные визуальные модули. Никаких combat/movement/player хаков.
      this.module.add(new Hud());
      this.module.add(new AspectRation());
      this.module.add(new CameraCustomer());
      this.module.add(new CustomWorld());
      this.module.add(new ESP());
      this.module.add(new Gamma());
      this.module.add(new Cosmetic());
      this.module.add(new ItemESP());
      this.module.add(new JumpCircle());
      this.module.add(new NameTags());
      this.module.add(new NoRender());
      this.module.add(new GlowCubes());
      this.module.add(new SwingAnimation());
      this.module.add(new TargetESP());
      this.module.add(new Particles());
      this.module.add(new ProjectilePrediction());
      this.module.add(new SkinManager());
      this.module.add(new MenuSettingsModule());
      this.module.add(new SmoothCamera());
      this.module.add(new BetterWorld());
      this.module.add(new BlockOutline());
      this.module.add(new HitEffect());
      this.module.add(new ItemPhysics());
      this.module.add(new KillEffect());
      this.module.add(new ShaderHand());
      this.module.add(new GlassHand());
      this.module.add(new Freecam());
      this.module.add(new InventoryHUD());
      this.module.add(new Friends());
      this.module.add(new ClientSound());
      this.module.add(new HitSound());
      this.module.add(new DiscordRCP());
      this.module.add(new LagDetector());
      this.module.add(new SoundFX());
      this.module.add(new ChatSounds());

      // === Новые модули (Etaps 2-4) ===
      this.module.add(new ru.fluxvisuals.module.impl.visuals.MaceHelper());
      this.module.add(new ru.fluxvisuals.module.impl.visuals.ReachCircle());
      this.module.add(new ru.fluxvisuals.module.impl.visuals.Keystrokes());
      this.module.add(new ru.fluxvisuals.module.impl.visuals.ArmorStatus());
      this.module.add(new ru.fluxvisuals.module.impl.visuals.BetterPing());
      this.module.add(new ru.fluxvisuals.module.impl.visuals.Zoom());
      this.module.add(new ru.fluxvisuals.module.impl.visuals.Hitboxes());
      this.module.add(new ru.fluxvisuals.module.impl.visuals.DynamicLights());
      this.module.add(new ru.fluxvisuals.module.impl.visuals.EntityCulling());
      this.module.add(new ru.fluxvisuals.module.impl.visuals.CameraOverhaul());

      this.module.add(new ru.fluxvisuals.module.impl.visuals.MotionBlur());
      this.module.add(new ru.fluxvisuals.module.impl.visuals.FirstPersonModel());
      this.module.add(new ru.fluxvisuals.module.impl.utils.HitDelayFix());
      this.module.add(new ru.fluxvisuals.module.impl.utils.AppleSkin());
      this.module.add(new ru.fluxvisuals.module.impl.utils.BetterF3());
      this.module.add(new ru.fluxvisuals.module.impl.utils.PearlClutchHelper());

      this.module.sort(java.util.Comparator.comparing(m -> m.name));

      // Build caches
      rebuildCaches();
   }

   private void rebuildCaches() {
      moduleByClass.clear();
      modulesByBind.clear();
      for (Module m : module) {
         moduleByClass.put(m.getClass(), m);
         modulesByBind.computeIfAbsent(m.bind, k -> new ArrayList<>()).add(m);
      }
   }

   public ArrayList<Module> getModules() {
      return this.module;
   }

   public <T extends Module> T get(Class<T> clazz) {
      // Direct lookup is much faster than stream + filter
      Module m = moduleByClass.get(clazz);
      if (m != null) {
         return clazz.cast(m);
      }
      // Fallback for subclasses (rare)
      for (Module mod : module) {
         if (clazz.isAssignableFrom(mod.getClass())) {
            return clazz.cast(mod);
         }
      }
      return null;
   }

   public Module getModule(Class<?> class1) {
      return moduleByClass.get(class1);
   }

   public ArrayList<Module> getType(Category category) {
      ArrayList<Module> modules = new ArrayList<>();
      for (Module module1 : this.module) {
         if (module1.category == category) {
            modules.add(module1);
         }
      }
      return modules;
   }

   public Module[] getBind(int bind) {
      List<Module> list = modulesByBind.get(bind);
      if (list != null) {
         return list.toArray(new Module[0]);
      }
      return new Module[0];
   }

   // Call when modules list changes
   public void invalidateCaches() {
      rebuildCaches();
   }
}