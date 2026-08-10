package ru.fluxvisuals.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.discord.DiscordIpc;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.lifecycle.ClientTickEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;

@IModule(name = "Discord RPC", description = "Показывает активность FluxVisuals в Discord (с кнопкой на сервер)", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class DiscordRCP extends Module {
   private final DiscordIpc ipc = new DiscordIpc();
   private volatile boolean started = false;
   private Thread thread;
   private long lastReconnect = 0L;

   public DiscordRCP() {
      this.enable = true; // включено сразу — RPC пишет только в named pipe, без риска
      this.onEnable();    // регистрируем обработчики событий
   }

   @EventInit
   public void onTick(ClientTickEvent e) {
      startRpc();
   }

   private synchronized void startRpc() {
      if (started) return;
      started = true;

      thread = new Thread(() -> {
         long startTs = System.currentTimeMillis() / 1000L;
         while (!Thread.currentThread().isInterrupted()) {
            try {
               if (!ipc.isConnected()) {
                  // переподключение не чаще раза в 5 секунд, пока Discord не поднимется
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

   @Override
   public void onDisable() {
      started = false;
      if (thread != null) {
         thread.interrupt();
      }
      ipc.shutdown();
   }
}
