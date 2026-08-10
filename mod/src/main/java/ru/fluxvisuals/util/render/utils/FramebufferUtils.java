package ru.fluxvisuals.util.render.utils;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.GlTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.GpuTexture;

public final class FramebufferUtils {
    public static int getColorTextureId(Framebuffer framebuffer) {
        if (framebuffer == null) {
            return 0;
        }
        Object attachment = framebuffer.getColorAttachment();

        if (attachment == null) {
            return 0;
        }
        if (attachment instanceof Number number) {
            return number.intValue();
        }
        if (attachment instanceof GlTexture glTexture) {
            return glTexture.getGlId();
        }
        if (attachment instanceof GpuTextureView view) {
            GpuTexture tex = view.texture();
            if (tex instanceof GlTexture glTexture) {
                return glTexture.getGlId();
            }
        }
        // Reflection Fallback 1: getGlId()
        try {
            java.lang.reflect.Method getGlId = attachment.getClass().getMethod("getGlId");
            return ((Number) getGlId.invoke(attachment)).intValue();
        } catch (Exception ignored) {}

        // Reflection Fallback 2: getTextureId()
        try {
            java.lang.reflect.Method getTextureId = attachment.getClass().getMethod("getTextureId");
            return ((Number) getTextureId.invoke(attachment)).intValue();
        } catch (Exception ignored) {}

        // Reflection Fallback 3: id()
        try {
            java.lang.reflect.Method id = attachment.getClass().getMethod("id");
            return ((Number) id.invoke(attachment)).intValue();
        } catch (Exception ignored) {}

        // Reflection Fallback 4: try to find any field of type int that represents the GL ID
        for (String fieldName : new String[]{"glId", "id", "glTextureId", "textureId", "texture"}) {
            try {
                java.lang.reflect.Field field = attachment.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return ((Number) field.get(attachment)).intValue();
            } catch (Exception ignored) {}
        }

        System.err.println("[FluxVisualsClient-Blur-Error] Unknown color attachment class: " + attachment.getClass().getName());
        return 0;
    }

    public static int getDepthTextureId(Framebuffer framebuffer) {
        if (framebuffer == null) {
            return 0;
        }
        Object attachment = framebuffer.getDepthAttachment();
        if (attachment == null) {
            return 0;
        }
        if (attachment instanceof Number number) {
            return number.intValue();
        }
        if (attachment instanceof GlTexture glTexture) {
            return glTexture.getGlId();
        }
        if (attachment instanceof GpuTextureView view) {
            GpuTexture tex = view.texture();
            if (tex instanceof GlTexture glTexture) {
                return glTexture.getGlId();
            }
        }
        // Reflection Fallback 1: getGlId()
        try {
            java.lang.reflect.Method getGlId = attachment.getClass().getMethod("getGlId");
            return ((Number) getGlId.invoke(attachment)).intValue();
        } catch (Exception ignored) {}

        // Reflection Fallback 2: getTextureId()
        try {
            java.lang.reflect.Method getTextureId = attachment.getClass().getMethod("getTextureId");
            return ((Number) getTextureId.invoke(attachment)).intValue();
        } catch (Exception ignored) {}

        // Reflection Fallback 3: id()
        try {
            java.lang.reflect.Method id = attachment.getClass().getMethod("id");
            return ((Number) id.invoke(attachment)).intValue();
        } catch (Exception ignored) {}

        // Reflection Fallback 4: fields
        for (String fieldName : new String[]{"glId", "id", "glTextureId", "textureId", "texture"}) {
            try {
                java.lang.reflect.Field field = attachment.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return ((Number) field.get(attachment)).intValue();
            } catch (Exception ignored) {}
        }

        System.err.println("[FluxVisualsClient-Blur-Error] Unknown depth attachment class: " + attachment.getClass().getName());
        return 0;
    }
}
