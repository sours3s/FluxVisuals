package ru.fluxvisuals.ui.fluxgui.module.render;

import ru.fluxvisuals.ui.fluxgui.setting.MultiBooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.Perspective;

/** Донорский модуль анимаций, адаптированный к текущему менеджеру модулей. */
public final class Animations {
    public static final Animations INSTANCE = new Animations();
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public static final String MODE_TAB = "Таб";
    public static final String MODE_PERSPECTIVE = "Смена перспективы";
    public static final String MODE_INVENTORY = "Открытие инвентаря";
    public static final String MODE_SLOT = "Смена слотов";
    public static final String MODE_CHAT = "Анимация чата";
    public static final MultiBooleanSetting modes = new MultiBooleanSetting(
        "Режимы", "Таб", "Смена перспективы", "Открытие инвентаря", "Смена слотов", "Анимация чата"
    );
    private float tabProgress = 0.0F;
    private float invProgress = 0.0F;
    private float slotProgress = 0.0F;
    private float perspectiveDistance = 0.0F;
    private long lastTime = 0L;
    public boolean forceTabRender = false;

    private Animations() {
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean isModeEnabled(String key) {
        return modes.isEnabled(key);
    }

    public void updateAnimations() {
        long now = System.currentTimeMillis();
        if (this.lastTime == 0L) {
            this.lastTime = now;
        }

        float dt = (float)(now - this.lastTime) / 1000.0F;
        this.lastTime = now;
        if (dt > 0.1F) {
            dt = 0.1F;
        }

        if (this.mc.player != null && this.mc.options != null) {
            int targetSlot = this.mc.player.getInventory().getSelectedSlot();
            this.slotProgress = this.animateExp(this.slotProgress, targetSlot, 38.0F, dt);
            if (Math.abs(this.slotProgress - targetSlot) < 0.05F) {
                this.slotProgress = targetSlot;
            }

            boolean isTabPressed;
            this.tabProgress = this.animateExp(this.tabProgress, (isTabPressed = this.mc.options.playerListKey.isPressed()) ? 1.0F : 0.0F, 25.0F, dt);
            this.forceTabRender = this.isModeEnabled("Таб") && (isTabPressed || this.tabProgress > 0.01F);
            boolean isInvOpen = this.mc.currentScreen instanceof InventoryScreen;
            this.invProgress = this.animateExp(this.invProgress, isInvOpen ? 1.0F : 0.0F, 30.0F, dt);
            Perspective currentPerspective = this.mc.options.getPerspective();
            boolean isThirdPerson = !currentPerspective.isFirstPerson();
            this.perspectiveDistance = this.animateExp(this.perspectiveDistance, isThirdPerson ? 4.0F : 0.0F, 15.0F, dt);
        }
    }

    public float getSmoothedSlot() {
        return this.isModeEnabled("Смена слотов") ? this.slotProgress : (this.mc.player != null ? this.mc.player.getInventory().getSelectedSlot() : 0);
    }

    public float getTabProgress() {
        return this.isModeEnabled("Таб") ? this.tabProgress : 1.0F;
    }

    public float getInventoryProgress() {
        return this.isModeEnabled("Открытие инвентаря") ? this.invProgress : 1.0F;
    }

    public float getPerspectiveDistance() {
        return this.isModeEnabled("Смена перспективы") ? this.perspectiveDistance : 4.0F;
    }

    public void onEnable() {
        this.lastTime = System.currentTimeMillis();
        if (this.mc.player != null) {
            this.slotProgress = this.mc.player.getInventory().getSelectedSlot();
        }

        this.tabProgress = 0.0F;
        this.invProgress = 0.0F;
        this.perspectiveDistance = this.mc.options != null && !this.mc.options.getPerspective().isFirstPerson() ? 4.0F : 0.0F;
    }

    private float animateExp(float current, float target, float speed, float dt) {
        float factor = 1.0F - (float)Math.exp(-speed * dt);
        return current + (target - current) * factor;
    }
}
