package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;

@IModule(name = "Item Physics", description = "Добавляет предметам на земле физику", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class ItemPhysics extends Module {
   private static ItemPhysics instance;

   public ItemPhysics() {
      instance = this;
   }

   public static ItemPhysics getInstance() {
      return instance;
   }
}
