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
   private static final List<String> OWN_SCRIPTS = List.of(
         "fluxvisuals_fit", "fluxvisuals_first_person", "z_fluxvisuals_first_person");
   private static final String BLOCK_START = "-- >>> fluxvisuals first person";
   private static final String BLOCK_END = "-- <<< fluxvisuals first person";
   private static final String MAIN_SCRIPT = "script.lua";
   /** Name of the generated hide/normalize script. */
   private static final String HIDE_SCRIPT = "z_fluxvisuals_first_person.lua";
   private static final String HIDE_SCRIPT_ID = "z_fluxvisuals_first_person";
   private static final String LEGACY_HIDE_SCRIPT = "fluxvisuals_first_person.lua";

   private CosmeticFirstPerson() {
   }

   /** Extract a script id from an autoScripts entry. */
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
         // Remove our legacy injection from script.lua and register the standalone script.
         cleanupLegacyInjection(folder);
         boolean changed = registerScript(avatar, HIDE_SCRIPT_ID);
         Files.deleteIfExists(folder.resolve(LEGACY_HIDE_SCRIPT));
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
    * Figura loads every .lua file in name order. Our script must be last, otherwise an
    * avatar's own script can re-apply camera offsets after our reset.
    *
    * @return true if avatar.json was changed
    */
   private static boolean registerScript(JsonObject avatar, String script) {
      if (!avatar.has(AUTO_SCRIPTS) || !avatar.get(AUTO_SCRIPTS).isJsonArray()) {
         return false;
      }
      JsonArray scripts = avatar.getAsJsonArray(AUTO_SCRIPTS);
      for (int i = scripts.size() - 1; i >= 0; i--) {
         JsonElement element = scripts.get(i);
         String id = element.isJsonPrimitive() ? scriptId(element.getAsString()) : "";
         if (OWN_SCRIPTS.contains(id)) {
            scripts.remove(i);
         }
      }
      // Always append ours so it runs after the avatar's own scripts.
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
            + "-- Written by FluxVisuals. Hides the avatar body from its wearer's own\n"
            + "-- camera so the camera is never inside the model; held items stay visible.\n"
            + "local fluxvisualsBody = nil\n"
            + "local fluxvisualsHidden = nil\n"
            + "local fluxvisualsVanillaHidden = false\n"
            + "local fluxvisualsArmsHidden = false\n"
            + "local fluxvisualsArmParts = nil\n"
            + "-- Set by the avatar's own events.item_render when it returns its weapon model.\n"
            + "FLUXVISUALS_CUSTOM_WEAPON_ID = nil\n"
            + "local function fluxvisualsIsItem(part)\n"
            + "    local name = part:getName():lower()\n"
            + "    local parent = part:getParentType() or ''\n"
            + "    if parent == 'Item' or parent == 'Trident' or parent == 'Arrow'\n"
            + "        or parent == 'LeftItemPivot' or parent == 'RightItemPivot'\n"
            + "        or parent == 'LeftArm' or parent == 'RightArm' then\n"
            + "        return true\n"
            + "    end\n"
            + "    -- Keep hands, arms, sleeves and weapon handles visible in first person.\n"
            + "    if name:match('hand') ~= nil or name == 'arm'\n"
            + "        or name == 'leftarm' or name == 'rightarm'\n"
            + "        or name == 'leftsleeve' or name == 'rightsleeve'\n"
            + "        or name:match('glove') ~= nil or name:match('finger') ~= nil\n"
            + "        or name:match('knuckle') ~= nil or name:match('palm') ~= nil\n"
            + "        or name:match('l_h') ~= nil or name:match('r_h') ~= nil then\n"
            + "        return true\n"
            + "    end\n"
            + "    return name:match('item') ~= nil or name:match('sword') ~= nil\n"
            + "        or name:match('scythe') ~= nil or name:match('weapon') ~= nil\n"
            + "        or name:match('trident') ~= nil or name:match('blade') ~= nil\n"
            + "        or name:match('spear') ~= nil or name:match('axe') ~= nil\n"
            + "        or name:match('hammer') ~= nil or name:match('bow') ~= nil\n"
            + "        or name:match('staff') ~= nil\n"
            + "end\n"
            + "local function fluxvisualsCollect(part, blocked, out)\n"
            + "    if not part then return end\n"
            + "    local isItem = fluxvisualsIsItem(part)\n"
            + "    if not blocked and not isItem then\n"
            + "        out[#out + 1] = {part = part, visible = part:getVisible()}\n"
            + "    end\n"
            + "    for _, child in pairs(part:getChildren() or {}) do\n"
            + "        fluxvisualsCollect(child, blocked or isItem, out)\n"
            + "    end\n"
            + "end\n"
            + "local function fluxvisualsFindBody()\n"
            + "    local out = {}\n"
            + "    for _, model in pairs(models:getChildren() or {}) do\n"
            + "        fluxvisualsCollect(model, false, out)\n"
            + "    end\n"
            + "    return out\n"
            + "end\n"
            + "local function fluxvisualsIsArmPart(part)\n"
            + "    local name = part:getName():lower()\n"
            + "    local parent = part:getParentType() or ''\n"
            + "    -- The weapon model lives under Item and must stay visible.\n"
            + "    if parent == 'Item' or parent == 'Trident' or parent == 'Arrow'\n"
            + "        or parent == 'LeftItemPivot' or parent == 'RightItemPivot' then return false end\n"
            + "    if name:match('sword') ~= nil or name:match('scythe') ~= nil\n"
            + "        or name:match('weapon') ~= nil or name:match('blade') ~= nil\n"
            + "        or name:match('spear') ~= nil or name:match('axe') ~= nil\n"
            + "        or name:match('hammer') ~= nil or name:match('bow') ~= nil\n"
            + "        or name:match('staff') ~= nil or name:match('item') ~= nil then return false end\n"
            + "    if name:match('hand') ~= nil or name == 'arm'\n"
            + "        or name:match('leftarm') ~= nil or name:match('rightarm') ~= nil\n"
            + "        or name:match('sleeve') ~= nil or name:match('glove') ~= nil\n"
            + "        or name:match('finger') ~= nil or name:match('knuckle') ~= nil\n"
            + "        or name:match('palm') ~= nil or name:match('l_h') ~= nil or name:match('r_h') ~= nil then return true end\n"
            + "    return parent == 'LeftArm' or parent == 'RightArm'\n"
            + "end\n"
            + "local function fluxvisualsCollectArms(part, out)\n"
            + "    if not part then return end\n"
            + "    if fluxvisualsIsArmPart(part) then\n"
            + "        out[#out + 1] = {part = part, visible = part:getVisible()}\n"
            + "    end\n"
            + "    for _, child in pairs(part:getChildren() or {}) do\n"
            + "        fluxvisualsCollectArms(child, out)\n"
            + "    end\n"
            + "end\n"
            + "local function fluxvisualsFindArms()\n"
            + "    local out = {}\n"
            + "    for _, model in pairs(models:getChildren() or {}) do\n"
            + "        fluxvisualsCollectArms(model, out)\n"
            + "    end\n"
            + "    return out\n"
            + "end\n"
            + "local function fluxvisualsApplyHide()\n"
            + "    if not host:isHost() then return end\n"
            + "    local hide = renderer:isFirstPerson()\n"
            + "    if hide then\n"
            + "        -- Avatar scripts re-show parts every render frame, so always re-hide.\n"
            + "        if not fluxvisualsBody then fluxvisualsBody = fluxvisualsFindBody() end\n"
            + "        for _, entry in ipairs(fluxvisualsBody) do\n"
            + "            entry.part:setVisible(false)\n"
            + "        end\n"
            + "        fluxvisualsHidden = true\n"
            + "    elseif fluxvisualsHidden then\n"
            + "        for _, entry in ipairs(fluxvisualsBody or {}) do\n"
            + "            entry.part:setVisible(entry.visible)\n"
            + "        end\n"
            + "        fluxvisualsHidden = false\n"
            + "    end\n"
            + "end\n"
            + "local function fluxvisualsApplyVanillaHide()\n"
            + "    if not host:isHost() or not vanilla_model then return end\n"
            + "    if renderer:isFirstPerson() then\n"
            + "        -- Match vanilla: no torso, legs or head in first person. Arms and\n"
            + "        -- held items are separate parts and stay visible.\n"
            + "        -- Never hide vanilla_model.PLAYER — it contains both arms.\n"
            + "        vanilla_model.HEAD:setVisible(false)\n"
            + "        vanilla_model.BODY:setVisible(false)\n"
            + "        vanilla_model.HAT:setVisible(false)\n"
            + "        vanilla_model.JACKET:setVisible(false)\n"
            + "        vanilla_model.LEFT_LEG:setVisible(false)\n"
            + "        vanilla_model.RIGHT_LEG:setVisible(false)\n"
            + "        vanilla_model.LEFT_PANTS:setVisible(false)\n"
            + "        vanilla_model.RIGHT_PANTS:setVisible(false)\n"
            + "        vanilla_model.CAPE:setVisible(false)\n"
            + "        vanilla_model.ARMOR:setVisible(false)\n"
            + "        vanilla_model.ELYTRA:setVisible(false)\n"
            + "        fluxvisualsVanillaHidden = true\n"
            + "    elseif fluxvisualsVanillaHidden then\n"
            + "        vanilla_model.HEAD:setVisible(nil)\n"
            + "        vanilla_model.BODY:setVisible(nil)\n"
            + "        vanilla_model.HAT:setVisible(nil)\n"
            + "        vanilla_model.JACKET:setVisible(nil)\n"
            + "        vanilla_model.LEFT_LEG:setVisible(nil)\n"
            + "        vanilla_model.RIGHT_LEG:setVisible(nil)\n"
            + "        vanilla_model.LEFT_PANTS:setVisible(nil)\n"
            + "        vanilla_model.RIGHT_PANTS:setVisible(nil)\n"
            + "        vanilla_model.CAPE:setVisible(nil)\n"
            + "        vanilla_model.ARMOR:setVisible(nil)\n"
            + "        vanilla_model.ELYTRA:setVisible(nil)\n"
            + "        fluxvisualsVanillaHidden = false\n"
            + "    end\n"
            + "end\n"
            + "local function fluxvisualsHasSwordInHands()\n"
            + "    if not host:isHost() or not player:isLoaded() then return false end\n"
            + "    if not renderer:isFirstPerson() then return false end\n"
            + "    local main = player:getHeldItem(false)\n"
            + "    local off = player:getHeldItem(true)\n"
            + "    if main and main.id and tostring(main.id):find('sword') then return true end\n"
            + "    if off and off.id and tostring(off.id):find('sword') then return true end\n"
            + "    local renderedId = FLUXVISUALS_CUSTOM_WEAPON_ID\n"
            + "    if renderedId and main and main.id == renderedId then return true end\n"
            + "    if renderedId and off and off.id == renderedId then return true end\n"
            + "    return false\n"
            + "end\n"
            + "local function fluxvisualsApplyArmHide()\n"
            + "    if not host:isHost() then return end\n"
            + "    local hide = fluxvisualsHasSwordInHands()\n"
            + "    if vanilla_model then\n"
            + "        for _, part in ipairs({vanilla_model.LEFT_ARM, vanilla_model.LEFT_SLEEVE,\n"
            + "                               vanilla_model.RIGHT_ARM, vanilla_model.RIGHT_SLEEVE}) do\n"
            + "            if part then part:setVisible(hide and false or nil) end\n"
            + "        end\n"
            + "    end\n"
            + "    if hide then\n"
            + "        if not fluxvisualsArmParts then fluxvisualsArmParts = fluxvisualsFindArms() end\n"
            + "        for _, entry in ipairs(fluxvisualsArmParts) do\n"
            + "            entry.part:setVisible(false)\n"
            + "        end\n"
            + "    elseif fluxvisualsArmsHidden then\n"
            + "        for _, entry in ipairs(fluxvisualsArmParts or {}) do\n"
            + "            entry.part:setVisible(entry.visible)\n"
            + "        end\n"
            + "    end\n"
            + "    fluxvisualsArmsHidden = hide\n"
            + "end\n"
            + "local function fluxvisualsNormalizeCamera()\n"
            + "    if not host:isHost() then return end\n"
            + "    if not renderer:isFirstPerson() then return end\n"
            + "    -- Clear every Figura camera override for the wearer. Avatar scripts commonly\n"
            + "    -- move the camera to a hat or head pivot for first person; that places the\n"
            + "    -- camera inside the body. nil restores vanilla camera behaviour.\n"
            + "    renderer:setEyeOffset(nil)\n"
            + "    renderer:setCameraPos(nil)\n"
            + "    renderer:setCameraPivot(nil)\n"
            + "    renderer:setOffsetCameraPivot(nil)\n"
            + "    renderer:setCameraRot(nil)\n"
            + "    renderer:setOffsetCameraRot(nil)\n"
            + "end\n"
            + "events.TICK:register(fluxvisualsApplyHide)\n"
            + "events.TICK:register(fluxvisualsApplyVanillaHide)\n"
            + "events.TICK:register(fluxvisualsApplyArmHide)\n"
            + "events.TICK:register(fluxvisualsNormalizeCamera)\n"
            + "events.RENDER:register(fluxvisualsApplyHide)\n"
            + "events.RENDER:register(fluxvisualsApplyVanillaHide)\n"
            + "events.RENDER:register(fluxvisualsApplyArmHide)\n"
            + "events.RENDER:register(fluxvisualsNormalizeCamera)\n"
            + "events.POST_RENDER:register(fluxvisualsApplyHide)\n"
            + "events.POST_RENDER:register(fluxvisualsApplyVanillaHide)\n"
            + "events.POST_RENDER:register(fluxvisualsApplyArmHide)\n"
            + "events.POST_RENDER:register(fluxvisualsNormalizeCamera)\n"
            + BLOCK_END + "\n";
   }

   /** Refresh generated scripts and clean up stale script names. */
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
            Files.deleteIfExists(folder.resolve(LEGACY_HIDE_SCRIPT));
            for (String name : OWN_SCRIPTS) {
               Path script = folder.resolve(name + ".lua");
               if (!name.equals(HIDE_SCRIPT_ID) && Files.isRegularFile(script)) {
                  Files.deleteIfExists(script);
               }
            }
            Files.writeString(folder.resolve(HIDE_SCRIPT), hideBlock(), StandardCharsets.UTF_8);
            cleanupLegacyInjection(folder);
            Path avatarFile = folder.resolve("avatar.json");
            if (Files.isRegularFile(avatarFile)) {
               JsonObject avatar = readAvatar(avatarFile);
               if (avatar != null && registerScript(avatar, HIDE_SCRIPT_ID)) {
                  Files.writeString(avatarFile, avatar.toString(), StandardCharsets.UTF_8);
               }
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
            // Remove autoScripts entirely when it only contained our scripts.
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
