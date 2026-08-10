package ru.fluxvisuals.api.render.system.sys2d;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import ru.fluxvisuals.api.render.system.ClientPipelines;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class ClientLayer implements SimpleGuiElementRenderState {

    private int x0, y0, x1, y1;
    private int col1, col2;
    private final Matrix3x2f pose = new Matrix3x2f();

    public ClientLayer() {
    }

    @Override
    public void setupVertices(VertexConsumer vertices) {
        Matrix3x2f p = this.pose();

        Matrix4f matrix = new Matrix4f().set(
                p.m00, p.m01, 0.0f, 0.0f,
                p.m10, p.m11, 0.0f, 0.0f,
                0.0f,  0.0f,  1.0f, 0.0f,
                p.m20, p.m21, 0.0f, 1.0f
        );

        float x0 = (float)this.x0();
        float y0 = (float)this.y0();
        float x1 = (float)this.x1();
        float y1 = (float)this.y1();

        vertices.vertex(matrix, x0, y0, 0.0f).color(this.col1());
        vertices.vertex(matrix, x0, y1, 0.0f).color(this.col2());
        vertices.vertex(matrix, x1, y1, 0.0f).color(this.col2());
        vertices.vertex(matrix, x1, y0, 0.0f).color(this.col1());
    }

    public Matrix3x2f pose() {
        return this.pose;
    }

    @Override
    public RenderPipeline comp_4055() {
        return ClientPipelines.HUD.getRenderPipeline();
    }

    @Override
    public TextureSetup comp_4056() {
        return TextureSetup.empty();
    }

    @Override
    public ScreenRect comp_4274() {
        return new ScreenRect(this.x0(), this.y0(), this.x1() - this.x0(), this.y1() - this.y0());
    }

    @Override
    @Nullable
    public ScreenRect comp_4069() {
        return null;
    }

    public int x0() { return this.x0; }
    public int y0() { return this.y0; }
    public int x1() { return this.x1; }
    public int y1() { return this.y1; }
    public int col1() { return this.col1; }
    public int col2() { return this.col2; }

    public void x0(int x0) { this.x0 = x0; }
    public void y0(int y0) { this.y0 = y0; }
    public void x1(int x1) { this.x1 = x1; }
    public void y1(int y1) { this.y1 = y1; }
    public void col1(int col1) { this.col1 = col1; }
    public void col2(int col2) { this.col2 = col2; }
}