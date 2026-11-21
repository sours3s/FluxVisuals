package ru.fluxvisuals.event.impl.render;

import net.minecraft.client.gui.DrawContext;

public class DrawEvent {
    private final DrawContext graphics;

    public DrawEvent() { this.graphics = null; }
    public DrawEvent(DrawContext graphics) { this.graphics = graphics; }

    public DrawContext getGraphics() { return graphics; }
}
