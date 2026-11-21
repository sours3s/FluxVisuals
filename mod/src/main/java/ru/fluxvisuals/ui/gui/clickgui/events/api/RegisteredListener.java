package ru.fluxvisuals.ui.gui.clickgui.events.api;
import ru.fluxvisuals.ui.gui.clickgui.util.type.EventPriority;


public final class RegisteredListener<T extends Event> {
    public final EventCallback<T> callback;

    public final EventPriority priority;

    public RegisteredListener(EventCallback<T> eventCallback, EventPriority eventPriority) {
        this.callback = eventCallback;
        this.priority = eventPriority;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "callback=" + this.callback + ", " + "priority=" + this.priority + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.callback, this.priority);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RegisteredListener)) return false;
        RegisteredListener o = (RegisteredListener) obj;
        return java.util.Objects.equals(this.callback, o.callback) && java.util.Objects.equals(this.priority, o.priority);
    }
public EventCallback<T> callback() {
        return this.callback;
    }

    public EventPriority priority() {
        return this.priority;
    }
}
