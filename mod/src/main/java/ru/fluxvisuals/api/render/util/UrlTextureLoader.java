package ru.fluxvisuals.api.render.util;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.PointerBuffer;
import ru.fluxvisuals.Client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Create by daun kvass
 */
public class UrlTextureLoader {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "UrlTextureLoader");
        t.setDaemon(true);
        return t;
    });

    private enum State { IDLE, LOADING, DONE, FAILED }

    private static final Map<String, State>      states  = new ConcurrentHashMap<>();
    private static final Map<String, byte[]>     pending = new ConcurrentHashMap<>();

    public static String get(String url) {
        if (url == null || url.isEmpty()) return null;

        String atlasKey = "url:" + url;
        if (Client.RENDERER != null
                && Client.RENDERER.getCrenderSystem() != null
                && Client.RENDERER.getCrenderSystem().getAtlas().has(atlasKey)) {
            return atlasKey;
        }

        State state = states.getOrDefault(url, State.IDLE);

        if (state == State.FAILED) return null;

        if (state == State.DONE) {
            byte[] bytes = pending.remove(url);
            if (bytes != null) uploadToAtlas(atlasKey, bytes);
            return Client.RENDERER.getCrenderSystem().getAtlas().has(atlasKey) ? atlasKey : null;
        }

        if (state == State.IDLE) {
            states.put(url, State.LOADING);
            EXECUTOR.submit(() -> download(url));
        }

        return null;
    }

    private static void download(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.connect();

            if (conn.getResponseCode() != 200) {
                states.put(urlStr, State.FAILED);
                return;
            }

            byte[] bytes;
            try (InputStream is = conn.getInputStream()) {
                bytes = is.readAllBytes();
            }

            pending.put(urlStr, bytes);
            states.put(urlStr, State.DONE);

        } catch (Exception e) {
            states.put(urlStr, State.FAILED);
        }
    }

    private static void uploadToAtlas(String atlasKey, byte[] imageBytes) {
        ByteBuffer rawBuf = null;
        ByteBuffer pixelBuf = null;
        try {
            rawBuf = MemoryUtil.memAlloc(imageBytes.length);
            rawBuf.put(imageBytes).flip();
            IntBuffer w = MemoryUtil.memAllocInt(1);
            IntBuffer h = MemoryUtil.memAllocInt(1);
            IntBuffer ch = MemoryUtil.memAllocInt(1);
            boolean isGif = imageBytes.length > 3
                && imageBytes[0] == 'G' && imageBytes[1] == 'I' && imageBytes[2] == 'F';

            int frameCount = 1;
            if (isGif) {
                PointerBuffer delaysOut = MemoryUtil.memAllocPointer(1);
                IntBuffer framesOut = MemoryUtil.memAllocInt(1);
                pixelBuf = STBImage.stbi_load_gif_from_memory(rawBuf, delaysOut, w, h, framesOut, ch, 4);
                frameCount = framesOut.get(0);
                MemoryUtil.memFree(delaysOut);
                MemoryUtil.memFree(framesOut);
            } else {
                pixelBuf = STBImage.stbi_load_from_memory(rawBuf, w, h, ch, 4);
            }

            MemoryUtil.memFree(rawBuf);
            rawBuf = null;

            if (pixelBuf == null) {
                MemoryUtil.memFree(w); MemoryUtil.memFree(h); MemoryUtil.memFree(ch);
                return;
            }

            int width  = w.get(0);
            int totalH = h.get(0);
            int height = (isGif && frameCount > 1) ? totalH / frameCount : totalH;
            MemoryUtil.memFree(w); MemoryUtil.memFree(h); MemoryUtil.memFree(ch);
            int glId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuf);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            STBImage.stbi_image_free(pixelBuf);
            pixelBuf = null;

            Client.RENDERER.getCrenderSystem().getAtlas().addTextureFromGL(atlasKey, glId);
            GL11.glDeleteTextures(glId);

        } catch (Exception e) {
            if (rawBuf   != null) MemoryUtil.memFree(rawBuf);
            if (pixelBuf != null) STBImage.stbi_image_free(pixelBuf);
        }
    }
    public static void invalidate(String url) {
        if (url == null) return;
        states.remove(url);
        pending.remove(url);
    }

    public static String getResource(String resourcePath) {
        if (resourcePath == null || resourcePath.isEmpty()) return null;

        String atlasKey = "res:" + resourcePath;
        if (Client.RENDERER != null
                && Client.RENDERER.getCrenderSystem() != null
                && Client.RENDERER.getCrenderSystem().getAtlas().has(atlasKey)) {
            return atlasKey;
        }

        State state = states.getOrDefault(atlasKey, State.IDLE);
        if (state == State.FAILED) return null;

        if (state == State.DONE) {
            byte[] bytes = pending.remove(atlasKey);
            if (bytes != null) uploadToAtlas(atlasKey, bytes);
            return Client.RENDERER.getCrenderSystem().getAtlas().has(atlasKey) ? atlasKey : null;
        }

        if (state == State.IDLE) {
            states.put(atlasKey, State.LOADING);
            EXECUTOR.submit(() -> {
                try (InputStream is = UrlTextureLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
                    if (is == null) { states.put(atlasKey, State.FAILED); return; }
                    pending.put(atlasKey, is.readAllBytes());
                    states.put(atlasKey, State.DONE);
                } catch (Exception e) {
                    states.put(atlasKey, State.FAILED);
                }
            });
        }

        return null;
    }}
