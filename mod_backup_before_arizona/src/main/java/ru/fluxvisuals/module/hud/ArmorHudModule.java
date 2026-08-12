package ru.fluxvisuals.module.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.hud.HudLayout;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.RenderUtils;
import ru.fluxvisuals.render.Theme;

/** Прочность брони (4 слота) в виде полосок. */
public class ArmorHudModule extends Module {
    private static final String[] SLOTS = {"Boots", "Legs", "Chest", "Helm"};
    private static final EquipmentSlot[] SLOT_TYPES = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private final Setting position = Setting.mode("Position", "Расположение", "Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right");

    public ArmorHudModule() {
        super("ArmorHUD", "Прочность брони", Category.HUD);
        addSetting(position);
    }

    @Override
    public void onRender2D(DrawContext g, float tickDelta) {
        var p = mc().player;
        if (p == null) return;
        boolean any = false;
        for (EquipmentSlot slot : SLOT_TYPES) if (!p.getEquippedStack(slot).isEmpty()) { any = true; break; }
        if (!any) return;

        float rowH = 11f;
        float w = 96f;
        float h = rowH * 4 + 4f;
        HudLayout.INSTANCE.push(g, HudLayout.fromIndex(position.getModeIndex()), w, h, pos -> {
            float x = pos[0], y = pos[1];
            RenderUtils.drawRoundedRect(g, x, y, w, h, 4f, Theme.bg());
            for (int i = 0; i < 4; i++) {
                float ry = y + 2f + i * rowH;
                ItemStack stack = p.getEquippedStack(SLOT_TYPES[i]);
                String label = SLOTS[i];
                if (!stack.isEmpty()) {
                    int maxDmg = stack.getMaxDamage();
                    if (maxDmg > 0) {
                        float pct = (maxDmg - stack.getDamage()) / (float) maxDmg;
                        float barX = x + 42f, barW = w - 52f;
                        RenderUtils.drawRoundedRect(g, barX, ry + 3f, barW, 4f, 2f, ColorUtils.withAlpha(0x000000, 110));
                        RenderUtils.drawRoundedRect(g, barX, ry + 3f, barW * pct, 4f, 2f, ColorUtils.healthColor(pct));
                        RenderUtils.textShadow(g, label, x + 5f, ry + 1f, Theme.textDim());
                        RenderUtils.text(g, (int) (pct * 100) + "%", barX + barW + 2f, ry + 1f, Theme.textDim());
                    } else {
                        RenderUtils.textShadow(g, label, x + 5f, ry + 1f, Theme.textDim());
                        RenderUtils.text(g, "∞", x + 42f, ry + 1f, Theme.textDim());
                    }
                } else {
                    RenderUtils.textShadow(g, label, x + 5f, ry + 1f, ColorUtils.withAlpha(Theme.textDim(), 90));
                }
            }
        });
    }
}
