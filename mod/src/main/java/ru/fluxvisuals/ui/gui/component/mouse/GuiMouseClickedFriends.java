package ru.fluxvisuals.ui.gui.component.mouse;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.config.friend.Friend;
import ru.fluxvisuals.config.friend.FriendManager;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.ui.gui.component.render.GuiRenderFriends;
import ru.fluxvisuals.ui.gui.component.render.GuiRenderMain;

/**
 * Клики вкладки «Друзья»: фокус строки поиска, добавление/удаление друзей.
 */
@Environment(EnvType.CLIENT)
public class GuiMouseClickedFriends extends GuiScreen {
   private GuiMouseClickedFriends() {
   }

   public static boolean mouseClickedFriends(int mouseX, int mouseY, int button) {
      if (button != 0) {
         return false;
      }
      if (GuiScreen.selectedCategories == null || GuiScreen.selectedCategories != Category.Friends) {
         return false;
      }

      float x = GuiRenderFriends.rowX();
      float w = GuiRenderFriends.rowW();
      float inputH = 24.0F;
      float inputY = GuiLayout.clipY() + 2.0F;
      float listY0 = inputY + inputH + 6.0F;
      float headerH = 18.0F;
      float rowH = 20.0F;
      float gapR = 3.0F;
      float scroll = GuiScreen.getScrollUtil().getScroll();

      // Фокус поля поиска.
      if (GuiRenderMain.isHovered(mouseX, mouseY, x, inputY, w, inputH)) {
         GuiScreen.friendsSearchEditing = true;
         return true;
      }

      List<String> online = GuiRenderFriends.filteredOnline();
      List<Friend> friends = GuiRenderFriends.filteredFriends();
      boolean showManual = !GuiScreen.friendsSearchText.trim().isEmpty()
            && online.stream().noneMatch(n -> n.equalsIgnoreCase(GuiScreen.friendsSearchText.trim()));

      int onlineRows = online.size() + (showManual ? 1 : 0);
      float rowIndex = 0.0F;

      // Кнопка «+» в строке «Добавить по нику».
      if (showManual) {
         rowIndex++;
         float y = listY0 + rowIndex * (rowH + gapR) - scroll;
         rowIndex++;
         if (GuiRenderFriends.isSmallButton(x, y, w, mouseX, mouseY)) {
            addFriend(GuiScreen.friendsSearchText.trim());
            return true;
         }
      }

      // Строки онлайн-игроков.
      for (String name : online) {
         float y = listY0 + rowIndex * (rowH + gapR) - scroll;
         rowIndex++;
         if (!FriendManager.isFriend(name) && GuiRenderFriends.isSmallButton(x, y, w, mouseX, mouseY)) {
            addFriend(name);
            return true;
         }
      }

      // Заголовок «Друзья» + строки друзей.
      if (friends.size() > 0) {
         rowIndex++;
      }
      for (Friend f : friends) {
         float y = listY0 + rowIndex * (rowH + gapR) - scroll;
         rowIndex++;
         if (GuiRenderFriends.isSmallButton(x, y, w, mouseX, mouseY)) {
            removeFriend(f.getName());
            return true;
         }
      }

      return false;
   }

   /** Публичный вход для добавления по нику (Enter в поле поиска, команды). */
   public static void addFriendDirect(String name) {
      addFriend(name);
   }

   private static void addFriend(String name) {
      if (name == null || name.isEmpty()) {
         return;
      }
      if (FluxVisualsClient.get == null || FluxVisualsClient.get.friendManager == null) {
         return;
      }
      if (name.equalsIgnoreCase(GuiScreen.mc.player.getName().getString())) {
         return; // нельзя добавить себя
      }
      FluxVisualsClient.get.friendManager.add(name);
      FluxVisualsClient.get.manager.get(Hud.class).showNotification("+", "Друг " + name + " добавлен", 3000L, 0xFF00FF88);
   }

   private static void removeFriend(String name) {
      if (name == null || name.isEmpty() || FluxVisualsClient.get == null || FluxVisualsClient.get.friendManager == null) {
         return;
      }
      FluxVisualsClient.get.friendManager.remove(name);
      FluxVisualsClient.get.manager.get(Hud.class).showNotification("-", "Друг " + name + " удалён", 3000L, 0xFFFF5353);
   }
}
