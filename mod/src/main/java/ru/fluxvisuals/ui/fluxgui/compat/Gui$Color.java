package ru.fluxvisuals.ui.fluxgui.compat;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Arrays;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Matrix3x2fc;

public final class Gui$Color {
    final Matrix3x2fc matrix;
    final RenderPipeline pipeline;
    float[] xy;
    int[] argb;
    int n = 0;
    float minX = Float.POSITIVE_INFINITY;
    float minY = Float.POSITIVE_INFINITY;
    float maxX = Float.NEGATIVE_INFINITY;
    float maxY = Float.NEGATIVE_INFINITY;
    int fanCount;
    float fanCx;
    float fanCy;
    float fanPx;
    float fanPy;
    int fanCc;
    int fanPc;
    int stripCount;
    float sax;
    float say;
    float sbx;
    float sby;
    int sac;
    int sbc;

    Gui$Color(Matrix3x2fc matrix, RenderPipeline pipeline, int expectedVertices) {
        this.matrix = matrix;
        this.pipeline = pipeline;
        int capacity = Math.max(4, expectedVertices);
        this.xy = new float[capacity * 2];
        this.argb = new int[capacity];
    }

    void ensure(int add) {
        if ((this.n + add) * 2 > this.xy.length) {
            this.xy = Arrays.copyOf(this.xy, Math.max(this.xy.length * 2, (this.n + add) * 2));
        }

        if (this.n + add > this.argb.length) {
            this.argb = Arrays.copyOf(this.argb, Math.max(this.argb.length * 2, this.n + add));
        }
    }

    public Gui$Color vertex(float x, float y, int packedArgb) {
        this.ensure(1);
        this.xy[this.n * 2] = x;
        this.xy[this.n * 2 + 1] = y;
        this.argb[this.n] = packedArgb;
        this.minX = Math.min(this.minX, x);
        this.minY = Math.min(this.minY, y);
        this.maxX = Math.max(this.maxX, x);
        this.maxY = Math.max(this.maxY, y);
        this.n++;
        return this;
    }

    public Gui$Color quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int c) {
        this.vertex(x0, y0, c).vertex(x1, y1, c).vertex(x2, y2, c).vertex(x3, y3, c);
        return this;
    }

    public Gui$Color tri(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2) {
        this.vertex(x0, y0, c0).vertex(x1, y1, c1).vertex(x2, y2, c2).vertex(x2, y2, c2);
        return this;
    }

    public void fanVertex(float x, float y, int c) {
        if (this.fanCount == 0) {
            this.fanCx = x;
            this.fanCy = y;
            this.fanCc = c;
        } else if (this.fanCount == 1) {
            this.fanPx = x;
            this.fanPy = y;
            this.fanPc = c;
        } else {
            this.tri(this.fanCx, this.fanCy, this.fanCc, this.fanPx, this.fanPy, this.fanPc, x, y, c);
            this.fanPx = x;
            this.fanPy = y;
            this.fanPc = c;
        }

        this.fanCount++;
    }

    public void fanVertex(float x, float y, float r, float g, float b, float a) {
        this.fanVertex(x, y, Gui.argb(r, g, b, a));
    }

    public void stripVertex(float x, float y, int c) {
        if (this.stripCount >= 2) {
            this.tri(this.sax, this.say, this.sac, this.sbx, this.sby, this.sbc, x, y, c);
        }

        this.sax = this.sbx;
        this.say = this.sby;
        this.sac = this.sbc;
        this.sbx = x;
        this.sby = y;
        this.sbc = c;
        this.stripCount++;
    }

    public void stripVertex(float x, float y, float r, float g, float b, float a) {
        this.stripVertex(x, y, Gui.argb(r, g, b, a));
    }

    public void submit(DrawContext ctx) {
        if (this.n >= 4) {
            int count = this.n & -4;
            float[] fxy = this.xy;
            int[] fc = this.argb;
            Matrix3x2fc m = this.matrix;
            RenderPipeline pl = this.pipeline;
            ScreenRect bnds = Gui.computeBounds(this.minX, this.minY, this.maxX, this.maxY, m);
            ScreenRect scissor = ctx.scissorStack.peekLast();
            ctx.state.addSimpleElement(new Gui$Color$1(this, count, m, fxy, fc, pl, scissor, bnds));
        }
    }
}
