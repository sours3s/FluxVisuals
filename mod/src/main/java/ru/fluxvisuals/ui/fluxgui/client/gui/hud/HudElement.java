package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import net.minecraft.client.gui.DrawContext;

public interface HudElement {
    String id();

    float x();

    float y();

    float width();

    float height();

    boolean hasContent();

    void render(DrawContext var1, Hud var2);
}
