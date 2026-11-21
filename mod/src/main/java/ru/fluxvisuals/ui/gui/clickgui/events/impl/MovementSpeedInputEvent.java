package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;

public class MovementSpeedInputEvent extends CancellableEvent {
    public final float movementForward;
    public final float movementSideways;

    public float getMovementForward() {
        return this.movementForward;
    }

    public float getMovementSideways() {
        return this.movementSideways;
    }

    public MovementSpeedInputEvent(float f, float f2) {
        this.movementForward = f;
        this.movementSideways = f2;
    }
}
