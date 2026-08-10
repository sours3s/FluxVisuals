package ru.fluxvisuals.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.lifecycle.ClientTickEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.module.impl.visuals.Hud;

@IModule(name = "Lag Detector", description = "Уведомляет о провалах TPS сервера и скачках пинга.", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class LagDetector extends Module {
   public final BooleanSetting tpsWarning = new BooleanSetting("TPS Warning", true);
   public final SliderSetting tpsThreshold = new SliderSetting("TPS Threshold", 14.0F, 5.0F, 20.0F, 0.5F, false);
   public final BooleanSetting pingSpike = new BooleanSetting("Ping Spike Warning", true);
   public final SliderSetting pingThreshold = new SliderSetting("Ping Threshold (ms)", 250.0F, 50.0F, 1000.0F, 10.0F, false);
   public final SliderSetting cooldown = new SliderSetting("Notify Cooldown (s)", 10.0F, 1.0F, 60.0F, 1.0F, false);

   private static long lastTickTime = 0L;
   private static int tickCount = 0;
   private static float currentTps = 20.0F;
   private static long lastNotify = 0L;
   private static long lastPacketTime = 0L;
   private static float packetBasedTps = 20.0F;

   public static void onWorldTimePacket(WorldTimeUpdateS2CPacket packet) {
      long now = System.currentTimeMillis();
      if (lastPacketTime != 0L) {
         float dt = (now - lastPacketTime) / 1000.0F;
         if (dt > 0) {
            packetBasedTps = 1.0F / dt;
         }
      }
      lastPacketTime = now;
   }

   @EventInit
   public void onTick(ClientTickEvent e) {
      if (!enable || mc.player == null || mc.world == null) return;

      long now = System.currentTimeMillis();
      if (lastTickTime != 0L) {
         float dt = (now - lastTickTime) / 1000.0F;
         if (dt > 0) {
            tickCount++;
            if (dt >= 1.0F) {
               currentTps = tickCount / dt;
               tickCount = 0;
               lastTickTime = now;
            }
         }
      } else {
         lastTickTime = now;
      }

      // Проверка TPS
      if (tpsWarning.get()) {
         float tps = Math.min(currentTps, packetBasedTps); // берём худшее
         if (tps < tpsThreshold.get() && now - lastNotify > cooldown.get() * 1000L) {
            lastNotify = now;
            Hud hud = FluxVisualsClient.get.manager != null ? FluxVisualsClient.get.manager.get(Hud.class) : null;
            if (hud != null) {
               hud.showNotification("warn", "Сервер лагает (TPS ~" + Math.round(tps) + ")", 3000L, 0xFFFF5353);
            }
         }
      }

      // Проверка пинга
      if (pingSpike.get()) {
         if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) {
               int ping = entry.getLatency();
               if (ping > pingThreshold.get() && now - lastNotify > cooldown.get() * 1000L) {
                  lastNotify = now;
                  Hud hud = FluxVisualsClient.get.manager != null ? FluxVisualsClient.get.manager.get(Hud.class) : null;
                  if (hud != null) {
                     hud.showNotification("warn", "Пинг вспыхнул (" + ping + "ms)", 3000L, 0xFFFFB347);
                  }
               }
            }
         }
      }
   }
}