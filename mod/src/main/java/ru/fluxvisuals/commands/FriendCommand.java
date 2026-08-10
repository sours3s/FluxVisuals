package ru.fluxvisuals.commands;

import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.config.friend.Friend;
import ru.fluxvisuals.config.friend.FriendManager;
import ru.fluxvisuals.ui.Colors;

@Environment(EnvType.CLIENT)
public final class FriendCommand implements Command {
   private static final FriendCommand INSTANCE = new FriendCommand();
   private static final List<String> ALIASES = List.of(".friend", ".f");

   private FriendCommand() {
   }

   public static FriendCommand getInstance() {
      return INSTANCE;
   }

   @Override
   public String name() {
      return "friend";
   }

   @Override
   public List<String> aliases() {
      return ALIASES;
   }

   @Override
   public String usage() {
      return ".friend <ник|add|remove|list|clear>";
   }

   @Override
   public String description() {
      return "Управление списком друзей";
   }

   @Override
   public void execute(CommandContext context, String arguments) throws CommandException {
      FriendManager manager = this.getManager();
      if (arguments == null || arguments.isBlank()) {
         context.sendInfo("Использование: " + this.usage());
         return;
      }

      String[] parts = arguments.trim().split("\\s+", 2);
      String operation = parts[0].toLowerCase(Locale.ROOT);
      String name = parts.length > 1 ? parts[1].trim() : "";
      switch (operation) {
         case "add" -> this.addFriend(context, manager, name);
         case "remove", "delete", "del" -> this.removeFriend(context, manager, name);
         case "list" -> {
            this.requireNoAdditionalArgument(name);
            this.listFriends(context);
         }
         case "clear" -> {
            this.requireNoAdditionalArgument(name);
            this.clearFriends(context, manager);
         }
         default -> {
            if (parts.length > 1) {
               throw new CommandException("Использование: " + this.usage());
            }

            this.addFriend(context, manager, parts[0]);
         }
      }
   }

   private FriendManager getManager() throws CommandException {
      if (FluxVisualsClient.get == null || FluxVisualsClient.get.friendManager == null) {
         throw new CommandException("Список друзей ещё не готов");
      }

      return FluxVisualsClient.get.friendManager;
   }

   private void addFriend(CommandContext context, FriendManager manager, String inputName) throws CommandException {
      String name = this.validateName(inputName);
      String resolvedName = this.resolveOnlineName(context, name);
      if (context.client().player != null && context.client().player.getName().getString().equalsIgnoreCase(resolvedName)) {
         throw new CommandException("Нельзя добавить себя в список друзей");
      }

      if (manager.isFriend(resolvedName)) {
         throw new CommandException("Игрок " + resolvedName + " уже находится в списке друзей");
      }

      manager.add(resolvedName);
      context.sendSuccess("Игрок " + resolvedName + " добавлен в список друзей");
   }

   private void removeFriend(CommandContext context, FriendManager manager, String inputName) throws CommandException {
      String name = this.validateName(inputName);
      Friend friend = FriendManager.getFriends().stream().filter(entry -> entry.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
      if (friend == null) {
         throw new CommandException("Игрок " + name + " отсутствует в списке друзей");
      }

      manager.remove(friend.getName());
      context.sendSuccess("Игрок " + friend.getName() + " удалён из списка друзей");
   }

   private void listFriends(CommandContext context) {
      List<Friend> friends = List.copyOf(FriendManager.getFriends());
      if (friends.isEmpty()) {
         context.sendInfo("Список друзей пуст");
         return;
      }

      MutableText message = Text.literal("Друзья (" + friends.size() + "): ");
      for (int index = 0; index < friends.size(); index++) {
         MutableText name = Text.literal(friends.get(index).getName()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(Colors.getClientPrimary())));
         message = message.append(name);
         if (index < friends.size() - 1) {
            message = message.append(Text.literal(" | "));
         }
      }

      context.sendInfo(message);
   }

   private void clearFriends(CommandContext context, FriendManager manager) throws CommandException {
      if (FriendManager.getFriends().isEmpty()) {
         throw new CommandException("Список друзей уже пуст");
      }

      manager.clearFriend();
      context.sendSuccess("Список друзей очищен");
   }

   private String validateName(String inputName) throws CommandException {
      String name = inputName == null ? "" : inputName.trim();
      if (name.isEmpty()) {
         throw new CommandException("Укажите имя игрока");
      }

      if (name.length() > 16 || !name.matches("[A-Za-z0-9_]+")) {
         throw new CommandException("Некорректное имя игрока: " + name);
      }

      return name;
   }

   private void requireNoAdditionalArgument(String argument) throws CommandException {
      if (!argument.isEmpty()) {
         throw new CommandException("Использование: " + this.usage());
      }
   }

   private String resolveOnlineName(CommandContext context, String name) {
      if (context.client().world == null) {
         return name;
      }

      return context.client().world.getPlayers().stream()
         .map(AbstractClientPlayerEntity::getName)
         .map(Text::getString)
         .filter(onlineName -> onlineName.equalsIgnoreCase(name))
         .findFirst()
         .orElse(name);
   }
}
