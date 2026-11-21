package ru.fluxvisuals.ui.gui.events.api;


public interface EventCallback<T extends Event> {
    void call(T t);
}
