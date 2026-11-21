package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;

import net.minecraft.entity.effect.StatusEffect;

public class StatusEffectEvent extends CancellableEvent {
    public final StatusEffect effect;

    public StatusEffect getEffect() {
        return this.effect;
    }

    public StatusEffectEvent(StatusEffect statusEffect) {
        this.effect = statusEffect;
    }
}
