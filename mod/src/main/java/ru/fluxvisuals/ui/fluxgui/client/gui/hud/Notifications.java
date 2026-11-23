package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import ru.fluxvisuals.ui.fluxgui.client.gui.render.IconRender;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.RoundedRenderer;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.TextRender;
import ru.fluxvisuals.ui.fluxgui.module.Module;
import ru.fluxvisuals.ui.fluxgui.module.ModuleManager;
import ru.fluxvisuals.ui.fluxgui.util.language.LanguageManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;

public final class Notifications implements HudElement {
    public static final Notifications INSTANCE = new Notifications();
    static final float X = 791.0F;
    static final float Y = 84.0F;
    static final float HEIGHT = 37.0F;
    static final float GAP_Y = 7.0F;
    static final float ICON_X = 11.25F;
    static final float ICON_Y = 12.25F;
    static final float SEPARATOR_X = 33.0F;
    static final float SEPARATOR_Y = 12.5F;
    static final float SEPARATOR_HEIGHT = 12.0F;
    static final float BASELINE = 24.08F;
    static final float PREFIX_X = 44.603F;
    static final float TRACK_Y = 12.5F;
    static final float TRACK_WIDTH = 25.0F;
    static final float TRACK_HEIGHT = 12.0F;
    static final float TRACK_RADIUS = 6.0F;
    static final float KNOB_WIDTH = 15.0F;
    static final float KNOB_HEIGHT = 8.0F;
    static final float KNOB_RADIUS = 4.0F;
    static final float KNOB_INSET = 2.0F;
    static final float KNOB_TRAVEL = 6.0F;
    static final float TEXT_GAP = 4.5F;
    static final float MIN_WIDTH = 145.0F;
    static final float RIGHT_PADDING = 12.0F;
    static final float TAIL_GAP = 12.0F;
    static final long HOLD_MILLIS = 2600L;
    static final long FADE_MILLIS = 400L;
    static final int MAX_VISIBLE = 3;
    static final CopyOnWriteArrayList<Notifications$Entry> ENTRIES = new CopyOnWriteArrayList<>();
    static final Map<Module, Boolean> KNOWN = new IdentityHashMap<>();
    static final Map<RegistryEntry<StatusEffect>, Boolean> WARNED_EFFECTS = new HashMap<>();

    Notifications() {
    }

    @Override
    public String id() {
        return "notifications";
    }

    @Override
    public float x() {
        return 791.0F;
    }

    @Override
    public float y() {
        return Y;
    }

    @Override
    public float width() {
        float width = 145.0F;
        int count = 0;

        for (Notifications$Entry entry : ENTRIES) {
            width = Math.max(width, entryWidth(entry));
            if (++count >= 3) {
                break;
            }
        }

        return ENTRIES.isEmpty() ? entryWidth(Notifications$Entry.placeholder()) : width;
    }

    @Override
    public float height() {
        int count = Math.max(1, Math.min(3, ENTRIES.size()));
        return 37.0F + (count - 1) * 44.0F;
    }

    @Override
    public boolean hasContent() {
        if (!ru.fluxvisuals.ui.fluxgui.module.render.Hud.notifications.getValue()) {
            return false;
        }

        poll();
        prune();
        return !ENTRIES.isEmpty();
    }

    @Override
    public void render(DrawContext context, Hud hud) {
        if (hud.isEditing()) {
            poll();
            prune();
        }

        if (ENTRIES.isEmpty()) {
            if (hud.isEditing()) {
                renderEntry(context, hud, Notifications$Entry.placeholder(), 0.0F, 1.0F);
            }
        } else {
            int index = 0;

            for (Notifications$Entry entry : ENTRIES) {
                if (index >= 3) {
                    break;
                }

                float targetY = index * 44.0F;
                entry.currentY = MathHelper.lerp(0.16F, entry.currentY, targetY);
                renderEntry(context, hud, entry, entry.currentY, opacity(entry));
                index++;
            }
        }
    }

    static void renderEntry(DrawContext context, Hud hud, Notifications$Entry entry, float yOffset, float opacity) {
        if (!(opacity <= 0.0F)) {
            float y = Y + yOffset;
            float width = entryWidth(entry);
            hud.panel(context, 791.0F, y, width, 37.0F, 10.0F, opacity);
            int visualAccent = visualAccent();
            String primaryIcon = Hud.primaryIcon("command_small");
            IconRender.draw(
                context, primaryIcon, 802.25F, y + 12.25F, IconRender.getDesignSize(primaryIcon) * Hud.primaryIconScale(), Hud.fade(visualAccent, opacity)
            );
            Hud.separator(context, 824.0F, y + 12.5F, 12.0F, Hud.fade(452984831, opacity));
            if (entry.message != null) {
                TextRender.draw(context, entry.message, 835.603F, y + 24.08F, 14.241F, Hud.fade(Hud.styledColor(-2434342), opacity));
            } else {
                String prefix = ru.fluxvisuals.ui.fluxgui.module.render.Hud.text("Модуль", "Module");
                String suffix = ru.fluxvisuals.ui.fluxgui.module.render.Hud.text("переключён", "was switched to");
                float pen = 835.603F;
                TextRender.draw(context, prefix, pen, y + 24.08F, 14.241F, Hud.fade(Hud.styledColor(-2434342), opacity));
                pen += TextRender.width(prefix, 14.241F) + 4.5F;
                TextRender.draw(context, entry.moduleName, pen, y + 24.08F, 14.241F, Hud.fade(Hud.styledColor(-1), opacity));
                pen += TextRender.width(entry.moduleName, 14.241F) + 4.5F;
                TextRender.draw(context, suffix, pen, y + 24.08F, 14.241F, Hud.fade(Hud.styledColor(-2434342), opacity));
                pen += TextRender.width(suffix, 14.241F);
                float tailSeparatorX = pen + 12.0F;
                Hud.separator(context, tailSeparatorX, y + 12.5F, 12.0F, Hud.fade(452984831, opacity));
                entry.toggleAnimation = MathHelper.lerp(0.1F, entry.toggleAnimation, entry.enabled ? 1.0F : 0.0F);
                float toggleEase = smooth(entry.toggleAnimation);
                float trackX = tailSeparatorX + 2.0F + 10.0F;
                float trackY = y + 12.5F;
                RoundedRenderer.drawRoundedRect(context, trackX, trackY, 25.0F, 12.0F, 6.0F, Hud.fade(lerpColor(452984831, visualAccent, toggleEase), opacity));
                RoundedRenderer.drawRoundedRect(context, trackX + 2.0F + 6.0F * toggleEase, trackY + 2.0F, 15.0F, 8.0F, 4.0F, Hud.fade(-1, opacity));
            }
        }
    }

