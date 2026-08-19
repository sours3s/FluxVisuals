package ru.fluxvisuals.crosshair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.cfg.ConfigManager;

/**
 * Настройки кастомного прицела (не модуль — отдельная система «Мастерской прицелов»).
 * Сохраняются в crosshair.json в папке конфигов. Прицел рендерится в игре всегда
 * на основе этих настроек (InGameHudCrosshairMixin), тумблер-модуля нет.
 */
@Environment(EnvType.CLIENT)
public final class CrosshairSettings {
   private static CrosshairSettings INSTANCE;

   // Стили
   public static final String[] STYLES = {"Cross", "Dot", "GapDot", "T", "Corner", "Circle"};

   // Поля
   public String style = "Cross";
   public boolean enabled = false;
   public float size = 6.0F;
   public float thickness = 1.5F;
   public float gap = 3.0F;
   public int colorR = 255;
   public int colorG = 255;
   public int colorB = 255;

   private CrosshairSettings() {}

   public static CrosshairSettings getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new CrosshairSettings();
         INSTANCE.load();
      }
      return INSTANCE;
   }

   /** Копия с дефолтами для превью карточки стиля. */
   public static CrosshairSettings createPreview(String style) {
      CrosshairSettings s = new CrosshairSettings();
      s.style = style;
      return s;
   }

   public int getColor() {
      int color = 0xFF000000 | ((colorR & 0xFF) << 16) | ((colorG & 0xFF) << 8) | (colorB & 0xFF);
      return (color & 0x00FFFFFF) | 0xC8000000; // alpha 200
   }

   private File getFile() {
      return new File(ConfigManager.getConfigDirectoryPath(), "crosshair.json");
   }

   public void save() {
      try {
         File dir = ConfigManager.getConfigDirectoryPath();
         if (dir != null && !dir.exists()) dir.mkdirs();
         JsonObject obj = new JsonObject();
         obj.addProperty("style", style);
         obj.addProperty("enabled", enabled);
         obj.addProperty("size", size);
         obj.addProperty("thickness", thickness);
         obj.addProperty("gap", gap);
         obj.addProperty("colorR", colorR);
         obj.addProperty("colorG", colorG);
         obj.addProperty("colorB", colorB);
         String json = new GsonBuilder().setPrettyPrinting().create().toJson(obj);
         try (FileWriter w = new FileWriter(getFile())) {
            w.write(json);
         }
      } catch (IOException ignored) {}
   }

   public void load() {
      try {
         File f = getFile();
         if (f == null || !f.exists()) return;
         try (FileReader r = new FileReader(f)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            if (obj.has("style")) style = obj.get("style").getAsString();
            if (obj.has("enabled")) enabled = obj.get("enabled").getAsBoolean();
            if (obj.has("size")) size = obj.get("size").getAsFloat();
            if (obj.has("thickness")) thickness = obj.get("thickness").getAsFloat();
            if (obj.has("gap")) gap = obj.get("gap").getAsFloat();
            if (obj.has("colorR")) colorR = obj.get("colorR").getAsInt();
            if (obj.has("colorG")) colorG = obj.get("colorG").getAsInt();
            if (obj.has("colorB")) colorB = obj.get("colorB").getAsInt();
            // Clamp
            size = Math.max(2, Math.min(15, size));
            thickness = Math.max(0.5F, Math.min(4, thickness));
            gap = Math.max(0, Math.min(10, gap));
            colorR = Math.max(0, Math.min(255, colorR));
            colorG = Math.max(0, Math.min(255, colorG));
            colorB = Math.max(0, Math.min(255, colorB));
         }
      } catch (Exception ignored) {}
   }

   /** Проверяет валидность стиля (защита от старого/битого конфига). */
   public boolean isStyleValid() {
      for (String s : STYLES) if (s.equalsIgnoreCase(style)) return true;
      return false;
   }
}
