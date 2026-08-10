package ru.fluxvisuals.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.Theme;
import ru.fluxvisuals.config.StyleConfig;

@Environment(EnvType.CLIENT)
public class GuiManager {
   public static MinecraftClient mc = MinecraftClient.getInstance();
   private File file;
   private Theme currentTheme = Theme.THEME1;
   private Category currentCategory = Category.Visuals;

   public void init() {
      this.file = new File(FluxVisualsClient.get.root + "\\configs", "gui.cfg");

      try {
         if (!this.file.getParentFile().exists()) {
            this.file.getParentFile().mkdirs();
         }

         if (!this.file.exists()) {
            this.file.createNewFile();
            this.saveSettings();
         } else {
            this.readSettings();
         }
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   public void setGuiTheme(Theme theme) {
      this.currentTheme = theme;
      this.saveSettings();
   }

   public void setGuiCategory(Category category) {
      this.currentCategory = category;
      this.saveSettings();
   }

   public Theme getCurrentTheme() {
      return this.currentTheme;
   }

   public Category getCurrentCategory() {
      return this.currentCategory;
   }

   public void saveSettings() {
      try (FileWriter writer = new FileWriter(this.file)) {
         Properties props = new Properties();
         props.setProperty("theme", this.currentTheme.name());
         props.setProperty("category", this.currentCategory.name());
         props.setProperty("style.watermarkLogo", String.valueOf(StyleConfig.watermarkLogo));
         props.setProperty("style.watermarkGlow", String.valueOf(StyleConfig.watermarkGlow));
         props.setProperty("style.watermarkName", String.valueOf(StyleConfig.watermarkName));
         props.setProperty("style.clickGuiLogo", String.valueOf(StyleConfig.clickGuiLogo));
         props.store(writer, "GUI Settings");
      } catch (IOException var6) {
         var6.printStackTrace();
      }
   }

   private void readSettings() {
      try (FileReader reader = new FileReader(this.file)) {
         Properties props = new Properties();
         props.load(reader);
         this.currentTheme = Theme.valueOf(props.getProperty("theme", Theme.THEME1.name()));
         this.currentCategory = Category.valueOf(props.getProperty("category", Category.Visuals.name()));
         StyleConfig.watermarkLogo = Boolean.parseBoolean(props.getProperty("style.watermarkLogo", "true"));
         StyleConfig.watermarkGlow = Boolean.parseBoolean(props.getProperty("style.watermarkGlow", "true"));
         StyleConfig.watermarkName = Boolean.parseBoolean(props.getProperty("style.watermarkName", "true"));
         StyleConfig.clickGuiLogo = Boolean.parseBoolean(props.getProperty("style.clickGuiLogo", "true"));
      } catch (IllegalArgumentException | IOException var6) {
         var6.printStackTrace();
      }
   }
}
