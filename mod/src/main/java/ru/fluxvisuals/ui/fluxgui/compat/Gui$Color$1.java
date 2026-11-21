package ru.fluxvisuals.ui.fluxgui.compat;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2fc;

public class Gui$Color$1 implements SimpleGuiElementRenderState {
    final int val$count;
    final Matrix3x2fc val$m;
    final float[] val$fxy;
    final int[] val$fc;
    final RenderPipeline val$pl;
    final ScreenRect val$scissor;
    final ScreenRect val$bnds;

    Gui$Color$1(
        Gui$Color this$0, int param2, Matrix3x2fc nullx, float[] nullxx, int[] nullxxx, RenderPipeline nullxxxx, ScreenRect nullxxxxx, ScreenRect nullxxxxxx
    ) {
        this.val$count = param2;
        this.val$m = nullx;
        this.val$fxy = nullxx;
        this.val$fc = nullxxx;
        this.val$pl = nullxxxx;
        this.val$scissor = nullxxxxx;
        this.val$bnds = nullxxxxxx;
    }

    @Override
    public void setupVertices(VertexConsumer vertices) {
        for (int i = 0; i < this.val$count; i++) {
            vertices.vertex(this.val$m, this.val$fxy[i * 2], this.val$fxy[i * 2 + 1]).color(this.val$fc[i]);
        }
    }

    @Override
    public RenderPipeline comp_4055() {
        return this.val$pl;
    }

    @Override
    public TextureSetup comp_4056() {
        return TextureSetup.empty();
    }

    @Override
    public ScreenRect comp_4069() {
        return this.val$scissor;
    }

    @Override
    public ScreenRect comp_4274() {
        return this.val$bnds;
    }
}
