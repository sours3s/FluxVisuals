package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.net.CloudConfigService;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class ConfigAutoSaveScheduler {
    public static final long autoSaveDelayMs = 1200;
    public static final Object lock = new Object();
    public static ScheduledFuture<?> pendingSave;

    public ConfigAutoSaveScheduler() {
    }

    public static void scheduleAutoSave() {
        CloudConfigService cloudConfigService357 = FluxVisuals.INSTANCE.cloudConfigService();
        cloudConfigService357.activeConfig().ifPresent(configReference -> {
            if (FluxVisuals.INSTANCE.configManager().menuStateConfig().isAutoSaveDisabled(configReference.id())) {
                return;
            }
            synchronized (lock) {
                if (pendingSave != null && !pendingSave.isDone()) {
                    pendingSave.cancel(false);
                }
                pendingSave = FluxVisuals.INSTANCE.executor().schedule(() -> {
                    cloudConfigService357.saveConfig(configReference.id(), FluxVisuals.INSTANCE.moduleRepository(), FluxVisuals.INSTANCE.widgetStack()).thenRun(() -> {
                        FluxVisuals.LOGGER.info("Config auto-saved: {}", configReference.id());
                    }).exceptionally(th -> {
                        FluxVisuals.LOGGER.error("Failed to auto-save config", th);
                        return null;
                    });
                }, autoSaveDelayMs, TimeUnit.MILLISECONDS);
            }
        });
    }
}
