package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;

public class CameraDistanceEvent extends CancellableEvent {
    public float pitch;
    public float distance;
    public ToggleAnimator frontAnim;
    public ToggleAnimator backAnim;

    public CameraDistanceEvent(float f) {
        this.pitch = f;
    }

    public float getPitch() {
        return this.pitch;
    }

    public float getDistance() {
        return this.distance;
    }

    public ToggleAnimator getFrontAnim() {
        return this.frontAnim;
    }

    public ToggleAnimator getBackAnim() {
        return this.backAnim;
    }

    public void setPitch(float f) {
        this.pitch = f;
    }

    public void setDistance(float f) {
        this.distance = f;
    }

    public void setFrontAnim(ToggleAnimator toggleAnimator) {
        this.frontAnim = toggleAnimator;
    }

    public void setBackAnim(ToggleAnimator toggleAnimator) {
        this.backAnim = toggleAnimator;
    }
}
