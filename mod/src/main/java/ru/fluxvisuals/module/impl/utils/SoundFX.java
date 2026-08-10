package ru.fluxvisuals.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.lifecycle.ClientTickEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.utils.SoundUtil;

@IModule(name = "Sound FX", description = "Кастомные звуки: отрыв тотема, лом брони, лом инструмента.", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class SoundFX extends Module {
   public final BooleanSetting totemPop = new BooleanSetting("Totem Pop", true);
   public final SliderSetting totemVolume = new SliderSetting("Totem Volume", 50.0F, 0.0F, 100.0F, 1.0F, true);
   public final BooleanSetting armorBreak = new BooleanSetting("Armor Break", true);
   public final SliderSetting armorVolume = new SliderSetting("Armor Volume", 50.0F, 0.0F, 100.0F, 1.0F, true);
   public final BooleanSetting toolBreak = new BooleanSetting("Tool Break", true);
   public final SliderSetting toolVolume = new SliderSetting("Tool Volume", 50.0F, 0.0F, 100.0F, 1.0F, true);

   private final ItemStack[] prevArmor = new ItemStack[4];
   private ItemStack prevMainHand = ItemStack.EMPTY;

   @Override
   public void onEnable() {
      super.onEnable();
      if (mc.player != null) {
         PlayerEntity p = mc.player;
         prevArmor[0] = p.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).copy();
         prevArmor[1] = p.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).copy();
         prevArmor[2] = p.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS).copy();
         prevArmor[3] = p.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET).copy();
         prevMainHand = p.getMainHandStack().copy();
      }
   }

   @EventInit
   public void onTick(ClientTickEvent e) {
      if (!enable || mc.player == null) return;
      PlayerEntity p = mc.player;

      if (armorBreak.get()) {
         checkArmorBreak(p);
      }
      if (toolBreak.get()) {
         checkToolBreak(p);
      }
   }

   /**
    * Вызывается из {@link ru.fluxvisuals.mixin.ClientPlayNetworkHandlerMixin} при получении
    * {@link EntityStatusS2CPacket}. Статический: пакетные хендлеры не регистрируются через
    * EventManager (параметр не является {@code Event}).
    */
   public static void handleEntityStatus(EntityStatusS2CPacket packet) {
      SoundFX mod = FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null
            ? FluxVisualsClient.get.manager.get(SoundFX.class) : null;
      if (mod == null || !mod.enable || !mod.totemPop.get()) return;
      if (packet.getStatus() == 35) { // TOTEM_POP
         SoundUtil.playSound_wav("totem", mod.totemVolume.get() / 100.0F);
      }
   }

   private void checkArmorBreak(PlayerEntity p) {
      net.minecraft.entity.EquipmentSlot[] slots = new net.minecraft.entity.EquipmentSlot[]{
         net.minecraft.entity.EquipmentSlot.HEAD,
         net.minecraft.entity.EquipmentSlot.CHEST,
         net.minecraft.entity.EquipmentSlot.LEGS,
         net.minecraft.entity.EquipmentSlot.FEET
      };
      for (int i = 0; i < 4; i++) {
         ItemStack cur = p.getEquippedStack(slots[i]);
         ItemStack prev = prevArmor[i];
         if (prev.isDamageable() && (cur.isEmpty() || (!cur.isDamageable() || cur.getDamage() >= cur.getMaxDamage()))) {
            if (!prev.isEmpty() && prev.getDamage() < prev.getMaxDamage()) {
               SoundUtil.playSound_wav("armor_break", armorVolume.get() / 100.0F);
            }
         }
         prevArmor[i] = cur.copy();
      }
   }

   private void checkToolBreak(PlayerEntity p) {
      ItemStack cur = p.getMainHandStack();
      if (prevMainHand.isDamageable() && (cur.isEmpty() || (!cur.isDamageable() || cur.getDamage() >= cur.getMaxDamage()))) {
         if (!prevMainHand.isEmpty() && prevMainHand.getDamage() < prevMainHand.getMaxDamage()) {
            SoundUtil.playSound_wav("tool_break", toolVolume.get() / 100.0F);
         }
      }
      prevMainHand = cur.copy();
   }
}