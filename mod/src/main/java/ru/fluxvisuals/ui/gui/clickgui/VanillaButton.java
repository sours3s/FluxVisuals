package ru.fluxvisuals.ui.gui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;

public class VanillaButton extends ButtonWidget {
    public VanillaButton(int x, int y, int width, int height, MutableText text, PressAction pressAction) {
        super(x, y, width, height, text, pressAction, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    public void drawIcon(DrawContext context, int x, int y, float tickDelta) {
        // No icon to draw
    }
}