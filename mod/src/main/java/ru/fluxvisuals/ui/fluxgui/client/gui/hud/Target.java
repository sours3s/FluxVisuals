package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import ru.fluxvisuals.ui.fluxgui.client.gui.render.RoundedRenderer;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.TextRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;

public final class Target implements HudElement {
    public static final Target INSTANCE = new Target();
    private static final float X = 863.0F;
    private static final float Y = 755.0F;
    private static final float HEIGHT = 52.0F;
    private static final float HEAD_X = 10.0F;
    private static final float HEAD_Y = 10.0F;
    private static final float HEAD_SIZE = 32.0F;
    private static final float HEAD_RADIUS = 6.0F;
    private static final int SKIN_FACE_SIZE = 8;
    private static final float SEPARATOR_Y = 18.0F;
    private static final float SEPARATOR_HEIGHT = 16.0F;
    private static final float HEAD_SEPARATOR_X = 48.0F;
    private static final float CONTENT_X = 58.0F;
    private static final float NAME_BASELINE = 23.595F;
    private static final float ARMOR_Y = 29.0F;
    private static final float ARMOR_SIZE = 13.0F;
    private static final float ARMOR_STEP = 16.0F;
    private static final float CONTENT_GAP = 12.0F;
    private static final float BAR_GAP = 10.0F;
    private static final float HEALTH_TEXT_GAP = 10.0F;
    private static final float RIGHT_PAD = 14.0F;
    private static final float HEALTH_BAR_Y = 12.0F;
    private static final float HEALTH_BAR_WIDTH = 3.5F;
    private static final float HEALTH_BAR_HEIGHT = 28.0F;
    private static final float HEALTH_BASELINE = 31.083F;
    private static final float MIN_BAR_HEIGHT = 0.5F;
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static LivingEntity currentTarget;
    private static int lastResolveAge = Integer.MIN_VALUE;
    private static LivingEntity lastResolvedTarget;

    private Target() {
    }

    @Override
    public String id() {
        return "target";
    }

    @Override
    public float x() {
        return 863.0F;
    }

    @Override
    public float y() {
        return 755.0F;
    }

    @Override
    public float width() {
        LivingEntity subject = subject();
        return barX(subject) - 863.0F + 3.5F + 10.0F + TextRender.width(healthText(subject), 13.224F) + 14.0F;
    }

    @Override
    public float height() {
        return 52.0F;
    }

    @Override
    public boolean hasContent() {
        return resolveTarget() != null;
    }

    @Override
    public void render(DrawContext context, Hud hud) {
        LivingEntity subject = subject();
        String health = healthText(subject);
        float separatorX = separatorX(subject);
        float barX = barX(subject);
        float textX = barX + 3.5F + 10.0F;
        float panelWidth = textX - 863.0F + TextRender.width(health, 13.224F) + 14.0F;
        hud.panel(context, 863.0F, 755.0F, panelWidth, 52.0F, 10.0F);
        RoundedRenderer.drawRoundedRect(context, 873.0F, 765.0F, 32.0F, 32.0F, 6.0F, Hud.animatedColor(452984831));
        if (subject instanceof AbstractClientPlayerEntity playerSubject) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(873.0F, 765.0F);
            context.getMatrices().scale(4.0F, 4.0F);
            PlayerSkinDrawer.draw(context, playerSubject.getSkin(), 0, 0, 8, Hud.animatedColor(-1));
            context.getMatrices().popMatrix();
        } else if (subject != null) {
            try {
                int x1 = 873;
                int y1 = 765;
                int x2 = 905;
                int y2 = 797;
                InventoryScreen.drawEntity(context, x1, y1, x2, y2, 14, 0.0625F, 0.0F, 0.0F, subject);
            } catch (Exception var14) {
            }
        }

        Hud.separator(context, 911.0F, 773.0F, 16.0F, 452984831);
        TextRender.draw(context, name(subject), 921.0F, 778.595F, 14.241F, Hud.animatedColor(-2434342));

        for (int index = 0; index < ARMOR_SLOTS.length; index++) {
            ItemStack stack = subject != null ? subject.getEquippedStack(ARMOR_SLOTS[index]) : ItemStack.EMPTY;
            Hud.item(context, stack, 921.0F + index * 16.0F, 784.0F, 13.0F);
        }

        Hud.separator(context, separatorX, 773.0F, 16.0F, 452984831);
        float barHeight = 28.0F * healthRatio(subject);
        if (barHeight >= 0.5F) {
            float radius = Math.min(3.5F, barHeight) / 2.0F;
            RoundedRenderer.drawRoundedRect(context, barX, 795.0F - barHeight, 3.5F, barHeight, radius, Hud.animatedColor(Hud.accent()));
        }

        TextRender.draw(context, health, textX, 786.083F, 13.224F, Hud.animatedColor(Hud.accent()));
    }

    private static float separatorX(LivingEntity subject) {
        float armor = (ARMOR_SLOTS.length - 1) * 16.0F + 13.0F;
        float content = Math.max(TextRender.width(name(subject), 14.241F), armor);
        return 921.0F + content + 12.0F;
    }

    private static float barX(LivingEntity subject) {
        return separatorX(subject) + 2.0F + 10.0F;
    }

    private static String name(LivingEntity subject) {
        return subject == null ? "" : subject.getName().getString();
    }

    private static String healthText(LivingEntity subject) {
        return String.valueOf(Math.round(subject == null ? 0.0F : subject.getHealth()));
    }

    private static float healthRatio(LivingEntity subject) {
        return subject == null ? 0.0F : MathHelper.clamp(subject.getHealth() / Math.max(subject.getMaxHealth(), 1.0F), 0.0F, 1.0F);
    }

    private static LivingEntity subject() {
        LivingEntity target = resolveTarget();
        return target != null ? target : MinecraftClient.getInstance().player;
    }

    private static LivingEntity resolveTarget() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            currentTarget = null;
            lastResolvedTarget = null;
            lastResolveAge = Integer.MIN_VALUE;
            return null;
        }

        // При открытом чате TargetHUD показывается только на себя —
        // это позволяет видеть/перетаскивать элемент, не целясь в другого игрока.
        if (client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            currentTarget = client.player;
            lastResolvedTarget = client.player;
            lastResolveAge = client.player.age;
            return client.player;
        }

        if (lastResolveAge == client.player.age) {
            return lastResolvedTarget;
        }

        lastResolveAge = client.player.age;
        LivingEntity pointed = null;
        if (client.targetedEntity instanceof LivingEntity living) {
            pointed = living;
        } else if (client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) {
            pointed = living;
        }

        if (isValidTarget(pointed) && client.player.distanceTo(pointed) <= 4.0F) {
            currentTarget = pointed;
            lastResolvedTarget = pointed;
            return pointed;
        }

        // Панель должна исчезать сразу, как только прицел сошёл с цели.
        // Раньше здесь оставался запомненный/ближайший игрок — из-за этого
        // TargetHUD отображался, даже когда никто не наведен.
        currentTarget = null;
        lastResolvedTarget = null;
        return null;
    }

    private static boolean isValidTarget(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        return entity != null && entity != client.player && entity.isAlive() && !entity.isInvisible();
    }
}
