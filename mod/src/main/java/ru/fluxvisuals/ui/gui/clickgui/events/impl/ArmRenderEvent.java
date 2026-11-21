package ru.fluxvisuals.ui.gui.clickgui.events.impl;
import ru.fluxvisuals.ui.gui.clickgui.events.api.CancellableEvent;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

public class ArmRenderEvent extends CancellableEvent {
    public final Arm arm;
    public final MatrixStack matrixStack;
    public final float swingProgress;

    public ArmRenderEvent(Arm arm, MatrixStack matrixStack, float f) {
        this.arm = arm;
        this.matrixStack = matrixStack;
        this.swingProgress = f;
    }

    public Arm arm() {
        return this.arm;
    }

    public MatrixStack matrixStack() {
        return this.matrixStack;
    }

    public float swingProgress() {
        return this.swingProgress;
    }
}
