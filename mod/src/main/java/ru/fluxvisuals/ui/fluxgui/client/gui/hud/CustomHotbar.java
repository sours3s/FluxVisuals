package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import ru.fluxvisuals.ui.fluxgui.client.gui.render.RoundedRenderer;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.ShadowRender;
import ru.fluxvisuals.ui.fluxgui.module.ModuleManager;
import ru.fluxvisuals.ui.fluxgui.module.render.Animations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

public final class CustomHotbar implements HudElement {
    public static final CustomHotbar INSTANCE = new CustomHotbar();
    private static final float X = 769.0F;
    private static final float Y = 1032.0F;
    private static final float WIDTH = 382.0F;
    private static final float HEIGHT = 48.0F;
    private static final float RADIUS = 13.0F;
    private static final float SLOT_X = 11.0F;
    private static final float SLOT_Y = 5.0F;
    private static final float SLOT_STEP = 40.0F;
    private static final float SLOT_SIZE = 38.0F;
    private static final float ITEM_INSET = 3.0F;
    private static final float ITEM_SIZE = 32.0F;
    static final float FIXED_SCREEN_WIDTH = 182.0F;
    private float selectedSlot = Float.NaN;

    private CustomHotbar() {
    }

    @Override
    public String id() {
        return "custom_hotbar";
    }

    @Override
    public float x() {
        return 769.0F;
    }

    @Override
    public float y() {
        return 1032.0F;
    }

    @Override
    public float width() {
        return 382.0F;
    }

    @Override
    public float height() {
        return 48.0F;
    }

    @Override
    public boolean hasContent() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && !player.isSpectator();
    }

    @Override
    public void render(DrawContext context, Hud hud) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null) {
            hud.panel(context, 769.0F, 1032.0F, 382.0F, 48.0F, 13.0F);
            float targetSlot = player.getInventory().getSelectedSlot();
            Animations animations = ModuleManager.INSTANCE.getModule(Animations.class);
            if (animations != null && animations.isEnabled() && animations.isModeEnabled("Смена слотов")) {
                targetSlot = animations.getSmoothedSlot();
            }

            if (!Float.isFinite(this.selectedSlot)) {
                this.selectedSlot = targetSlot;
            }

            this.selectedSlot = MathHelper.lerp(0.24F, this.selectedSlot, targetSlot);
            if (Math.abs(this.selectedSlot - targetSlot) < 0.01F) {
                this.selectedSlot = targetSlot;
            }

            float selectionX = 780.0F + this.selectedSlot * 40.0F;
            int accent = Hud.animatedColor(Hud.accent());
            ShadowRender.draw(context, selectionX, 1037.0F, 38.0F, 38.0F, 10.0F, 9.0F, Hud.fade(1426063360 | accent & 16777215, Hud.animatedOpacity()));
            RoundedRenderer.drawRoundedRect(context, selectionX, 1037.0F, 38.0F, 38.0F, 10.0F, Hud.animatedColor(419430399));
            RoundedRenderer.drawAntialiasedRoundedOutline(context, selectionX, 1037.0F, 38.0F, 38.0F, 10.0F, 2.0F, accent);

            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = player.getInventory().getStack(slot);
                float itemX = 780.0F + slot * 40.0F + 3.0F;
                float itemY = 1040.0F;
                drawStack(context, stack, itemX, itemY, 32.0F);
            }
        }
    }

    private static void drawStack(DrawContext context, ItemStack stack, float x, float y, float size) {
        if (!stack.isEmpty()) {
            Hud.item(context, stack, x, y, size);
            float scale = size / 16.0F;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(scale, scale);
            MinecraftClient client = MinecraftClient.getInstance();
            context.drawStackOverlay(client.textRenderer, stack, 0, 0);
            ru.fluxvisuals.ui.fluxgui.module.render.Cooldowns.renderHotbarCooldown(context, stack, 0, 0);
            context.getMatrices().popMatrix();
        }
    }
}
