package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import ru.fluxvisuals.ui.fluxgui.client.gui.render.IconRender;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.RoundedRenderer;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.TextRender;
import ru.fluxvisuals.ui.fluxgui.module.Module;
import ru.fluxvisuals.ui.fluxgui.module.ModuleManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.util.math.MathHelper;

public final class Hotkeys implements HudElement {
    public static final Hotkeys INSTANCE = new Hotkeys();
    private static final float X = 179.0F;
    private static final float Y = 322.0F;
    private static final float MARK_X = 167.5F;
    private static final float MARK_Y = 14.0F;
    private static final float ICON_X = 9.25F;
    private static final float ICON_Y = 54.25F;
    private static final float ROW_X = 30.886F;
    private static final float BADGE_WIDTH = 33.0F;
    private static final float BADGE_ICON_Y = 54.0F;
    private static final float BADGE_ICON_SIZE = 13.0F;
    private static final float KEY_BASELINE = 65.0F;
    private static int cachedPlayerAge = Integer.MIN_VALUE;
    private static List<Module> cachedBound = List.of();
    private static float currentWidth = 193.0F;

    private Hotkeys() {
    }

    @Override
    public String id() {
        return "hotkeys";
    }

    @Override
    public float x() {
        return 179.0F;
    }

    @Override
    public float y() {
        return 322.0F;
    }

    @Override
    public float width() {
        return currentWidth;
    }

    @Override
    public float height() {
        return Hud.listHeight(Math.max(1, bound().size()));
    }

    @Override
    public boolean hasContent() {
        return !bound().isEmpty();
    }

    @Override
    public void render(DrawContext context, Hud hud) {
        List<Module> modules = bound();
        if (modules.isEmpty()) {
            return;
        }

        float targetWidth = targetWidth(modules);
        currentWidth = MathHelper.lerp(0.12F, currentWidth, targetWidth);
        if (Math.abs(currentWidth - targetWidth) < 0.05F) {
            currentWidth = targetWidth;
        }

        hud.listPanel(context, 179.0F, 322.0F, currentWidth, Hud.listHeight(modules.size()), "Hotkeys", "keyboard", currentWidth - 25.5F, 14.0F);
        for (int index = 0; index < modules.size(); index++) {
            Module module = modules.get(index);
            String key = Type.KEYSYM.createFromCode(module.getKey()).getLocalizedText().getString();
            row(context, index, module.getName(), key);
        }
    }

    private static void row(DrawContext context, int index, String name, String key) {
        float offset = index * 27.0F;
        IconRender.draw(context, "command_small", 188.25F, 376.25F + offset, IconRender.getDesignSize("command_small"), Hud.animatedColor(Hud.accent()));
        TextRender.draw(context, name, 209.886F, 387.583F + offset, 13.224F, Hud.animatedColor(-2829100));
        float badgeWidth = Math.max(33.0F, TextRender.width(key, 12.206F) + 23.0F);
        float badgeX = currentWidth - badgeWidth - 10.0F;
        RoundedRenderer.drawRoundedRect(context, 179.0F + badgeX, 373.0F + offset, badgeWidth, 19.0F, 3.0F, Hud.animatedColor(452984831));
        IconRender.draw(context, "keyboard", 179.0F + badgeX + 3.0F, 376.0F + offset, 13.0F, Hud.animatedColor(-2133140774));
        TextRender.draw(
            context, key, 179.0F + badgeX + badgeWidth - TextRender.width(key, 12.206F) - 5.0F, 387.0F + offset, 12.206F, Hud.animatedColor(-2133140774)
        );
    }

    private static float targetWidth(List<Module> modules) {
        float target = 193.0F;

        for (Module module : modules) {
            String key = Type.KEYSYM.createFromCode(module.getKey()).getLocalizedText().getString();
            float badge = Math.max(33.0F, TextRender.width(key, 12.206F) + 23.0F);
            target = Math.max(target, 30.886F + TextRender.width(module.getName(), 13.224F) + 10.0F + badge + 10.0F);
        }

        return Math.min(330.0F, target);
    }

    private static List<Module> bound() {
        int age = MinecraftClient.getInstance().player == null ? (int)(System.currentTimeMillis() / 50L) : MinecraftClient.getInstance().player.age;
        if (cachedPlayerAge == age) {
            return cachedBound;
        }

        cachedPlayerAge = age;
        List<Module> list = new ArrayList<>();

        for (Module module : ModuleManager.INSTANCE.getModules()) {
            if (module.isEnabled() && module.getKey() != -1 && module.getKey() != 0) {
                list.add(module);
            }
        }

        cachedBound = list;
        return cachedBound;
    }
}
