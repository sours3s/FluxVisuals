package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import ru.fluxvisuals.ui.fluxgui.client.gui.render.TextRender;
import ru.fluxvisuals.ui.fluxgui.util.language.LanguageManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;

public final class Potions implements HudElement {
    public static final Potions INSTANCE = new Potions();
    private static final float X = 1539.0F;
    private static final float Y = 240.0F;
    private static final float MARK_X = 171.0F;
    private static final float MARK_Y = 15.5F;
    private static final float ROW_X = 33.017F;
    private static final float BADGE_WIDTH = 50.0F;
    private static final String TITLE = "Potions";
    private static final String PLACEHOLDER_TIMER = "1:09";
    private static final String INFINITE_TIMER = "-:--";
    private static final Map<RegistryEntry<StatusEffect>, Integer> PEAK = new HashMap<>();
    private static int cachedPlayerAge = Integer.MIN_VALUE;
    private static List<StatusEffectInstance> cachedEffects = List.of();
    private static float currentWidth = 193.0F;

    private Potions() {
    }

    @Override
    public String id() {
        return "potions";
    }

    @Override
    public float x() {
        return 1539.0F;
    }

    @Override
    public float y() {
        return 240.0F;
    }

    @Override
    public float width() {
        return currentWidth;
    }

    @Override
    public float height() {
        return Hud.listHeight(Math.max(1, effects().size()));
    }

    @Override
    public boolean hasContent() {
        return !effects().isEmpty();
    }

    @Override
    public void render(DrawContext context, Hud hud) {
        List<StatusEffectInstance> active = effects();
        float targetWidth = targetWidth(active);
        currentWidth = MathHelper.lerp(0.12F, currentWidth, targetWidth);
        if (Math.abs(currentWidth - targetWidth) < 0.05F) {
            currentWidth = targetWidth;
        }

        hud.listPanel(context, 1539.0F, 240.0F, currentWidth, Hud.listHeight(Math.max(1, active.size())), "Potions", "flask", currentWidth - 22.0F, 15.5F);
        if (active.isEmpty()) {
            String placeholder = LanguageManager.translateStatusEffectName(StatusEffects.STRENGTH, ru.fluxvisuals.ui.fluxgui.module.render.Hud.isEnglish())
                + " 2";
            row(context, 0, StatusEffects.STRENGTH, placeholder, "1:09", 1.0F);
        } else {
            for (int index = 0; index < active.size(); index++) {
                StatusEffectInstance instance = active.get(index);
                RegistryEntry<StatusEffect> type = instance.getEffectType();
                String name = LanguageManager.translateStatusEffectName(instance, ru.fluxvisuals.ui.fluxgui.module.render.Hud.isEnglish());
                if (instance.getAmplifier() > 0) {
                    name = name + " " + (instance.getAmplifier() + 1);
                }

                String timer;
                float progress;
                if (instance.isInfinite()) {
                    timer = "-:--";
                    progress = 1.0F;
                } else {
                    int duration = instance.getDuration();
                    int peak = Math.max(PEAK.getOrDefault(type, 0), duration);
                    PEAK.put(type, peak);
                    timer = Hud.formatSeconds(duration / 20.0F);
                    progress = peak <= 0 ? 0.0F : (float)duration / peak;
                }

                row(context, index, type, name, timer, progress);
            }
        }
    }

    private static void row(DrawContext context, int index, RegistryEntry<StatusEffect> type, String name, String timer, float progress) {
        float offset = index * 27.0F;
        Hud.effect(context, type, 1549.0F, 294.0F + offset, 13.0F);
        TextRender.draw(context, name, 1572.017F, 305.583F + offset, 13.224F, Hud.animatedColor(-2829100));
        float badgeWidth = badgeWidth(timer);
        float badgeX = currentWidth - badgeWidth - 10.0F;
        Hud.timerBadge(context, 1539.0F + badgeX, 291.0F + offset, badgeWidth, 1539.0F + badgeX + 5.0F, 305.076F + offset, timer, progress);
    }

    private static float targetWidth(List<StatusEffectInstance> effects) {
        float target = 193.0F;
        if (effects.isEmpty()) {
            return target;
        }

        for (StatusEffectInstance instance : effects) {
            String name = LanguageManager.translateStatusEffectName(instance, ru.fluxvisuals.ui.fluxgui.module.render.Hud.isEnglish());
            if (instance.getAmplifier() > 0) {
                name = name + " " + (instance.getAmplifier() + 1);
            }

            String timer = instance.isInfinite() ? "-:--" : Hud.formatSeconds(instance.getDuration() / 20.0F);
            float required = 33.017F + TextRender.width(name, 13.224F) + 9.0F + badgeWidth(timer) + 10.0F;
            target = Math.max(target, required);
        }

        return Math.min(330.0F, target);
    }

    private static float badgeWidth(String timer) {
        return Math.max(50.0F, TextRender.width(timer, 12.206F) + 29.0F);
    }

    private static List<StatusEffectInstance> effects() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            cachedPlayerAge = Integer.MIN_VALUE;
            cachedEffects = List.of();
            return cachedEffects;
        }

        if (cachedPlayerAge != player.age) {
            cachedPlayerAge = player.age;
            cachedEffects = new ArrayList<>(player.getStatusEffects());
        }

        return cachedEffects;
    }
}
