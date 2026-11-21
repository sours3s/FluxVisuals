package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.type.InputType;
import ru.fluxvisuals.ui.gui.clickgui.util.type.KeyInputAction;


public final class KeyInput implements InputEvent {
    public final int keyCode;
    public final int scanCode;
    public final KeyInputAction keyAction;

    public final KeyModifiers modifiers;

    public KeyInput(int i, int i2, KeyInputAction keyInputAction, KeyModifiers keyModifiers) {
        this.keyCode = i;
        this.scanCode = i2;
        this.keyAction = keyInputAction;
        this.modifiers = keyModifiers;
    }

    @Override
    public InputType type() {
        return InputType.KEY;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "keyCode=" + this.keyCode + ", " + "scanCode=" + this.scanCode + ", " + "keyAction=" + this.keyAction + ", " + "modifiers=" + this.modifiers + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.keyCode, this.scanCode, this.keyAction, this.modifiers);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof KeyInput)) return false;
        KeyInput o = (KeyInput) obj;
        return java.util.Objects.equals(this.keyCode, o.keyCode) && java.util.Objects.equals(this.scanCode, o.scanCode) && java.util.Objects.equals(this.keyAction, o.keyAction) && java.util.Objects.equals(this.modifiers, o.modifiers);
    }
public int keyCode() {
        return this.keyCode;
    }

    public int scanCode() {
        return this.scanCode;
    }

    public KeyInputAction keyAction() {
        return this.keyAction;
    }

    public KeyModifiers modifiers() {
        return this.modifiers;
    }
}
