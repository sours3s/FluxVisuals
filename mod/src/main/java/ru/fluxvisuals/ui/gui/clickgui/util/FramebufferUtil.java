package ru.fluxvisuals.ui.gui.clickgui.util;

import java.util.function.Supplier;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class FramebufferUtil {
    public static Framebuffer ensureFramebuffer(Framebuffer framebuffer, int i, int i2, Supplier<Framebuffer> supplier) {
        if (framebuffer == null) {
            framebuffer = supplier.get();
            clearTransparent(framebuffer);
        }
        setLinearTextureFilter(framebuffer);
        return framebuffer;
    }

    public static void resizeIfNeeded(Framebuffer framebuffer, int i, int i2) {
        if (framebuffer.textureWidth == i && framebuffer.textureHeight == i2) {
            return;
        }
        framebuffer.resize(i, i2);
        clearTransparent(framebuffer);
        setLinearTextureFilter(framebuffer);
    }

    public static void clearTransparent(Framebuffer framebuffer) {
        int iGlGetInteger = GL11.glGetInteger(34016);
        int iGlGetInteger2 = GL11.glGetInteger(32873);
        int textureId = 0;
        Object attachment = framebuffer.getColorAttachment();
        if (attachment instanceof Number number) {
            textureId = number.intValue();
        } else if (attachment != null) {
            try {
                java.lang.reflect.Method getGlId = attachment.getClass().getMethod("getGlId");
                textureId = ((Number) getGlId.invoke(attachment)).intValue();
            } catch (Exception ignored) {}
        }
        GL11.glBindTexture(3553, textureId);
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glBindTexture(3553, iGlGetInteger2);
        GL30.glActiveTexture(iGlGetInteger);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(16384);
    }

    public static void setLinearTextureFilter(Framebuffer framebuffer) {
        int iGlGetInteger = GL11.glGetInteger(34016);
        int iGlGetInteger2 = GL11.glGetInteger(32873);
        int textureId = 0;
        Object attachment = framebuffer.getColorAttachment();
        if (attachment instanceof Number number) {
            textureId = number.intValue();
        } else if (attachment != null) {
            try {
                java.lang.reflect.Method getGlId = attachment.getClass().getMethod("getGlId");
                textureId = ((Number) getGlId.invoke(attachment)).intValue();
            } catch (Exception ignored) {}
        }
        GL11.glBindTexture(3553, textureId);
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glBindTexture(3553, iGlGetInteger2);
        GL30.glActiveTexture(iGlGetInteger);
    }

    public FramebufferUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
