package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import ru.fluxvisuals.ui.fluxgui.client.gui.render.RoundedRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

public final class ArmorHud implements HudElement {
    public static final ArmorHud INSTANCE = new ArmorHud();
    private static final float X = 40.0F;
    private static final float Y = 930.0F;
    private static final float WIDTH = 178.0F;
    private static final float HEIGHT = 48.0F;
    private static final float RADIUS = 10.0F;
    private static final float SLOT_X = 10.0F;
    private static final float SLOT_Y = 6.0F;
    private static final float SLOT_STEP = 41.0F;
    private static final float ITEM_SIZE = 30.0F;
    private static final float BAR_Y = 39.0F;
    private static final float BAR_WIDTH = 30.0F;
    private static final float BAR_HEIGHT = 3.0F;
    private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ArmorHud() {
    }

    @Override
    public String id() {
        return "armor_hud";
    }

    @Override
    public float x() {
        return 40.0F;
    }

    @Override
    public float y() {
        return 930.0F;
    }

    @Override
    public float width() {
        return 178.0F;
    }

    @Override
    public float height() {
        return 48.0F;
    }

    @Override
    public boolean hasContent() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }

        for (EquipmentSlot slot : SLOTS) {
            if (!player.getEquippedStack(slot).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void render(DrawContext context, Hud hud) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            hud.panel(context, 40.0F, 930.0F, 178.0F, 48.0F, 10.0F);

            for (int index = 0; index < SLOTS.length; index++) {
                ItemStack stack = player.getEquippedStack(SLOTS[index]);
                float slotX = 50.0F + index * 41.0F;
                if (!stack.isEmpty()) {
                    Hud.item(context, stack, slotX, 936.0F, 30.0F);
                }

                float durability = durability(stack);
                RoundedRenderer.drawRoundedRect(context, slotX, 969.0F, 30.0F, 3.0F, 1.5F, Hud.animatedColor(553648127));
                if (durability > 0.0F) {
                    float filled = Math.max(3.0F, 30.0F * durability);
                    RoundedRenderer.drawRoundedRect(context, slotX, 969.0F, filled, 3.0F, 1.5F, Hud.animatedColor(durabilityColor(durability)));
                }
            }
        }
    }

    private static float durability(ItemStack stack) {
        if (!stack.isEmpty() && stack.isDamageable() && stack.getMaxDamage() > 0) {
            return MathHelper.clamp(1.0F - (float)stack.getDamage() / stack.getMaxDamage(), 0.0F, 1.0F);
        } else {
            return stack.isEmpty() ? 0.0F : 1.0F;
        }
    }

    private static int durabilityColor(float durability) {
        float red = MathHelper.clamp((1.0F - durability) * 2.0F, 0.0F, 1.0F);
        float green = MathHelper.clamp(durability * 2.0F, 0.0F, 1.0F);
        int r = Math.round(MathHelper.lerp(red, 92.0F, 255.0F));
        int g = Math.round(MathHelper.lerp(green, 76.0F, 224.0F));
        return 0xFF000000 | r << 16 | g << 8 | 88;
    }
}
