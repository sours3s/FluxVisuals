package ru.fluxvisuals.module.visual;

import ru.fluxvisuals.client.Flags;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;

/** Убирает оранжевый огонь с экрана. */
public class FireOverlayModule extends Module {
    public FireOverlayModule() {
        super("FireOverlay", "Убрать огонь с экрана", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        Flags.fireOverlayDisabled = true;
    }

    @Override
    protected void onDisable() {
        Flags.fireOverlayDisabled = false;
    }
}
