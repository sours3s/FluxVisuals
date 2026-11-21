package ru.fluxvisuals.ui.gui.events.impl;
import ru.fluxvisuals.ui.gui.events.api.Event;
import ru.fluxvisuals.ui.gui.util.type.KeyInputAction;


public final class KeyInputEvent implements Event {
    public final KeyInputAction action;
    public final int key;

    public KeyInputEvent(KeyInputAction keyPressState, int i) {
        this.action = keyPressState;
        this.key = i;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "action=" + this.action + ", " + "key=" + this.key + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.action, this.key);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof KeyInputEvent)) return false;
        KeyInputEvent o = (KeyInputEvent) obj;
        return java.util.Objects.equals(this.action, o.action) && java.util.Objects.equals(this.key, o.key);
    }
public KeyInputAction action() {
        return this.action;
    }

    public int key() {
        return this.key;
    }
}
