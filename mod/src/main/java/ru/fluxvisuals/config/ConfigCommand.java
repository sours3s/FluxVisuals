package ru.fluxvisuals.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.MutableText;
import net.minecraft.text.TextColor;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.cfg.Config;
import ru.fluxvisuals.cfg.ConfigManager;
import ru.fluxvisuals.commands.Command;
import ru.fluxvisuals.commands.CommandContext;
import ru.fluxvisuals.commands.CommandException;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.Colors;
import ru.fluxvisuals.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public final class ConfigCommand implements Command {
   private static final ConfigCommand INSTANCE = new ConfigCommand();
   private static final List<String> COMMAND_ALIASES = List.of(".cfg", ".c", ".config", ".cgf", ".fig");
   private static final Map<String, ConfigCommand.CommandMetadata> COMMAND_DEFINITIONS;
   private static final List<String> SUB_COMMANDS;
   private static final String SUPPORTED_COMMANDS = "save/load/list/delete/export/import/profile";

   private ConfigCommand() {
   }

   public static ConfigCommand getInstance() {
      return INSTANCE;
   }

   public List<String> getCommandAliases() {
      return COMMAND_ALIASES;
   }

   public List<String> getSubCommands() {
      return SUB_COMMANDS;
   }

   public Map<String, ConfigCommand.CommandMetadata> getCommandMetadata() {
      return COMMAND_DEFINITIONS;
   }

   @Override
   public String name() {
      return "config";
   }

   @Override
   public List<String> aliases() {
      return COMMAND_ALIASES;
   }

   @Override
   public String usage() {
      return COMMAND_ALIASES.get(0) + " <save/load/list/delete>";
   }

   @Override
   public String description() {
      return "Manage client configuration profiles";
   }

   @Override
   public void execute(CommandContext context, String arguments) throws CommandException {
      if (FluxVisualsClient.get.configManager == null) {
         throw new CommandException("Configuration system is not ready yet");
      } else if (arguments != null && !arguments.isBlank()) {
         String[] parts = arguments.split("\\s+", 2);
         String subCommand = parts[0].toLowerCase(Locale.ROOT);
         String remainder = parts.length > 1 ? parts[1].trim() : "";
         switch (subCommand) {
            case "save":
               this.handleSave(context, remainder);
               break;
            case "load":
               this.handleLoad(context, remainder);
               break;
            case "list":
               this.handleList(context);
               break;
            case "delete":
               this.handleDelete(context, remainder);
               break;
            case "export":
               this.handleExport(context, remainder);
               break;
            case "import":
               this.handleImport(context, remainder);
               break;
            case "profile":
               this.handleProfile(context, remainder);
               break;
            default:
               throw new CommandException("Unknown command. Use save/load/list/delete/export/import/profile");
         }
      } else {
         context.sendInfo(
            "Usage: "
               + COMMAND_DEFINITIONS.values().stream().map(metadata -> COMMAND_ALIASES.get(0) + " " + metadata.usage()).collect(Collectors.joining(", "))
         );
      }
   }

   private void handleSave(CommandContext context, String name) throws CommandException {
      if (name != null && !name.isBlank()) {
         ConfigManager configManager = FluxVisualsClient.get.configManager;
         if (configManager.saveConfig(name)) {
            FluxVisualsClient.get.manager.get(Hud.class).showNotification("cfg", "Сохранен конфиг " + name, 6000L, Renderer2D.ColorUtil.getTextTwoColor(1, 1));
            context.sendSuccess("Config '" + name + "' saved");
         } else {
            throw new CommandException("Failed to save config '" + name + "'");
         }
      } else {
         throw new CommandException("Specify config name");
      }
   }

   private void handleLoad(CommandContext context, String name) throws CommandException {
      if (name != null && !name.isBlank()) {
         ConfigManager configManager = FluxVisualsClient.get.configManager;
         if (configManager.loadConfig(name)) {
            FluxVisualsClient.get.manager.get(Hud.class).showNotification("cfg", "Загружен конфиг " + name, 6000L, Renderer2D.ColorUtil.getTextTwoColor(1, 1));
            context.sendSuccess("Config '" + name + "' loaded");
         } else {
            throw new CommandException("Config '" + name + "' not found or failed to load");
         }
      } else {
         throw new CommandException("Specify config name to load");
      }
   }

   private void handleList(CommandContext context) {
      ConfigManager configManager = FluxVisualsClient.get.configManager;
      List<Config> configs = configManager.getContents();
      if (configs.isEmpty()) {
         context.sendInfo("No available configs");
      } else {
         MutableText builder = Text.literal("Available configs: ");

         for (int i = 0; i < configs.size(); i++) {
            String name = configs.get(i).getName();
            MutableText nameText = Text.literal(name);
            nameText = nameText.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(Colors.getClientPrimary())));
            builder = builder.append(nameText);
            if (i < configs.size() - 1) {
               builder = builder.append(Text.literal(" | "));
            }
         }

         context.sendInfo(builder);
      }
   }

   private void handleDelete(CommandContext context, String name) throws CommandException {
      if (name != null && !name.isBlank()) {
         ConfigManager configManager = FluxVisualsClient.get.configManager;
         if (configManager.deleteConfig(name)) {
            FluxVisualsClient.get.manager.get(Hud.class).showNotification("cfg", "Удален конфиг " + name, 6000L, Renderer2D.ColorUtil.getTextTwoColor(1, 1));
            context.sendSuccess("Config '" + name + "' deleted");
         } else {
            throw new CommandException("Config '" + name + "' not found or failed to delete");
         }
      } else {
         throw new CommandException("Specify config name to delete");
      }
   }

   private void handleExport(CommandContext context, String name) throws CommandException {
      if (name != null && !name.isBlank()) {
         ConfigManager configManager = FluxVisualsClient.get.configManager;
         Config config = configManager.findConfig(name);
         if (config != null) {
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(config.save());
            try {
               java.io.File file = new java.io.File(FluxVisualsClient.get.root, "configs/export_" + name + ".json");
               java.nio.file.Files.writeString(file.toPath(), json);
               context.sendSuccess("Config '" + name + "' exported to export_" + name + ".json");
            } catch (java.io.IOException e) {
               throw new CommandException("Failed to export config: " + e.getMessage());
            }
         } else {
            throw new CommandException("Config '" + name + "' not found");
         }
      } else {
         throw new CommandException("Specify config name to export");
      }
   }

   private void handleImport(CommandContext context, String name) throws CommandException {
      if (name != null && !name.isBlank()) {
         java.io.File file = new java.io.File(FluxVisualsClient.get.root, "configs/" + name + ".json");
         if (!file.exists()) {
            file = new java.io.File(FluxVisualsClient.get.root, "configs/export_" + name + ".json");
         }
         if (!file.exists()) {
            throw new CommandException("Config file '" + name + ".json' not found in configs folder");
         }
         try {
            String json = java.nio.file.Files.readString(file.toPath());
            com.google.gson.JsonObject object = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            ConfigManager configManager = FluxVisualsClient.get.configManager;
            String importName = name.replace("export_", "");
            Config config = configManager.findConfig(importName);
            if (config == null) {
               config = new Config(importName);
               configManager.getContents().add(config);
            }
            config.load(object);
            context.sendSuccess("Config '" + importName + "' imported successfully");
         } catch (java.io.IOException | com.google.gson.JsonSyntaxException e) {
            throw new CommandException("Failed to import config: " + e.getMessage());
         }
      } else {
         throw new CommandException("Specify config name to import (filename without .json)");
      }
   }

   private void handleProfile(CommandContext context, String args) throws CommandException {
      if (args != null && !args.isBlank()) {
         String[] parts = args.split("\\s+", 2);
         String subCmd = parts[0].toLowerCase(Locale.ROOT);
         String name = parts.length > 1 ? parts[1].trim() : "";
         ConfigManager configManager = FluxVisualsClient.get.configManager;

         switch (subCmd) {
            case "save":
               if (name.isBlank()) throw new CommandException("Specify profile name");
               if (configManager.saveConfig(name)) {
                  context.sendSuccess("Profile '" + name + "' saved");
               } else {
                  throw new CommandException("Failed to save profile");
               }
               break;
            case "load":
               if (name.isBlank()) throw new CommandException("Specify profile name");
               if (configManager.loadConfig(name)) {
                  context.sendSuccess("Profile '" + name + "' loaded");
               } else {
                  throw new CommandException("Profile '" + name + "' not found");
               }
               break;
            case "delete":
               if (name.isBlank()) throw new CommandException("Specify profile name");
               if (configManager.deleteConfig(name)) {
                  context.sendSuccess("Profile '" + name + "' deleted");
               } else {
                  throw new CommandException("Profile '" + name + "' not found");
               }
               break;
            case "list":
               List<Config> configs = configManager.getContents();
               if (configs.isEmpty()) {
                  context.sendInfo("No profiles");
               } else {
                  MutableText builder = Text.literal("Profiles: ");
                  for (int i = 0; i < configs.size(); i++) {
                     builder = builder.append(Text.literal(configs.get(i).getName()));
                     if (i < configs.size() - 1) builder = builder.append(Text.literal(", "));
                  }
                  context.sendInfo(builder);
               }
               break;
            default:
               throw new CommandException("Usage: .cfg profile <save/load/delete/list> [name]");
         }
      } else {
         context.sendInfo("Usage: .cfg profile <save/load/delete/list> [name]");
      }
   }

   static {
      Map<String, ConfigCommand.CommandMetadata> commands = new LinkedHashMap<>();
      commands.put("save", new ConfigCommand.CommandMetadata("save <name>", ConfigCommand.ArgumentType.NEW_CONFIG_NAME, "Save current config"));
      commands.put(
         "load", new ConfigCommand.CommandMetadata("load <name>", ConfigCommand.ArgumentType.EXISTING_CONFIG_NAME, "Load a config and apply its settings")
      );
      commands.put("list", new ConfigCommand.CommandMetadata("list", ConfigCommand.ArgumentType.NONE, "Show all available configs"));
      commands.put("delete", new ConfigCommand.CommandMetadata("delete <name>", ConfigCommand.ArgumentType.EXISTING_CONFIG_NAME, "Delete a saved config"));
      commands.put("export", new ConfigCommand.CommandMetadata("export <name>", ConfigCommand.ArgumentType.EXISTING_CONFIG_NAME, "Export config to file"));
      commands.put("import", new ConfigCommand.CommandMetadata("import <name>", ConfigCommand.ArgumentType.EXISTING_CONFIG_NAME, "Import config from file"));
      commands.put("profile", new ConfigCommand.CommandMetadata("profile <save/load/delete/list> [name]", ConfigCommand.ArgumentType.EXISTING_CONFIG_NAME, "Manage config profiles"));
      COMMAND_DEFINITIONS = Collections.unmodifiableMap(commands);
      SUB_COMMANDS = List.copyOf(COMMAND_DEFINITIONS.keySet());
   }

   @Environment(EnvType.CLIENT)
   public static enum ArgumentType {
      NONE,
      NEW_CONFIG_NAME,
      EXISTING_CONFIG_NAME;
   }

   @Environment(EnvType.CLIENT)
   public record CommandMetadata(String usage, ConfigCommand.ArgumentType argumentType, String description) {
   }
}
