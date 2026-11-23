package ru.fluxvisuals.utils.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class CosmeticFirstPerson {
   private static final Logger LOGGER = LoggerFactory.getLogger(CosmeticFirstPerson.class);
   private static final String AUTO_SCRIPTS = "autoScripts";
   private static final List<String> OWN_SCRIPTS = List.of("fluxvisuals_fit", "fluxvisuals_first_person");
   private static final String BLOCK_START = "-- >>> fluxvisuals first person";
   private static final String BLOCK_END = "-- <<< fluxvisuals first person";
   private static final String MAIN_SCRIPT = "script.lua";
   /** Отдельный файл: не трогаем чужой script.lua и его физику/анимации. */
   private static final String HIDE_SCRIPT = "fluxvisuals_first_person.lua";
   private static final String HIDE_SCRIPT_ID = "fluxvisuals_first_person";

   private CosmeticFirstPerson() {
   }

   static void installHide(Path folder) {
      if (folder == null) {
         return;
      }
      Path avatarFile = folder.resolve("avatar.json");
      if (!Files.isRegularFile(avatarFile)) {
         return;
      }
      try {
         JsonObject avatar = readAvatar(avatarFile);
         if (avatar == null) {
            return;
         }
         // Старые версии FluxVisuals дописывали блок в чужой script.lua.
         // Убираем его, но сам скрипт и его запись в autoScripts оставляем как было.
         cleanupLegacyInjection(folder);
         boolean descriptorChanged = registerScript(avatar, HIDE_SCRIPT_ID);
         Files.writeString(folder.resolve(HIDE_SCRIPT), hideBlock(), StandardCharsets.UTF_8);
         if (descriptorChanged) {
            Files.writeString(avatarFile, avatar.toString(), StandardCharsets.UTF_8);
         }
      } catch (Exception e) {
         LOGGER.warn("Could not add first-person hide to {}", folder, e);
      }
   }

   private static void cleanupLegacyInjection(Path folder) throws Exception {
      Path legacy = folder.resolve(MAIN_SCRIPT);
      if (!Files.isRegularFile(legacy)) {
         return;
      }
      String original = Files.readString(legacy, StandardCharsets.UTF_8);
      String cleaned = stripBlock(original);
      if (!cleaned.equals(original)) {
         Files.writeString(legacy, cleaned, StandardCharsets.UTF_8);
      }
   }

   /**
   * Figura выполняет все найденные Lua-файлы, только если autoScripts отсутствует.
   * Как только список появляется, запускаются только перечисленные скрипты.
   * Поэтому если аватар не задаёт autoScripts, не создаём его: иначе родные
   * Lua-скрипты аватара (в том числе кастомные мечи и first-person модели)
   * перестанут запускаться.
   *
   * @return true, если avatar.json был изменён
   */
   private static boolean registerScript(JsonObject avatar, String script) {
      if (!avatar.has(AUTO_SCRIPTS) || !avatar.get(AUTO_SCRIPTS).isJsonArray()) {
         return false;
      }
      JsonArray scripts = avatar.getAsJsonArray(AUTO_SCRIPTS);
      for (JsonElement element : scripts) {
         if (element.isJsonPrimitive() && script.equals(element.getAsString())) {
            return false;
         }
      }
      scripts.add(script);
      return true;
   }

   private static String stripBlock(String script) {
      int start = script.indexOf(BLOCK_START);
      if (start < 0) {
         return !script.isEmpty() && !script.endsWith("\n") ? script + "\n" : script;
      }
      int end = script.indexOf(BLOCK_END, start);
      String tail = end < 0 ? "" : script.substring(end + BLOCK_END.length());
      String head = script.substring(0, start);
      String joined = head + tail;
      return !joined.isEmpty() && !joined.endsWith("\n") ? joined + "\n" : joined;
   }

   private static String hideBlock() {
      return BLOCK_START + "\n"
            + "-- Written by FluxVisuals. Hides only the avatar head from its wearer's\n"
            + "-- own camera; hands, held items and custom swords stay visible.\n"
            + "local fluxvisualsHidden = nil\n"
            + "local function fluxvisualsApplyHide()\n"
            + "    if not host:isHost() then\n"
            + "        return\n"
            + "    end\n"
            + "    local hide = renderer:isFirstPerson()\n"
            + "    if hide == fluxvisualsHidden then\n"
            + "        return\n"
            + "    end\n"
            + "    fluxvisualsHidden = hide\n"
            + "    local head = models and models.root and models.root.Head\n"
            + "    if head then\n"
            + "        head:setVisible(not hide)\n"
            + "    end\n"
            + "end\n"
            + "events.TICK:register(fluxvisualsApplyHide)\n"
            + "events.RENDER:register(fluxvisualsApplyHide)\n"
            + BLOCK_END + "\n";
   }

   static void repair(Path folder) {
      if (folder != null) {
         try {
            Path avatarFile = folder.resolve("avatar.json");
            if (!Files.isRegularFile(avatarFile)) {
               return;
            }
            cleanupLegacyInjection(folder);
            for (String name : OWN_SCRIPTS) {
               Files.deleteIfExists(folder.resolve(name + ".lua"));
            }
            repairAvatar(folder, avatarFile);
         } catch (Exception var4) {
            LOGGER.warn("Could not clean FluxVisuals edits out of {}", folder, var4);
         }
      }
   }

   private static void repairAvatar(Path folder, Path avatarFile) throws Exception {
      JsonObject avatar = readAvatar(avatarFile);
      if (avatar != null && avatar.has(AUTO_SCRIPTS) && avatar.get(AUTO_SCRIPTS).isJsonArray()) {
         JsonArray scripts = avatar.getAsJsonArray(AUTO_SCRIPTS);
         JsonArray kept = new JsonArray();
         boolean removed = false;
         for (JsonElement element : scripts) {
            if (element.isJsonPrimitive() && OWN_SCRIPTS.contains(element.getAsString())) {
               removed = true;
            } else {
               kept.add(element);
            }
         }
         if (removed) {
            if (matchesEveryScript(folder, kept)) {
               avatar.remove(AUTO_SCRIPTS);
            } else {
               avatar.add(AUTO_SCRIPTS, kept);
            }
            Files.writeString(avatarFile, avatar.toString(), StandardCharsets.UTF_8);
         }
      }
   }

   private static boolean matchesEveryScript(Path folder, JsonArray scripts) throws Exception {
      List<String> present = new ArrayList<>();
      try (Stream<Path> files = Files.walk(folder)) {
         files.filter(Files::isRegularFile)
               .filter(path -> path.getFileName().toString().endsWith(".lua"))
               .forEach(path -> {
                  String relative = folder.relativize(path).toString().replace('\\', '/');
                  present.add(relative.substring(0, relative.length() - ".lua".length()));
               });
      }
      if (present.size() != scripts.size()) {
         return false;
      }
      for (JsonElement element : scripts) {
         if (!element.isJsonPrimitive() || !present.contains(element.getAsString())) {
            return false;
         }
      }
      return true;
   }

   private static JsonObject readAvatar(Path avatarFile) {
      try {
         JsonElement parsed = JsonParser.parseString(Files.readString(avatarFile, StandardCharsets.UTF_8));
         return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
      } catch (Exception var2) {
         LOGGER.warn("Avatar descriptor {} is not readable JSON; left alone", avatarFile);
         return null;
      }
   }
}
