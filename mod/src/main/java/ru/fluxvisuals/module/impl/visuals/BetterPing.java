package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Better Ping Display — чистый вывод пинга с цветовой маркировкой.
 */
@IModule(name = "Better Ping", description = "Чистый вывод пинга с цветовой маркировкой", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class BetterPing extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public final BooleanSetting showMs = new BooleanSetting("Show ms", true);
   public final BooleanSetting colored = new BooleanSetting("Colored", true);

   public BetterPing() {
      this.addSettings(new Setting[]{showMs, colored});
   }

   @EventInit
   public void onRender(EventScreen e) {
      if (!this.enable || mc.player == null || mc.world == null) return;
      if (mc.currentScreen instanceof ChatScreen) return;
      Renderer2D r2 = e.renderer();
      if (r2 == null) return;

      PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
      if (entry == null) return;

      int ping = entry.getLatency();
      String text = String.valueOf(ping) + (showMs.get() ? " ms" : "");

      int color;
      if (colored.get()) {
         if (ping < 50) color = 0xFF55FF55;
         else if (ping < 150) color = 0xFFFFFF55;
         else color = 0xFFFF5555;
      } else {
         color = Renderer2D.ColorUtil.getTextColor(1, 1);
      }

      float x = mc.getWindow().getScaledWidth() - 60.0F;
      float y = 5.0F;
      float fontSize = 10.0F;
      r2.text(FontRegistry.INTER_MEDIUM, x, y, fontSize, text, color);
   }
}
