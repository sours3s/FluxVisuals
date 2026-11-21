package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.type.InputType;
import ru.fluxvisuals.ui.gui.clickgui.util.type.MouseButtonAction;


public final class MouseButtonInput implements InputEvent {
    public final int button;
    public final MouseButtonAction action;

    public final MouseModifiers mods;

    public MouseButtonInput(int i, MouseButtonAction mouseButtonAction, MouseModifiers mouseModifiers) {
        this.button = i;
        this.action = mouseButtonAction;
        this.mods = mouseModifiers;
    }

    @Override
    public InputType type() {
        return InputType.BUTTON;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "button=" + this.button + ", " + "action=" + this.action + ", " + "mods=" + this.mods + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.button, this.action, this.mods);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MouseButtonInput)) return false;
        MouseButtonInput o = (MouseButtonInput) obj;
        return java.util.Objects.equals(this.button, o.button) && java.util.Objects.equals(this.action, o.action) && java.util.Objects.equals(this.mods, o.mods);
    }
public int button() {
        return this.button;
    }

    public MouseButtonAction action() {
        return this.action;
    }

    public MouseModifiers mods() {
        return this.mods;
    }
}
