package ru.fluxvisuals.ui.fluxgui.cosmetics;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Заглушка 3D-превью косметики GUI FluxVisuals. */
public final class BedrockModel {
    public float minX = 0.0F;
    public float maxX = 1.0F;
    public float minY = 0.0F;
    public float maxY = 1.0F;
    public float minZ = 0.0F;
    public float maxZ = 1.0F;

    public boolean renderGui(DrawContext context, Identifier texture, MatrixStack matrices, Object animation, float time) {
        return false;
    }
}
