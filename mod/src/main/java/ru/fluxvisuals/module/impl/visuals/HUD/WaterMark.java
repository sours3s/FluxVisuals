package ru.fluxvisuals.module.impl.visuals.HUD;

import java.net.SocketAddress;
import java.util.Calendar;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import ru.fluxvisuals.config.StyleConfig;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;
import ru.fluxvisuals.util.render.texture.TextureLoader;

@Environment(EnvType.CLIENT)
public class WaterMark {
   public static MinecraftClient mc = MinecraftClient.getInstance();

   public static void waterMark(Renderer2D r2) {
      int mainColorGlow = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 50);
      String playerName = mc.player != null ? mc.player.getName().getString() : "";

      Calendar calendar = Calendar.getInstance();
      int hours = calendar.get(11);
      int minutes = calendar.get(12);
      String timeStr = String.format("%d:%02d", hours, minutes);

      float nameWidth = r2.measureText(FontRegistry.INTER_SEMIBOLD, playerName, 28.0F).width + 148.0F;
      float fpsWidth = r2.measureText(FontRegistry.INTER_SEMIBOLD, String.valueOf(mc.getCurrentFps()), 28.0F).width
         + r2.measureText(FontRegistry.INTER_SEMIBOLD, "fps", 28.0F).width + 40.0F;
      float timeWidth = r2.measureText(FontRegistry.INTER_MEDIUM, timeStr, 28.0F).width;

      // ===== Top group: logo + name + fps + time =====
      String clientName = StyleConfig.clientName != null && !StyleConfig.clientName.isBlank() ? StyleConfig.clientName : "Flux Visuals";
      float fluxWidth = r2.measureText(FontRegistry.INTER_SEMIBOLD, clientName, 30.0F).width;
      float logoW = StyleConfig.watermarkLogo ? 20.0F : 0.0F;
      float leftPanelWidth = 48.0F + fluxWidth + 12.0F;
      float panelGap = 9.0F;
      float rightPanelWidth = nameWidth + fpsWidth + timeWidth - 55.0F;
      float rightPanelOffset = leftPanelWidth + panelGap;
      float topGroupWidth = rightPanelOffset + rightPanelWidth;
      float topGroupHeight = 40.64F;

      DraggableManager.DragSession topSession = DraggableManager.getInstance()
            .beginDrag("watermarkTop", 20.0F, 20.0F, topGroupWidth, topGroupHeight);
      float tx = topSession.positionX();
      float ty = topSession.positionY();

      Hud.drawClientRect(r2, tx, ty, leftPanelWidth, 40.64F, 13.0F, 1.0F, 1.0F);
      float textX = tx + 34.0F;
      if (StyleConfig.watermarkGlow) {
         r2.shadow(tx + 23.27F, ty + 20.0F, 0.1F, 0.1F, 8.0F, 10.0F, 0.1F, mainColorGlow);
      }
      if (StyleConfig.watermarkLogo) {
         int logo = TextureLoader.load("assets/fluxvisuals/textures/gui/logo.png");
         r2.drawRgbaTexture(logo, tx + 5.0F, ty + 5.0F, 26.0F, 26.0F, -1, true);
         textX = tx + 34.0F;
      } else {
         textX = tx + 16.0F;
      }
      if (StyleConfig.watermarkName) {
         r2.text(FontRegistry.INTER_SEMIBOLD, textX, ty + 25.5F, 30.0F, clientName, Renderer2D.ColorUtil.getTextColor(1, 1));
      }

