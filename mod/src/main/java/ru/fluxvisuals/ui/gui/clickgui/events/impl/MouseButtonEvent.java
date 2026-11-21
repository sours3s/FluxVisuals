package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.Event;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ButtonAction;


public final class MouseButtonEvent implements Event {
    public final ButtonAction action;
    public final int button;

    public MouseButtonEvent(ButtonAction buttonAction, int i) {
        this.action = buttonAction;
        this.button = i;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "action=" + this.action + ", " + "button=" + this.button + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.action, this.button);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MouseButtonEvent)) return false;
        MouseButtonEvent o = (MouseButtonEvent) obj;
        return java.util.Objects.equals(this.action, o.action) && java.util.Objects.equals(this.button, o.button);
    }
public ButtonAction action() {
        return this.action;
    }

    public int button() {
        return this.button;
    }
}
