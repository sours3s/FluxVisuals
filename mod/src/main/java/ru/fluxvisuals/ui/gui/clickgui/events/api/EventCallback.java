package ru.fluxvisuals.ui.gui.clickgui.events.api;


public interface EventCallback<T extends Event> {
    void call(T t);
}
