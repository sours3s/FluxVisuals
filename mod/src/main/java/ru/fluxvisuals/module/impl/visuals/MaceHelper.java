package ru.fluxvisuals.module.impl.visuals;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.player.AttackEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.ChatUtil;

@IModule(name = "Mace Helper", description = "Помощник для игры с булавой.", category = Category.Visuals, bind = 0)
@Environment(EnvType.CLIENT)
public class MaceHelper extends Module {
   public final BooleanSetting chatMessage = new BooleanSetting("Сообщение в чат", true);
   public final BooleanSetting handHighlight = new BooleanSetting("Подсветка в руке", true);
   public final BooleanSetting hotbarHighlight = new BooleanSetting("Подсветка в хотбаре", true);

   public MaceHelper() {
      this.addSettings(new Setting[]{chatMessage, handHighlight, hotbarHighlight});
   }

   public static boolean isMace(ItemStack stack) {
      return stack != null && !stack.isEmpty()
         ? stack.isOf(Items.MACE) || stack.getItem().toString().toLowerCase().contains("mace")
         : false;
   }

   public static boolean isHoldingMace() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client != null && client.player != null ? isMace(client.player.getMainHandStack()) || isMace(client.player.getOffHandStack()) : false;
   }

   public static int getChargePercentage() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.player != null) {
         float progress = client.player.getAttackCooldownProgress(0.0F);
         return (int)Math.min(100.0F, Math.max(0.0F, progress * 100.0F));
      } else {
         return 0;
      }
   }

   public static Color getChargeColor() {
      return getChargeColor(getChargePercentage());
   }

   public static Color getChargeColor(int charge) {
      if (charge >= 80) {
         return new Color(40, 220, 50);
      } else {
         return charge >= 41 ? new Color(255, 140, 0) : new Color(240, 40, 40);
      }
   }

   @EventInit
   public void onAttack(AttackEvent event) {
      if (mc.player != null && event.getTarget() != null) {
         ItemStack mainHand = mc.player.getMainHandStack();
         if (isMace(mainHand) && this.chatMessage.get()) {
            int charge = getChargePercentage();
            ChatUtil.addChatMessage("Булава заряжена на " + charge + "%");
         }
      }
   }
}
