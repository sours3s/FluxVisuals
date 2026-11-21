package ru.fluxvisuals.ui.gui.clickgui.config;
import ru.fluxvisuals.ui.gui.clickgui.events.api.ClientListener;
import ru.fluxvisuals.ui.gui.clickgui.util.CoreTickHandler;
import ru.fluxvisuals.ui.gui.clickgui.util.FragmentShaderBuilder;
import ru.fluxvisuals.ui.gui.clickgui.util.KeybindHandler;

import java.util.ArrayList;
import java.util.List;

public class ListenerRepository {
    public final List<ClientListener> listeners = new ArrayList();

    public ListenerRepository() {
        setup();
    }

    public void setup() {
        registerListeners(new KeybindHandler(), new FragmentShaderBuilder(), new CoreTickHandler());
    }

    public void registerListeners(ClientListener... clientListeners) {
        this.listeners.addAll(List.of(clientListeners));
    }

    public List<ClientListener> getListeners() {
        return this.listeners;
    }
}
