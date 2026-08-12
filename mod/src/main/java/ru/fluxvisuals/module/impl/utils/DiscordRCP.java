package ru.fluxvisuals.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.discord.DiscordIpc;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.lifecycle.ClientTickEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;

/**
 * Discord RPC — показывает активность FluxVisuals в Discord.
 * Включается/выключается в ClickGUI, корректно подключается/отключается.
 */
@IModule(name = "Discord RPC", description = "Показывает активность FluxVisuals в Discord (с кнопкой на сервер)", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class DiscordRCP extends Module {
   private final DiscordIpc ipc = new DiscordIpc();
   private volatile boolean started = false;
   private Thread thread;
   private long lastReconnect = 0L;

   public DiscordRCP() {
      // По умолчанию включен
      this.enable = true;
   }

   @EventInit
   public void onTick(ClientTickEvent e) {
      if (enable) {
         startRpc();
      } else {
         stopRpc();
      }
   }

   private synchronized void startRpc() {
      if (started) return;
      started = true;

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
                  ipc.updatePresence(null, "Flux Visuals", startTs);
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
      ipc.shutdown();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      stopRpc();
   }

   @Override
   public void onEnable() {
      super.onEnable();
      // startRpc будет вызван на следующем тике
   }
}