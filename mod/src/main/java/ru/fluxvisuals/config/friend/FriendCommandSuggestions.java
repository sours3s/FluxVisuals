package ru.fluxvisuals.config.friend;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import ru.fluxvisuals.commands.FriendCommand;
import ru.fluxvisuals.commands.suggestions.CommandSuggestionProvider;
import ru.fluxvisuals.commands.suggestions.CommandSuggestions;

@Environment(EnvType.CLIENT)
public final class FriendCommandSuggestions implements CommandSuggestionProvider {
   private static final FriendCommandSuggestions INSTANCE = new FriendCommandSuggestions();
   private static final List<String> OPERATIONS = List.of("add", "remove", "list", "clear");

   private FriendCommandSuggestions() {
   }

   public static FriendCommandSuggestions getInstance() {
      return INSTANCE;
   }

   @Override
   public List<String> aliases() {
      return FriendCommand.getInstance().aliases();
   }

   @Override
   public boolean supportsInput(String input) {
      return this.findAlias(input) != null || this.matchesAliasPrefix(input);
   }

   @Override
   public CommandSuggestions.SuggestionSet collect(String input) {
      AliasMatch alias = this.findAlias(input);
      if (alias == null) {
         return null;
      }

      String arguments = input.substring(alias.aliasEnd());
      if (arguments.isEmpty()) {
         return CommandSuggestions.of(this.buildRootEntries("", true));
      }

      int leadingSpaces = this.countLeadingWhitespace(arguments);
      String trimmed = arguments.substring(leadingSpaces);
      if (trimmed.isEmpty()) {
         return CommandSuggestions.of(this.buildRootEntries("", false));
      }

      String firstToken = this.nextToken(trimmed);
      if (trimmed.length() == firstToken.length()) {
         return CommandSuggestions.of(this.buildRootEntries(firstToken, false));
      }

      String afterFirst = trimmed.substring(firstToken.length());
      int spacesAfterFirst = this.countLeadingWhitespace(afterFirst);
      if (spacesAfterFirst == 0) {
         return CommandSuggestions.of(this.buildRootEntries(firstToken, false));
      }

      String remaining = afterFirst.substring(spacesAfterFirst);
      String secondToken = this.nextToken(remaining);
      if (remaining.length() > secondToken.length()) {
         return null;
      }

      return switch (firstToken.toLowerCase(Locale.ROOT)) {
         case "add" -> CommandSuggestions.of(this.buildOnlinePlayerEntries(secondToken, false));
         case "remove", "delete", "del" -> CommandSuggestions.of(this.buildFriendEntries(secondToken));
         default -> null;
      };
   }

   @Override
   public List<CommandSuggestions.SuggestionEntry> collectAliasSuggestions(String input) {
      if (input == null || !input.startsWith(".")) {
         return List.of();
      }

      List<CommandSuggestions.SuggestionEntry> entries = new ArrayList<>();
      for (String alias : this.aliases()) {
         if (alias.startsWith(input)) {
            entries.add(new CommandSuggestions.SuggestionEntry(alias, "Управление списком друзей", alias.substring(input.length()), false));
         }
      }

      return entries;
   }

   private List<CommandSuggestions.SuggestionEntry> buildRootEntries(String partial, boolean leadingSpace) {
      String normalized = partial.toLowerCase(Locale.ROOT);
      List<CommandSuggestions.SuggestionEntry> entries = new ArrayList<>();
      for (String operation : OPERATIONS) {
         if (operation.startsWith(normalized)) {
            boolean acceptsName = operation.equals("add") || operation.equals("remove");
            String completion = this.completion(operation, partial, leadingSpace, acceptsName);
            String usage = acceptsName ? operation + " <ник>" : operation;
            entries.add(new CommandSuggestions.SuggestionEntry(usage, this.operationDescription(operation), completion, false));
         }
      }

      entries.addAll(this.buildOnlinePlayerEntries(partial, leadingSpace));
      return entries;
   }

   private List<CommandSuggestions.SuggestionEntry> buildOnlinePlayerEntries(String partial, boolean leadingSpace) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world == null) {
         return List.of();
      }

      String ownName = client.player == null ? "" : client.player.getName().getString();
      String normalized = partial.toLowerCase(Locale.ROOT);
      return client.world.getPlayers().stream()
         .map(player -> player.getName().getString())
         .filter(name -> !name.equalsIgnoreCase(ownName))
         .filter(name -> !this.isFriend(name))
         .filter(name -> normalized.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(normalized))
         .sorted(String.CASE_INSENSITIVE_ORDER)
         .map(name -> new CommandSuggestions.SuggestionEntry(name, "Добавить игрока в друзья", this.completion(name, partial, leadingSpace, false), false))
         .toList();
   }

   private List<CommandSuggestions.SuggestionEntry> buildFriendEntries(String partial) {
      String normalized = partial.toLowerCase(Locale.ROOT);
      return FriendManager.getFriends().stream()
         .map(Friend::getName)
         .filter(name -> normalized.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(normalized))
         .sorted(Comparator.comparing(name -> name.toLowerCase(Locale.ROOT)))
         .map(name -> new CommandSuggestions.SuggestionEntry(name, "Удалить игрока из друзей", this.completion(name, partial, false, false), false))
         .toList();
   }

   private boolean isFriend(String name) {
      return FriendManager.getFriends().stream().anyMatch(friend -> friend.getName().equalsIgnoreCase(name));
   }

   private String completion(String value, String partial, boolean leadingSpace, boolean trailingSpace) {
      StringBuilder completion = new StringBuilder();
      if (leadingSpace) {
         completion.append(' ');
      }

      completion.append(value.substring(Math.min(partial.length(), value.length())));
      if (trailingSpace) {
         completion.append(' ');
      }

      return completion.isEmpty() ? null : completion.toString();
   }

   private String operationDescription(String operation) {
      return switch (operation) {
         case "add" -> "Добавить игрока в друзья";
         case "remove" -> "Удалить игрока из друзей";
         case "list" -> "Показать список друзей";
         case "clear" -> "Очистить список друзей";
         default -> "";
      };
   }

   private AliasMatch findAlias(String input) {
      if (input == null) {
         return null;
      }

      for (String alias : this.aliases()) {
         if (input.length() >= alias.length() && input.regionMatches(true, 0, alias, 0, alias.length())) {
            if (input.length() == alias.length() || Character.isWhitespace(input.charAt(alias.length()))) {
               return new AliasMatch(alias.length());
            }
         }
      }

      return null;
   }

   private int countLeadingWhitespace(String value) {
      int index = 0;
      while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
         index++;
      }

      return index;
   }

   private String nextToken(String value) {
      int index = 0;
      while (index < value.length() && !Character.isWhitespace(value.charAt(index))) {
         index++;
      }

      return value.substring(0, index);
   }

   @Override
   public boolean matchesAliasPrefix(String input) {
      if (input == null || !input.startsWith(".")) {
         return false;
      }

      return this.aliases().stream().anyMatch(alias -> alias.startsWith(input));
   }

   private record AliasMatch(int aliasEnd) {
   }
}
