package ru.fluxvisuals.ui.gui.clickgui.util.net;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

public class TooltipPriceReader {
    public int getPrice(ItemStack itemStack) {
        Mc mc = Mc.INSTANCE;
        ClientPlayerEntity player = mc.getPlayer();
        GameOptions gameOptions = mc.getGameOptions();
        if (player == null) {
            return -1;
        }
        List tooltip = itemStack.getTooltip(Item.TooltipContext.DEFAULT, player, gameOptions.advancedItemTooltips ? TooltipType.ADVANCED : TooltipType.BASIC);
        for (int i = 1; i < tooltip.size(); i++) {
            String string = ((Text) tooltip.get(i)).getString();
            if (string.toLowerCase(Locale.ROOT).replace('e', (char) 1077).replace('a', (char) 1072).contains("цена")) {
                try {
                    return Integer.parseInt((String) string.chars().filter(i2 -> {
                        return i2 >= 48 && i2 <= 57;
                    }).mapToObj(i3 -> {
                        return String.valueOf((char) i3);
                    }).collect(Collectors.joining()));
                } catch (Throwable th) {
                }
            }
        }
        return -1;
    }
}
