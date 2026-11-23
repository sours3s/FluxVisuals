package ru.fluxvisuals.api.render.system.sys2d;

import ru.fluxvisuals.api.render.system.TextureUse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import ru.fluxvisuals.Client;

public class TextureAtlas {

    public final int atlasWidth, atlasHeight;
    private final int glId;
    private int currentX = 0, currentY = 0, rowHeight = 0;
    private final Map<String, UV> uvMap = new HashMap<>();

    public static class UV {
        public final float u0, v0, u1, v1;
        public final float width, height;
        public UV(float u0, float v0, float u1, float v1, float width, float height) {
            this.u0 = u0; this.v0 = v0; this.u1 = u1; this.v1 = v1;
            this.width = width; this.height = height;
        }
    }

    public TextureAtlas(int width, int height) {
        this.atlasWidth = width;
        this.atlasHeight = height;
        glId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, atlasWidth, atlasHeight, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    public int getGlId() {
        return glId;
    }

    private int getTextureId(AbstractTexture tex) {
        if (tex == null) return 0;
        try {
            GlTextureView view = (GlTextureView) tex.getGlTextureView();
            if (view != null) {
                GlTexture glTex = (GlTexture) view.texture();
                if (glTex != null) {
                    return glTex.getGlId();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public UV addTextureFromImage(String name, Identifier identifier) {
        AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(identifier);
        int id = getTextureId(tex);
        if (id == 0) return null;
        return addTextureFromGL(name, id);
    }

    public UV getOrCreate(String name, Identifier identifier) {
        if (!has(name)) {
            try {
                AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(identifier);
                int id = getTextureId(tex);
                if (id == 0) {
                    Client.RENDERER.getCrenderSystem().addPrepare(identifier);
                    return null;
                }
                return addTextureFromGL(name, id);
            } catch (Exception e) {
                Client.RENDERER.getCrenderSystem().addPrepare(identifier);
                return null;
            }
        }
        return getUV(name);
    }

    public UV add(TextureUse texUse, Identifier identifier) {
        AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(identifier);
        int id = getTextureId(tex);
        if (id == 0) return null;
        return addTextureFromGL(texUse.name().toLowerCase(), id);
    }

    public UV addTextureFromGL(String name, int srcGlId) {
        int[] size = getSize(srcGlId);
        int width = size[0];
        int height = size[1];

        if (currentX + width > atlasWidth) {
            currentX = 0;
            currentY += rowHeight;
            rowHeight = 0;
        }

        if (currentY + height > atlasHeight) {
            throw new RuntimeException("Atlas overflow!");
        }

        rowHeight = Math.max(rowHeight, height);
        int readFbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
        GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, srcGlId, 0);

        int drawFbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, glId, 0);

        if (GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE ||
                GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            cleanupFbos(readFbo, drawFbo);
            throw new RuntimeException("FBO incomplete");
        }

        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL30.glBlitFramebuffer(0, 0, width, height, currentX, currentY, currentX + width, currentY + height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

        cleanupFbos(readFbo, drawFbo);

        float u0 = (float) currentX / atlasWidth;
        float v0 = (float) currentY / atlasHeight;
        float u1 = (float) width / atlasWidth;
        float v1 = (float) height / atlasHeight;
        currentX += width;

        UV uv = new UV(u0, v0, u1, v1, width, height);
        uvMap.put(name, uv);
        return uv;
    }

    public void updateTextureFromGL(String name, int srcGlId) {
        UV uv = uvMap.get(name);
        if (uv == null) return;

        int destX = Math.round(uv.u0 * atlasWidth);
        int destY = Math.round(uv.v0 * atlasHeight);
        int destW = Math.round(uv.u1 * atlasWidth);
        int destH = Math.round(uv.v1 * atlasHeight);

        int[] srcSize = getSize(srcGlId);
        int srcW = srcSize[0];
        int srcH = srcSize[1];

        int blitW = Math.min(srcW, destW);
        int blitH = Math.min(srcH, destH);

        int readFbo = GL30.glGenFramebuffers();
        int drawFbo = GL30.glGenFramebuffers();

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
        GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, srcGlId, 0);

        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, glId, 0);

        if (GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE ||
                GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            cleanupFbos(readFbo, drawFbo);
            return;
        }

        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);

        GL30.glBlitFramebuffer(0, 0, blitW, blitH, destX, destY, destX + blitW, destY + blitH, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

        cleanupFbos(readFbo, drawFbo);
    }

    private void cleanupFbos(int readFbo, int drawFbo) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
        GL30.glDeleteFramebuffers(readFbo);
        GL30.glDeleteFramebuffers(drawFbo);
    }

    public int[] getSize(int texId) {
        int target = GL11.GL_TEXTURE_2D;
        int prev = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(target, texId);
        int level = 0;
        int width  = GL11.glGetTexLevelParameteri(target, level, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(target, level, GL11.GL_TEXTURE_HEIGHT);
        GL11.glBindTexture(target, prev);
        return new int[]{ width, height };
    }

    public boolean has(String name) { return uvMap.containsKey(name); }
    public UV getUV(String name) { return uvMap.get(name); }
}