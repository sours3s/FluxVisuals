package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import ru.fluxvisuals.ui.fluxgui.client.gui.render.TextRender;
import ru.fluxvisuals.ui.fluxgui.util.language.LanguageManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class Cooldowns implements HudElement {
    public static final Cooldowns INSTANCE = new Cooldowns();
    static final float X = 98.0F;
    static final float Y = 535.0F;
    static final float MARK_X = 169.25F;
    static final float MARK_Y = 14.75F;
    static final float ROW_X = 32.632F;
    static final float BADGE_WIDTH = 53.0F;
    static final float ITEM_SIZE = 16.9F;
    static final String TITLE = "Cooldowns";
    static final String PLACEHOLDER_TIMER = "0:09";
    static final ItemStack PLACEHOLDER_ITEM = new ItemStack(Items.ENDER_PEARL);
    static final Map<Identifier, float[]> TRACK = new HashMap<>();
    static final float MIN_DELTA = 0.02F;
    static int cachedPlayerAge = Integer.MIN_VALUE;
    static List<ItemStack> cachedActive = List.of();
    static float currentWidth = 193.0F;

    Cooldowns() {
    }

    @Override
    public String id() {
        return "cooldowns";
    }

    @Override
    public float x() {
        return 98.0F;
    }

    @Override
    public float y() {
        return 535.0F;
    }

    @Override
    public float width() {
        return currentWidth;
    }

    @Override
    public float height() {
        return Hud.listHeight(Math.max(1, active().size()));
    }

    @Override
    public boolean hasContent() {
        return !active().isEmpty();
    }

    @Override
    public void render(DrawContext context, Hud hud) {
        List<ItemStack> stacks = active();
        List<Cooldowns$CooldownRow> rows = new ArrayList<>();
        if (stacks.isEmpty()) {
            rows.add(
                new Cooldowns$CooldownRow(
                    PLACEHOLDER_ITEM,
                    LanguageManager.translateHudCooldownItemName(PLACEHOLDER_ITEM.getItem(), ru.fluxvisuals.ui.fluxgui.module.render.Hud.isEnglish()),
                    "0:09",
                    1.0F
                )
            );
        } else {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            ItemCooldownManager manager = player.getItemCooldownManager();

            for (ItemStack stack : stacks) {
                float progress = manager.getCooldownProgress(stack, 0.0F);
                float remaining = remainingSeconds(manager.getGroup(stack), progress);
                rows.add(
                    new Cooldowns$CooldownRow(
                        stack,
                        LanguageManager.translateHudCooldownItemName(stack.getItem(), ru.fluxvisuals.ui.fluxgui.module.render.Hud.isEnglish()),
                        Hud.formatSeconds(remaining),
                        progress
                    )
                );
            }
        }

        float targetWidth = targetWidth(rows);
        currentWidth = MathHelper.lerp(0.12F, currentWidth, targetWidth);
        if (Math.abs(currentWidth - targetWidth) < 0.05F) {
            currentWidth = targetWidth;
        }

        hud.listPanel(context, 98.0F, 535.0F, currentWidth, Hud.listHeight(Math.max(1, rows.size())), "Cooldowns", "clock", currentWidth - 23.75F, 14.75F);

        for (int index = 0; index < rows.size(); index++) {
            Cooldowns$CooldownRow row = rows.get(index);
            row(context, index, row.stack(), row.name(), row.timer(), row.progress());
        }
    }

    static void row(DrawContext context, int index, ItemStack stack, String name, String timer, float progress) {
        float offset = index * 27.0F;
        float itemOffset = 1.9499998F;
        Hud.item(context, stack, 108.0F - itemOffset, 589.0F + offset - itemOffset, 16.9F);
        TextRender.draw(context, name, 130.632F, 600.583F + offset, 13.224F, Hud.animatedColor(-2829100));
        float badgeWidth = badgeWidth(timer);
        float badgeX = currentWidth - badgeWidth - 10.0F;
        Hud.timerBadge(context, 98.0F + badgeX, 586.0F + offset, badgeWidth, 98.0F + badgeX + 6.0F, 600.076F + offset, timer, progress);
    }

    static float targetWidth(List<Cooldowns$CooldownRow> rows) {
        float target = 193.0F;

        for (Cooldowns$CooldownRow row : rows) {
            float required = 32.632F + TextRender.width(row.name(), 13.224F) + 9.0F + badgeWidth(row.timer()) + 10.0F;
            target = Math.max(target, required);
        }

        return Math.min(330.0F, target);
    }

    static float badgeWidth(String timer) {
        return Math.max(53.0F, TextRender.width(timer, 12.206F) + 29.0F);
    }

    static float remainingSeconds(Identifier group, float progress) {
        long now = System.currentTimeMillis();
        float[] record = TRACK.get(group);
        if (record != null && !(progress > record[0])) {
            float delta = record[0] - progress;
            if (delta >= 0.02F) {
                float elapsed = ((float)now - record[1]) / 1000.0F;
                record[2] = elapsed / delta;
            }

            return record[2] * progress;
        } else {
            TRACK.put(group, new float[]{progress, (float)now, 0.0F});
            return 0.0F;
        }
    }

    static List<ItemStack> active() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            cachedPlayerAge = Integer.MIN_VALUE;
            cachedActive = List.of();
            return cachedActive;
        }

        if (cachedPlayerAge == player.age) {
            return cachedActive;
        }

        cachedPlayerAge = player.age;
        List<ItemStack> list = new ArrayList<>();
        ItemCooldownManager manager = player.getItemCooldownManager();
        Set<Identifier> seen = new HashSet<>();

        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty() && manager.isCoolingDown(stack) && seen.add(manager.getGroup(stack))) {
                list.add(stack);
            }
        }

        cachedActive = list;
        return cachedActive;
    }
}
