package ru.fluxvisuals.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.hud.ChatHud;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.utils.SoundUtil;
import ru.fluxvisuals.util.ChatCleanUtil;

@IModule(name = "Chat Sounds", description = "Звук при входящем сообщении и отдельный звук при упоминании ника.", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class ChatSounds extends Module {
   public final BooleanSetting chatSound = new BooleanSetting("Chat Message Sound", true);
   public final SliderSetting chatVolume = new SliderSetting("Chat Volume", 30.0F, 0.0F, 100.0F, 1.0F, true);
   public final BooleanSetting mentionSound = new BooleanSetting("Mention Sound", true);
   public final SliderSetting mentionVolume = new SliderSetting("Mention Volume", 50.0F, 0.0F, 100.0F, 1.0F, true);
   public final SliderSetting cooldown = new SliderSetting("Chat Cooldown (ms)", 500.0F, 0.0F, 5000.0F, 50.0F, false);

   private long lastChatSound = 0L;

   @Override
   public void onEnable() {
      super.onEnable();
      lastChatSound = 0L;
   }

   /** Хук вызывается из ChatHudMixin после очистки заголовков. */
   public static void onChatMessage(Text message) {
      ChatSounds instance = FluxVisualsClient.get.manager != null
         ? FluxVisualsClient.get.manager.get(ChatSounds.class)
         : null;
      if (instance == null || !instance.enable) return;

      if (instance.mc.player == null) return;
      String playerName = instance.mc.player.getName().getString();
      String msg = ChatCleanUtil.clean(message).getString();
      long now = System.currentTimeMillis();

      boolean isMention = msg.toLowerCase().contains(playerName.toLowerCase());
      if (isMention && instance.mentionSound.get()) {
         SoundUtil.playSound_wav("mention", instance.mentionVolume.get() / 100.0F);
      } else if (!isMention && instance.chatSound.get() && now - instance.lastChatSound > instance.cooldown.get()) {
         instance.lastChatSound = now;
         SoundUtil.playSound_wav("chat", instance.chatVolume.get() / 100.0F);
      }
   }
}