package ru.fluxvisuals.ui.gui.clickgui.config;
import ru.fluxvisuals.ui.gui.clickgui.util.ClientHandler;

import java.util.ArrayList;
import java.util.List;

public class HandlerRepository {
    public final List<ClientHandler> listeners = new ArrayList();

    public HandlerRepository() {
        setup();
    }

    public void setup() {
    }

    public void registerHandlers(ClientHandler... clientHandlers) {
        this.listeners.addAll(List.of(clientHandlers));
    }

    public List<ClientHandler> getListeners() {
        return this.listeners;
    }

}
