package ru.fluxvisuals.ui.fluxgui.compat;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2fc;

public class Gui$Tex$1 implements SimpleGuiElementRenderState {
    final int val$count;
    final Matrix3x2fc val$m;
    final float[] val$fxy;
    final float[] val$fuv;
    final int[] val$fc;
    final RenderPipeline val$pl;
    final TextureSetup val$ts;
    final ScreenRect val$scissor;
    final ScreenRect val$bnds;

    Gui$Tex$1(
        Gui$Tex this$0,
        int param2,
        Matrix3x2fc nullx,
        float[] nullxx,
        float[] nullxxx,
        int[] nullxxxx,
        RenderPipeline nullxxxxx,
        TextureSetup nullxxxxxx,
        ScreenRect nullxxxxxxx,
        ScreenRect nullxxxxxxxx
    ) {
        this.val$count = param2;
        this.val$m = nullx;
        this.val$fxy = nullxx;
        this.val$fuv = nullxxx;
        this.val$fc = nullxxxx;
        this.val$pl = nullxxxxx;
        this.val$ts = nullxxxxxx;
        this.val$scissor = nullxxxxxxx;
        this.val$bnds = nullxxxxxxxx;
    }

    @Override
    public void setupVertices(VertexConsumer vertices) {
        for (int i = 0; i < this.val$count; i++) {
            vertices.vertex(this.val$m, this.val$fxy[i * 2], this.val$fxy[i * 2 + 1])
                .texture(this.val$fuv[i * 2], this.val$fuv[i * 2 + 1])
                .color(this.val$fc[i]);
        }
    }

    @Override
    public RenderPipeline comp_4055() {
        return this.val$pl;
    }

    @Override
    public TextureSetup comp_4056() {
        return this.val$ts;
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
