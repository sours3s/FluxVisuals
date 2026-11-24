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

   /** autoScripts может содержать имя с .lua, без него или с путём. */
   private static String scriptId(String value) {
      String id = value == null ? "" : value.strip().replace('\\', '/');
      int slash = id.lastIndexOf('/');
      if (slash >= 0) {
         id = id.substring(slash + 1);
      }
      return id.endsWith(".lua") ? id.substring(0, id.length() - 4) : id;
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
         boolean changed = registerScript(avatar, HIDE_SCRIPT_ID);
         Files.writeString(folder.resolve(HIDE_SCRIPT), hideBlock(), StandardCharsets.UTF_8);
         if (changed) {
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
    * Figura запускает родные Lua аватара (скрытие ванильного тела, events.item_render
    * для мечей), только если autoScripts отсутствует. Поэтому не создаём его сами.
    *
    * @return true, если avatar.json был изменён
    */
   private static boolean registerScript(JsonObject avatar, String script) {
      if (!avatar.has(AUTO_SCRIPTS) || !avatar.get(AUTO_SCRIPTS).isJsonArray()) {
         return false;
      }
      JsonArray scripts = avatar.getAsJsonArray(AUTO_SCRIPTS);
      for (JsonElement element : scripts) {
         if (element.isJsonPrimitive() && script.equals(scriptId(element.getAsString()))) {
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
            + "local fluxvisualsHeads = nil\n"
            + "local fluxvisualsHidden = nil\n"
            + "local function fluxvisualsIsHead(part)\n"
            + "    local name = part:getName():lower()\n"
            + "    local parent = part:getParentType() or ''\n"
            + "    return parent == 'Head'\n"
            + "        or name == 'head' or name:match('head$') ~= nil\n"
            + "        or name == 'skull' or name:match('skull$') ~= nil\n"
            + "end\n"
            + "local function fluxvisualsIsItem(part)\n"
            + "    local name = part:getName():lower()\n"
            + "    local parent = part:getParentType() or ''\n"
            + "    if parent == 'Item' or parent == 'Trident' or parent == 'Arrow'\n"
            + "        or parent == 'LeftItemPivot' or parent == 'RightItemPivot' then\n"
            + "        return true\n"
            + "    end\n"
            + "    return name:match('item') ~= nil or name:match('sword') ~= nil\n"
            + "        or name:match('scythe') ~= nil or name:match('trident') ~= nil\n"
            + "        or name:match('blade') ~= nil or name:match('spear') ~= nil\n"
            + "        or name:match('axe') ~= nil or name:match('hammer') ~= nil\n"
            + "        or name:match('bow') ~= nil or name:match('staff') ~= nil\n"
            + "end\n"
            + "local function fluxvisualsCollect(part, blocked, out)\n"
            + "    if not part then return end\n"
            + "    local isItem = fluxvisualsIsItem(part)\n"
            + "    if not blocked and not isItem and fluxvisualsIsHead(part) then\n"
            + "        out[#out + 1] = {part = part, visible = part:getVisible()}\n"
            + "    end\n"
            + "    for _, child in pairs(part:getChildren() or {}) do\n"
            + "        fluxvisualsCollect(child, blocked or isItem, out)\n"
            + "    end\n"
            + "end\n"
            + "local function fluxvisualsFindHeads()\n"
            + "    local out = {}\n"
            + "    for _, model in pairs(models:getChildren() or {}) do\n"
            + "        fluxvisualsCollect(model, false, out)\n"
            + "    end\n"
            + "    return out\n"
            + "end\n"
            + "local function fluxvisualsApplyHide()\n"
            + "    if not host:isHost() then return end\n"
            + "    local hide = renderer:isFirstPerson()\n"
            + "    if hide == fluxvisualsHidden then return end\n"
            + "    fluxvisualsHidden = hide\n"
            + "    if not fluxvisualsHeads then fluxvisualsHeads = fluxvisualsFindHeads() end\n"
            + "    for _, entry in ipairs(fluxvisualsHeads) do\n"
            + "        if hide then\n"
            + "            entry.visible = entry.part:getVisible()\n"
            + "            entry.part:setVisible(false)\n"
            + "        else\n"
            + "            entry.part:setVisible(entry.visible)\n"
            + "        end\n"
            + "    end\n"
            + "end\n"
            + "local function fluxvisualsNormalizeCamera()\n"
            + "    if not host:isHost() then return end\n"
            + "    if not renderer:isFirstPerson() then return end\n"
            + "    -- Аватары задают eye offset и camera pivot в events.render каждый кадр.\n"
            + "    -- Наш скрипт загружается последним, поэтому этот обработчик выполняется\n"
            + "    -- после родного и выравнивает камеру только от первого лица. CameraPos\n"
            + "    -- и CameraRot не трогаем — их сброс давал «только небо» в 1.0.40.\n"
            + "    renderer:setEyeOffset(0, 0, 0)\n"
            + "    renderer:setOffsetCameraPivot(0, 0, 0)\n"
            + "end\n"
            + "events.TICK:register(fluxvisualsApplyHide)\n"
            + "events.RENDER:register(fluxvisualsApplyHide)\n"
            + "events.RENDER:register(fluxvisualsNormalizeCamera)\n"
            + "events.POST_RENDER:register(fluxvisualsApplyHide)\n"
            + BLOCK_END + "\n";
   }

   /**
    * После смены версии ассетов перезаписываем созданные нами Lua-файлы и убираем
    * autoScripts от старых сборок: пустой/урезанный список глушит родные скрипты
    * аватара, из-за чего events.item_render с моделями оружия не выполняется.
    */
   static void refreshGenerated(Path root) {
      if (root == null) {
         return;
      }
      List<Path> folders = new ArrayList<>();
      try (Stream<Path> stream = Files.walk(root, 4)) {
         stream.filter(Files::isDirectory)
               .filter(dir -> Files.isRegularFile(dir.resolve("avatar.json")))
               .forEach(folders::add);
      } catch (Exception e) {
         LOGGER.warn("Could not refresh generated Figura scripts in {}", root, e);
         return;
      }
      for (Path folder : folders) {
         try {
            Path hide = folder.resolve(HIDE_SCRIPT);
            if (Files.isRegularFile(hide)) {
               Files.writeString(hide, hideBlock(), StandardCharsets.UTF_8);
            }
            Files.deleteIfExists(folder.resolve("fluxvisuals_fit.lua"));
            cleanupLegacyInjection(folder);
            Path avatarFile = folder.resolve("avatar.json");
            if (Files.isRegularFile(avatarFile)) {
               repairAvatar(folder, avatarFile);
            }
         } catch (Exception e) {
            LOGGER.debug("Could not refresh generated script in {}", folder, e);
         }
      }
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
            if (element.isJsonPrimitive() && OWN_SCRIPTS.contains(scriptId(element.getAsString()))) {
               removed = true;
            } else {
               kept.add(element);
            }
         }
         if (removed) {
            // Пустой autoScripts запрещает запуск ВСЕХ скриптов — удаляем ключ целиком.
            if (kept.size() == 0 || matchesEveryScript(folder, kept)) {
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
         if (!element.isJsonPrimitive() || !present.contains(scriptId(element.getAsString()))) {
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
