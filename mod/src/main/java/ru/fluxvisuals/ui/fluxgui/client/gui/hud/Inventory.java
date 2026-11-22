package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import ru.fluxvisuals.ui.fluxgui.client.gui.render.TextRender;
import ru.fluxvisuals.ui.fluxgui.compat.Gui;
import ru.fluxvisuals.ui.fluxgui.compat.Gui$Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

public final class Inventory implements HudElement {
    public static final Inventory INSTANCE = new Inventory();
    private static final float X = 1444.0F;
    private static final float Y = 540.0F;
    private static final float WIDTH = 307.0F;
    private static final float HEIGHT = 89.0F;
    private static final float RADIUS = 8.0F;
    private static final float SLOT_X = 8.0F;
    private static final float SLOT_Y = 6.0F;
    private static final float SLOT_SIZE = 19.0F;
    private static final float SLOT_STEP_X = 34.0F;
    private static final float SLOT_STEP_Y = 29.0F;
    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final int FIRST_SLOT = 9;
    private static final float COUNT_TEXT_SIZE = 8.5F;
    private static final float TICK_X = 34.0F;
    private static final float TICK_Y = 9.0F;
    private static final float TICK_WIDTH = 1.0F;
    private static final float TICK_HEIGHT = 13.0F;

    private Inventory() {
    }

    @Override
    public String id() {
        return "inventory";
    }

    @Override
    public float x() {
        return 1444.0F;
    }

    @Override
    public float y() {
        return 540.0F;
    }

    @Override
    public float width() {
        return 307.0F;
    }

    @Override
    public float height() {
        return 89.0F;
    }

    @Override
    public boolean hasContent() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }

        for (int slot = 9; slot < 36; slot++) {
            if (!player.getInventory().getStack(slot).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void render(DrawContext context, Hud hud) {
        hud.panel(context, 1444.0F, 540.0F, 307.0F, 89.0F, 8.0F);
        int separatorColor = Hud.animatedColor(452984831);
        Gui$Color separators = Gui.color(context);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 8; column++) {
                float x = 1478.0F + column * 34.0F;
                float y = 549.0F + row * 29.0F;
                separators.quad(x, y, x + 1.0F, y, x + 1.0F, y + 13.0F, x, y + 13.0F, separatorColor);
            }
        }

        separators.submit(context);
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    ItemStack stack = player.getInventory().getStack(9 + row * 9 + column);
                    float itemX = 1452.0F + column * 34.0F;
                    float itemY = 546.0F + row * 29.0F;
                    boolean hasCount = !stack.isEmpty() && stack.getCount() > 1;
                    Hud.item(context, stack, itemX - (hasCount ? 2.0F : 0.0F), itemY, 19.0F);
                    if (!stack.isEmpty() && stack.getCount() > 1) {
                        String count = Integer.toString(stack.getCount());
                        float countX = itemX + 19.0F - TextRender.width(count, 8.5F) + 3.5F;
                        TextRender.draw(context, count, countX, itemY + 19.0F - 1.5F, 8.5F, Hud.animatedColor(-520093697));
                    }
                }
            }
        }
    }
}