      Hud.drawClientRect(r2, tx + rightPanelOffset, ty, rightPanelWidth, 40.64F, 13.0F, 1.0F, 1.0F);
      float rx = tx + rightPanelOffset - 103.5F;
      r2.text(FontRegistry.ICONS, rx + 117.56F, ty + 28.0F, 32.0F, "q", Renderer2D.ColorUtil.getMainColor(1, 1));
      r2.text(FontRegistry.INTER_SEMIBOLD, rx + 140.75F, ty + 25.5F, 28.0F, playerName, Renderer2D.ColorUtil.getTextColor(1, 1));
      r2.rect(rx + nameWidth, ty + 15.0F, 2.34F, 11.21F, 4.0F, Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 80));
      r2.text(FontRegistry.ICONS, rx + nameWidth + 10.0F, ty + 28.0F, 32.0F, "r", Renderer2D.ColorUtil.getMainColor(1, 1));
      float fpsTextX = rx + nameWidth + 33.0F;
      r2.text(FontRegistry.INTER_SEMIBOLD, fpsTextX, ty + 25.5F, 28.0F, String.valueOf(mc.getCurrentFps()), Renderer2D.ColorUtil.getTextColor(1, 1));
      fpsTextX += r2.measureText(FontRegistry.INTER_SEMIBOLD, String.valueOf(mc.getCurrentFps()), 28.0F).width;
      r2.text(FontRegistry.INTER_SEMIBOLD, fpsTextX, ty + 25.5F, 28.0F, "fps", Renderer2D.ColorUtil.getMainColor(1, 1));
      r2.rect(rx + nameWidth + fpsWidth, ty + 15.0F, 2.34F, 11.21F, 4.0F, Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 80));
      r2.text(FontRegistry.ICONS, rx + nameWidth + fpsWidth + 10.0F, ty + 28.0F, 32.0F, "s", Renderer2D.ColorUtil.getMainColor(1, 1));
      r2.text(FontRegistry.INTER_SEMIBOLD, rx + nameWidth + fpsWidth + 33.0F, ty + 25.5F, 28.0F, timeStr, Renderer2D.ColorUtil.getTextColor(1, 1));

      DraggableManager.getInstance().endDrag(topSession);

      // ===== Bottom group: ip + ping =====
      String ip = resolveIp();
      String ping = resolvePing();

      String ipMain = ip;
      String ipSuffix = "";
      int lastDotIndex = ip.lastIndexOf(46);
      if (lastDotIndex > 0 && lastDotIndex < ip.length() - 1) {
         ipMain = ip.substring(0, lastDotIndex);
         ipSuffix = ip.substring(lastDotIndex);
      }

      float ipMainWidth = r2.measureText(FontRegistry.INTER_SEMIBOLD, ipMain, 28.0F).width;
      float ipSuffixWidth = r2.measureText(FontRegistry.INTER_SEMIBOLD, ipSuffix, 28.0F).width;
      float ipServer = ipMainWidth + ipSuffixWidth + 44.0F;
      String pingValue = ping.replace("ms", "");
      float pingValueWidth = r2.measureText(FontRegistry.INTER_SEMIBOLD, pingValue, 28.0F).width;
      float pingMsWidth = r2.measureText(FontRegistry.INTER_SEMIBOLD, "ms", 28.0F).width;
      float pingW = pingValueWidth + pingMsWidth + 42.0F;

      float bottomGroupWidth = ipServer + pingW;
      float bottomGroupHeight = 40.64F;

      DraggableManager.DragSession bottomSession = DraggableManager.getInstance()
            .beginDrag("watermarkBottom", 20.0F, 69.6F, bottomGroupWidth, bottomGroupHeight);
      float bx = bottomSession.positionX();
      float by = bottomSession.positionY();

      Hud.drawClientRect(r2, bx, by, ipServer + pingW, 40.64F, 13.0F, 1.0F, 1.0F);
      r2.text(FontRegistry.ICONS, bx + 14.06F, by + 28.0F, 36.0F, "t", Renderer2D.ColorUtil.getMainColor(1, 1));
      float ipTextX = bx + 38.0F;
      r2.text(FontRegistry.INTER_SEMIBOLD, ipTextX, by + 24.5F, 28.0F, ipMain, Renderer2D.ColorUtil.getTextColor(1, 1));
      ipTextX += ipMainWidth;
      r2.text(FontRegistry.INTER_SEMIBOLD, ipTextX, by + 24.5F, 28.0F, ipSuffix, Renderer2D.ColorUtil.getMainColor(1, 1));
      r2.rect(bx + ipServer, by + 14.5F, 2.34F, 11.21F, 4.0F, Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 80));
      r2.text(FontRegistry.ICONS, bx + ipServer + 8.0F, by + 26.0F, 32.0F, "u", Renderer2D.ColorUtil.getMainColor(1, 1));
      float pingTextX = bx + 28.0F + ipServer;
      r2.text(FontRegistry.INTER_SEMIBOLD, pingTextX, by + 24.5F, 28.0F, pingValue, Renderer2D.ColorUtil.getTextColor(1, 1));
      pingTextX += pingValueWidth;
      r2.text(FontRegistry.INTER_SEMIBOLD, pingTextX, by + 24.5F, 28.0F, "ms", Renderer2D.ColorUtil.getMainColor(1, 1));

      DraggableManager.getInstance().endDrag(bottomSession);
   }

   private static String resolveIp() {
      String ip = "N/A";
      if (mc.isConnectedToLocalServer()) {
         return "localhost";
      }
      String domainName = null;
      if (mc.getCurrentServerEntry() != null) {
         domainName = mc.getCurrentServerEntry().address;
      }

      if (domainName != null && !domainName.isEmpty() && domainName.matches(".*[a-zA-Z].*")) {
         int colonIndex = domainName.lastIndexOf(58);
         if (colonIndex > 0) {
            return domainName.substring(0, colonIndex);
         }
         return domainName;
      } else if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
         SocketAddress address = mc.getNetworkHandler().getConnection().getAddress();
         if (address != null) {
            String addr = address.toString();
            if (addr.startsWith("/")) {
               addr = addr.substring(1);
            }
            int colonIndex = addr.lastIndexOf(58);
            if (colonIndex > 0) {
               return addr.substring(0, colonIndex);
            }
            return addr;
         }
      }
      return ip;
   }

   private static String resolvePing() {
      if (mc.getNetworkHandler() != null && mc.player != null) {
         PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         if (playerListEntry != null) {
            return playerListEntry.getLatency() + "ms";
         }
      }
      return "0ms";
   }
}
