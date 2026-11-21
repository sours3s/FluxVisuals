package ru.fluxvisuals.ui.fluxgui.compat;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Arrays;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fc;

public final class Gui$Tex {
    final Matrix3x2fc matrix;
    final Identifier texture;
    final FilterMode filterMode;
    final RenderPipeline pipeline;
    float[] xy;
    float[] uv;
    int[] argb;
    int n = 0;
    float minX = Float.POSITIVE_INFINITY;
    float minY = Float.POSITIVE_INFINITY;
    float maxX = Float.NEGATIVE_INFINITY;
    float maxY = Float.NEGATIVE_INFINITY;
    int fanCount;
    float fcx;
    float fcy;
    float fcu;
    float fcv;
    float fpx;
    float fpy;
    float fpu;
    float fpv;
    int fcc;
    int fpc;
    int stripCount;
    float sax;
    float say;
    float sau;
    float sav;
    float sbx;
    float sby;
    float sbu;
    float sbv;
    int sac;
    int sbc;

    Gui$Tex(Matrix3x2fc matrix, Identifier texture, FilterMode filterMode, RenderPipeline pipeline, int expectedVertices) {
        this.matrix = matrix;
        this.texture = texture;
        this.filterMode = filterMode;
        this.pipeline = pipeline;
        int capacity = Math.max(4, expectedVertices);
        this.xy = new float[capacity * 2];
        this.uv = new float[capacity * 2];
        this.argb = new int[capacity];
    }

    void ensure(int add) {
        if ((this.n + add) * 2 > this.xy.length) {
            int s = Math.max(this.xy.length * 2, (this.n + add) * 2);
            this.xy = Arrays.copyOf(this.xy, s);
            this.uv = Arrays.copyOf(this.uv, s);
        }

        if (this.n + add > this.argb.length) {
            this.argb = Arrays.copyOf(this.argb, Math.max(this.argb.length * 2, this.n + add));
        }
    }

    public Gui$Tex vertex(float x, float y, float u, float v, int packedArgb) {
        this.ensure(1);
        this.xy[this.n * 2] = x;
        this.xy[this.n * 2 + 1] = y;
        this.uv[this.n * 2] = u;
        this.uv[this.n * 2 + 1] = v;
        this.argb[this.n] = packedArgb;
        this.minX = Math.min(this.minX, x);
        this.minY = Math.min(this.minY, y);
        this.maxX = Math.max(this.maxX, x);
        this.maxY = Math.max(this.maxY, y);
        this.n++;
        return this;
    }

    public void fanVertex(float x, float y, float u, float v, int c) {
        if (this.fanCount == 0) {
            this.fcx = x;
            this.fcy = y;
            this.fcu = u;
            this.fcv = v;
            this.fcc = c;
        } else if (this.fanCount == 1) {
            this.fpx = x;
            this.fpy = y;
            this.fpu = u;
            this.fpv = v;
            this.fpc = c;
        } else {
            this.vertex(this.fcx, this.fcy, this.fcu, this.fcv, this.fcc)
                .vertex(this.fpx, this.fpy, this.fpu, this.fpv, this.fpc)
                .vertex(x, y, u, v, c)
                .vertex(x, y, u, v, c);
            this.fpx = x;
            this.fpy = y;
            this.fpu = u;
            this.fpv = v;
            this.fpc = c;
        }

        this.fanCount++;
    }

    public void stripVertex(float x, float y, float u, float v, int c) {
        if (this.stripCount >= 2) {
            this.vertex(this.sax, this.say, this.sau, this.sav, this.sac)
                .vertex(this.sbx, this.sby, this.sbu, this.sbv, this.sbc)
                .vertex(x, y, u, v, c)
                .vertex(x, y, u, v, c);
        }

        this.sax = this.sbx;
        this.say = this.sby;
        this.sau = this.sbu;
        this.sav = this.sbv;
        this.sac = this.sbc;
        this.sbx = x;
        this.sby = y;
        this.sbu = u;
        this.sbv = v;
        this.sbc = c;
        this.stripCount++;
    }

    public Gui$Tex quad(
        float x0,
        float y0,
        float u0,
        float v0,
        float x1,
        float y1,
        float u1,
        float v1,
        float x2,
        float y2,
        float u2,
        float v2,
        float x3,
        float y3,
        float u3,
        float v3,
        int c
    ) {
        this.vertex(x0, y0, u0, v0, c).vertex(x1, y1, u1, v1, c).vertex(x2, y2, u2, v2, c).vertex(x3, y3, u3, v3, c);
        return this;
    }

    public void submit(DrawContext ctx) {
        if (this.n >= 4) {
            AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(this.texture);
            GpuTextureView view = tex.getGlTextureView();
            GpuSampler sampler = RenderSystem.getSamplerCache().get(this.filterMode);
            TextureSetup ts = TextureSetup.of(view, sampler);
            int count = this.n & -4;
            float[] fxy = this.xy;
            float[] fuv = this.uv;
            int[] fc = this.argb;
            Matrix3x2fc m = this.matrix;
            RenderPipeline pl = this.pipeline;
            ScreenRect bnds = Gui.computeBounds(this.minX, this.minY, this.maxX, this.maxY, m);
            ScreenRect scissor = ctx.scissorStack.peekLast();
            ctx.state.addSimpleElement(new Gui$Tex$1(this, count, m, fxy, fuv, fc, pl, ts, scissor, bnds));
        }
    }
}
