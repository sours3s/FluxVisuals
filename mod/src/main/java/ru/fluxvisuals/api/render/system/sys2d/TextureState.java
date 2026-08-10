package ru.fluxvisuals.api.render.system.sys2d;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;

public class TextureState {

    private final int[] boundTextures;
    private final int originalActiveTexture;
    private final int maxTextureUnits;

    public TextureState() {
        maxTextureUnits = getInteger(GL30.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
        boundTextures = new int[maxTextureUnits];
        originalActiveTexture = getInteger(GL13.GL_ACTIVE_TEXTURE);
        for (int i = 0; i < maxTextureUnits; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            boundTextures[i] = getInteger(GL11.GL_TEXTURE_BINDING_2D);
        }
        GL13.glActiveTexture(originalActiveTexture);
    }

    public void restore() {
        for (int i = 0; i < maxTextureUnits; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTextures[i]);
        }
        GL13.glActiveTexture(originalActiveTexture);
    }

    private int getInteger(int parameter) {
        IntBuffer buf = BufferUtils.createIntBuffer(1);
        GL11.glGetIntegerv(parameter, buf);
        return buf.get(0);
    }
}