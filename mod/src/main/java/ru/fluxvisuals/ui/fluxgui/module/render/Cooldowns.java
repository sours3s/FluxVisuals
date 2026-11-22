package ru.fluxvisuals.ui.fluxgui.module.render;

import ru.fluxvisuals.mixin.accessor.ItemCooldownManagerAccessor;
import ru.fluxvisuals.mixin.accessor.ItemCooldownManagerEntryAccessor;
import ru.fluxvisuals.ui.fluxgui.setting.BooleanSetting;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;

/** Донорский рендер кулдаунов, адаптированный к текущему менеджеру модулей. */
public final class Cooldowns {
    public static final Cooldowns INSTANCE = new Cooldowns();
    private final BooleanSetting hotbar = new BooleanSetting("Хотбар", true);
    private final BooleanSetting inventory = new BooleanSetting("Инвентарь", true);

    private Cooldowns() {
    }

    public boolean isEnabled() {
        return true;
    }

    public static void renderHotbarCooldown(DrawContext context, ItemStack stack, int x, int y) {
        Cooldowns module = INSTANCE;
        if (module != null && module.isEnabled() && module.hotbar.getValue()) {
            renderCooldown(context, stack, x, y);
        }
    }

    public static void renderInventoryCooldown(DrawContext context, ItemStack stack, int x, int y) {
        Cooldowns module = INSTANCE;
        if (module != null && module.isEnabled() && module.inventory.getValue()) {
            renderCooldown(context, stack, x, y);
        }
    }

    private static void renderCooldown(DrawContext context, ItemStack stack, int x, int y) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && stack != null && !stack.isEmpty()) {
            float seconds = getRemainingSeconds(stack);
            if (!(seconds <= 0.0F)) {
                String text = seconds >= 10.0F ? String.valueOf((int)Math.ceil(seconds)) : String.format(Locale.US, "%.1f", seconds);
                context.fill(x, y, x + 16, y + 16, 1996488704);
                int textWidth = mc.textRenderer.getWidth(text);
                int textX = x + 8 - textWidth / 2;
                int textY = y + 5;
                context.getMatrices().pushMatrix();

                try {
                    context.getMatrices().translate(0.0F, 0.0F);
                    context.drawText(mc.textRenderer, text, textX, textY, -1, true);
                } finally {
                    context.getMatrices().popMatrix();
                }
            }
        }
    }

    private static float getRemainingSeconds(ItemStack stack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && !stack.isEmpty()) {
            ItemCooldownManager manager = mc.player.getItemCooldownManager();
            if (!manager.isCoolingDown(stack)) {
                return 0.0F;
            } else {
                ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor)manager;
                if (accessor.getEntries().get(manager.getGroup(stack)) instanceof ItemCooldownManagerEntryAccessor entry) {
                    float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
                    float remainingTicks = entry.getEndTick() - (accessor.getTick() + tickDelta);
                    return Math.max(0.0F, remainingTicks / 20.0F);
                } else {
                    return manager.getCooldownProgress(stack, 0.0F) * 10.0F;
                }
            }
        } else {
            return 0.0F;
        }
    }
}