    public static void post(String moduleName, boolean enabled) {
        if (ru.fluxvisuals.ui.fluxgui.module.render.Hud.notifications.getValue()) {
            addEntry(Notifications$Entry.module(moduleName, enabled));
        }
    }

    public static void post(Module module, boolean enabled) {
        KNOWN.put(module, enabled);
        post(module.getName(), enabled);
    }

    public static void postMessage(String message) {
        if (ru.fluxvisuals.ui.fluxgui.module.render.Hud.notifications.getValue() && message != null && !message.isBlank()) {
            addEntry(Notifications$Entry.message(message));
        }
    }

    public static void postLocalized(String russian, String english) {
        postMessage(ru.fluxvisuals.ui.fluxgui.module.render.Hud.text(russian, english));
    }

    static void addEntry(Notifications$Entry entry) {
        ENTRIES.add(0, entry);

        while (ENTRIES.size() > 3) {
            ENTRIES.remove(ENTRIES.size() - 1);
        }
    }

    static void poll() {
        for (Module module : ModuleManager.INSTANCE.getModules()) {
            boolean enabled = module.isEnabled();
            Boolean known = KNOWN.put(module, enabled);
            if (known != null && known != enabled) {
                post(module.getName(), enabled);
            }
        }

        pollEffects();
    }

    static void pollEffects() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            WARNED_EFFECTS.clear();
        } else {
            Set<RegistryEntry<StatusEffect>> active = new HashSet<>();

            for (StatusEffectInstance instance : client.player.getStatusEffects()) {
                RegistryEntry<StatusEffect> type = instance.getEffectType();
                active.add(type);
                if (!instance.isInfinite()) {
                    int seconds = instance.getDuration() / 20;
                    if (seconds > 10) {
                        WARNED_EFFECTS.put(type, false);
                    } else if (seconds > 0 && !WARNED_EFFECTS.getOrDefault(type, false)) {
                        WARNED_EFFECTS.put(type, true);
                        String effectName = LanguageManager.translateStatusEffectName(type, ru.fluxvisuals.ui.fluxgui.module.render.Hud.isEnglish());
                        postMessage(
                            ru.fluxvisuals.ui.fluxgui.module.render.Hud.isEnglish()
                                ? effectName + " ends in 10 sec."
                                : "До конца " + effectName + " осталось 10 сек."
                        );
                    }
                }
            }

            WARNED_EFFECTS.keySet().removeIf(typex -> !active.contains(typex));
        }
    }

    static void prune() {
        ENTRIES.removeIf(entry -> elapsed(entry) > 3000L);
    }

    static long elapsed(Notifications$Entry entry) {
        return System.currentTimeMillis() - entry.shownAt;
    }

    static float opacity(Notifications$Entry entry) {
        long elapsed = elapsed(entry);
        return elapsed <= 2600L ? 1.0F : MathHelper.clamp(1.0F - (float)(elapsed - 2600L) / 400.0F, 0.0F, 1.0F);
    }

    static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    static int lerpColor(int from, int to, float delta) {
        int a = Math.round(MathHelper.lerp(delta, from >>> 24 & 0xFF, to >>> 24 & 0xFF));
        int r = Math.round(MathHelper.lerp(delta, from >>> 16 & 0xFF, to >>> 16 & 0xFF));
        int g = Math.round(MathHelper.lerp(delta, from >>> 8 & 0xFF, to >>> 8 & 0xFF));
        int b = Math.round(MathHelper.lerp(delta, from & 0xFF, to & 0xFF));
        return a << 24 | r << 16 | g << 8 | b;
    }

    static int visualAccent() {
        return Hud.accent();
    }

    static float entryWidth(Notifications$Entry entry) {
        if (entry.message != null) {
            return Math.max(145.0F, 44.603F + TextRender.width(entry.message, 14.241F) + 12.0F);
        }

        String prefix = ru.fluxvisuals.ui.fluxgui.module.render.Hud.text("Модуль", "Module");
        String suffix = ru.fluxvisuals.ui.fluxgui.module.render.Hud.text("переключён", "was switched to");
        float textWidth = TextRender.width(prefix, 14.241F) + TextRender.width(entry.moduleName, 14.241F) + TextRender.width(suffix, 14.241F) + 9.0F;
        float width = 44.603F + textWidth + 12.0F + 2.0F + 10.0F + 25.0F + 12.0F;
        return Math.max(145.0F, width);
    }
}
