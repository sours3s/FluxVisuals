package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.events.api.ClientListener;
import ru.fluxvisuals.ui.gui.clickgui.util.type.EventPriority;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.PlayerTickEvent;

public class CoreTickHandler implements ClientListener {
    public CoreTickHandler() {
        FluxVisuals.INSTANCE.eventDispatcher().register(PlayerTickEvent.class, playerTickEvent -> {
            if (Mc.INSTANCE.isWorldLoaded() && playerTickEvent.isPre()) {
                ServerUtil.tick();
            }
        }, EventPriority.LOW);
    }
}