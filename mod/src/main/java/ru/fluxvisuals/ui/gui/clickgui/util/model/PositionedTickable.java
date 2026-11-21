package ru.fluxvisuals.ui.gui.clickgui.util.model;

import net.minecraft.util.math.Vec3d;

public interface PositionedTickable {
    Vec3d pos();

    void tick();
}
