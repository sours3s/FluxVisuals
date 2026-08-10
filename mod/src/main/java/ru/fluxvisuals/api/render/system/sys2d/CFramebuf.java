package ru.fluxvisuals.api.render.system.sys2d;

import static org.lwjgl.opengl.GL30C.*;

public class CFramebuf {
    public int fboID, textureID, depthRboID, width, height;
    public CFramebuf(int width, int height) {
        fboID = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);

        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureID, 0);

        depthRboID = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthRboID);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthRboID);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        this.width = width;
        this.height = height;
    }
    public void resize(int width, int height) {
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureID, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, depthRboID);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthRboID);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        this.width = width;
        this.height = height;
    }
    public void copyFrom(int id) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, id);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fboID);
        glBlitFramebuffer(
                0, 0, width, height,
                0, 0, width, height,
                GL_COLOR_BUFFER_BIT,
                GL_NEAREST
        );
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}
