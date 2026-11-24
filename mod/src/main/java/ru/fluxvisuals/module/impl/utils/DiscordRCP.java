package ru.fluxvisuals.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.MinecraftClient;
import ru.fluxvisuals.discord.DiscordIpc;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.lifecycle.ClientTickEvent;
import ru.fluxvisuals.license.LaunchGate;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;

/**
 * Discord Rich Presence. The details line shows the loader UID, the state line shows
 * the current server (localhost for a singleplayer world).
 */
@IModule(name = "Discord RPC", description = "Показывает статус FluxVisuals в Discord", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class DiscordRCP extends Module {
   private static final AtomicBoolean RPC_OWNER = new AtomicBoolean(false);
   private final DiscordIpc ipc = new DiscordIpc();
   private volatile boolean started = false;
   private volatile boolean owner = false;
   private volatile String server = "localhost";
   private Thread thread;
   private long lastReconnect = 0L;

   public DiscordRCP() {
      this.enable = true;
   }

   @EventInit
   public void onTick(ClientTickEvent e) {
      if (enable) {
         server = resolveServer();
         startRpc();
      } else {
         stopRpc();
      }
   }

   private static String resolveServer() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.world == null || client.isConnectedToLocalServer()) {
         return "localhost";
      }
      if (client.getCurrentServerEntry() != null) {
         String address = client.getCurrentServerEntry().address;
         if (address != null && !address.isBlank()) {
            return address;
         }
      }
      try {
         if (client.getNetworkHandler() != null && client.getNetworkHandler().getConnection() != null) {
            String address = client.getNetworkHandler().getConnection().getAddress().toString();
            if (address.startsWith("/")) {
               address = address.substring(1);
            }
            if (!address.isBlank()) {
               return address;
            }
         }
      } catch (Exception ignored) {
      }
      return "localhost";
   }

   private synchronized void startRpc() {
      if (started) return;
      if (!RPC_OWNER.compareAndSet(false, true)) {
         return; // Another module instance already owns the Discord pipe.
      }
      started = true;
      owner = true;

      thread = new Thread(() -> {
         long startTs = System.currentTimeMillis() / 1000L;
         while (!Thread.currentThread().isInterrupted() && enable) {
            try {
               if (!ipc.isConnected()) {
                  long now = System.currentTimeMillis();
                  if (now - lastReconnect >= 5000L) {
                     lastReconnect = now;
                     ipc.connect();
                  }
               } else {
                  String details = "UID: " + LaunchGate.getUid();
                  ipc.updatePresence(server, details, startTs);
               }
               Thread.sleep(2000L);
            } catch (InterruptedException ignored) {
               break;
            } catch (Exception ignored) {
            }
         }
      }, "FluxVisuals-Discord-RPC");
      thread.setDaemon(true);
      thread.start();
   }

   private synchronized void stopRpc() {
      if (!started) return;
      started = false;
      if (thread != null) {
         thread.interrupt();
         thread = null;
      }
      if (owner) {
         owner = false;
         RPC_OWNER.set(false);
         ipc.shutdown();
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      stopRpc();
   }

   @Override
   public void onEnable() {
      super.onEnable();
   }
}
