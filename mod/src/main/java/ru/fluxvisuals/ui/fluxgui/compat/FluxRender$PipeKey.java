package ru.fluxvisuals.ui.fluxgui.compat;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.Objects;

public final class FluxRender$PipeKey {
    boolean textured;
    DrawMode mode;
    boolean additive;
    boolean depthTest;
    boolean depthWrite;

    public FluxRender$PipeKey(boolean textured, DrawMode mode, boolean additive, boolean depthTest, boolean depthWrite) {
        this.textured = textured;
        this.mode = mode;
        this.additive = additive;
        this.depthTest = depthTest;
        this.depthWrite = depthWrite;
    }

    public boolean textured() {
        return this.textured;
    }

    public DrawMode mode() {
        return this.mode;
    }

    public boolean additive() {
        return this.additive;
    }

    public boolean depthTest() {
        return this.depthTest;
    }

    public boolean depthWrite() {
        return this.depthWrite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof FluxRender$PipeKey other)
                ? false
                : Objects.equals(this.textured, other.textured)
                    && Objects.equals(this.mode, other.mode)
                    && Objects.equals(this.additive, other.additive)
                    && Objects.equals(this.depthTest, other.depthTest)
                    && Objects.equals(this.depthWrite, other.depthWrite);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.textured, this.mode, this.additive, this.depthTest, this.depthWrite);
    }

    @Override
    public String toString() {
        return "FluxRender$PipeKey[textured="
            + this.textured
            + ", mode="
            + this.mode
            + ", additive="
            + this.additive
            + ", depthTest="
            + this.depthTest
            + ", depthWrite="
            + this.depthWrite
            + "]";
    }
}
