package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

public final class Notifications$Entry {
    final String moduleName;
    final String message;
    final boolean enabled;
    final long shownAt;
    float currentY;
    float toggleAnimation;

    Notifications$Entry(String moduleName, String message, boolean enabled, boolean animateToggle) {
        this.moduleName = moduleName;
        this.message = message;
        this.enabled = enabled;
        this.shownAt = System.currentTimeMillis();
        this.currentY = -8.0F;
        this.toggleAnimation = animateToggle ? (enabled ? 0.0F : 1.0F) : (enabled ? 1.0F : 0.0F);
    }

    static Notifications$Entry module(String name, boolean enabled) {
        return new Notifications$Entry(name == null ? "" : name, null, enabled, true);
    }

    static Notifications$Entry message(String message) {
        return new Notifications$Entry("", message, false, false);
    }

    static Notifications$Entry placeholder() {
        Notifications$Entry entry = message(
            ru.fluxvisuals.ui.fluxgui.module.render.Hud.text("Уведомления", "Notifications")
        );
        entry.currentY = 0.0F;
        return entry;
    }
}
