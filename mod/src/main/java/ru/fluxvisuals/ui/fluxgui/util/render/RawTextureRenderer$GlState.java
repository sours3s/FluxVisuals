package ru.fluxvisuals.ui.fluxgui.util.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class RawTextureRenderer$GlState {
    final int readFramebuffer = GL11.glGetInteger(36010);
    final int drawFramebuffer = GL11.glGetInteger(36006);
    final int currentProgram = GL11.glGetInteger(35725);
    final int currentVao = GL11.glGetInteger(34229);
    final int currentArrayBuffer = GL11.glGetInteger(34964);
    final int activeTexture = GL11.glGetInteger(34016);
    final int texture0;
    final boolean blend = GL11.glIsEnabled(3042);
    final boolean depth = GL11.glIsEnabled(2929);
    final boolean cull = GL11.glIsEnabled(2884);
    final boolean scissor = GL11.glIsEnabled(3089);
    final boolean depthMask = GL11.glGetBoolean(2930);
    final int blendSrcRgb = GL11.glGetInteger(32969);
    final int blendDstRgb = GL11.glGetInteger(32968);
    final int blendSrcAlpha = GL11.glGetInteger(32971);
    final int blendDstAlpha = GL11.glGetInteger(32970);
    final int[] scissorBox = new int[4];

    RawTextureRenderer$GlState() {
        GL11.glGetIntegerv(3088, this.scissorBox);
        GL13.glActiveTexture(33984);
        this.texture0 = GL11.glGetInteger(32873);
        GL13.glActiveTexture(this.activeTexture);
    }

    void restore() {
        GL30.glBindFramebuffer(36008, this.readFramebuffer);
        GL30.glBindFramebuffer(36009, this.drawFramebuffer);
        GL20.glUseProgram(this.currentProgram);
        GL30.glBindVertexArray(this.currentVao);
        GL15.glBindBuffer(34962, this.currentArrayBuffer);
        GL13.glActiveTexture(33984);
        GL11.glBindTexture(3553, this.texture0);
        GL13.glActiveTexture(this.activeTexture);
        setEnabled(3042, this.blend);
        setEnabled(2929, this.depth);
        setEnabled(2884, this.cull);
        setEnabled(3089, this.scissor);
        if (this.scissor) {
            GL11.glScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
        }

        GL11.glDepthMask(this.depthMask);
        GL20.glBlendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);
    }

    static void setEnabled(int capability, boolean enabled) {
        if (enabled) {
            GL11.glEnable(capability);
        } else {
            GL11.glDisable(capability);
        }
    }
}
