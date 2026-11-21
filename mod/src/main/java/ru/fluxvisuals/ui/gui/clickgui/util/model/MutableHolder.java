package ru.fluxvisuals.ui.gui.clickgui.util.model;

public final class MutableHolder<T> {
    public T get;

    public void set(T t) {
        this.get = t;
    }

    public T get() {
        return this.get;
    }

    public MutableHolder(T t) {
        this.get = t;
    }
}
