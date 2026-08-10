package ru.fluxvisuals.api.render.system.sys2d;

import com.mojang.blaze3d.opengl.GlStateManager;
import ru.fluxvisuals.api.render.system.ShaderUse;
import ru.fluxvisuals.api.render.system.TextureUse;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import ru.fluxvisuals.Client;
import org.lwjgl.system.MemoryUtil;
import sun.misc.Unsafe;

import java.awt.*;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static ru.fluxvisuals.MinecraftHolder.mc;
import static ru.fluxvisuals.MinecraftHolder.window;
import static ru.fluxvisuals.api.render.system.sys2d.AttributeHelper.*;

public class CRenderSystem {
    private static final CRenderSystem INSTANCE = new CRenderSystem();
    private final Stack<ScissorState> scissorStack = new Stack<>();
    private int vao, vbo, ebo;

    @Getter
    private TextureAtlas atlas;

    public void putTex(TextureUse textureUse, int glId) {
        textures[textureUse.id] = glId;
    }

    public CFramebuf cFramebuf;
    private static class LayerData {
        FloatBuffer vertices;
        IntBuffer indexes;
        int quadCount;
        int texId;
        long lastReset = System.currentTimeMillis();

        long vertexAddress;

        LayerData(int maxQuads) {
            this.vertices = BufferUtils.createFloatBuffer(maxQuads * 4 * SIZE);
            this.indexes = BufferUtils.createIntBuffer(maxQuads * 6);
            this.quadCount = 0;

            this.vertexAddress = MemoryUtil.memAddress(this.vertices);
        }

        void clear() {
            vertices.clear();
            indexes.clear();
            quadCount = 0;
            lastReset = System.currentTimeMillis();
        }
        void clearTimeout() {
            if (System.currentTimeMillis() - lastReset > 5000) {
                clear();
            }
        }

        int vertexFloatCount() {
            return vertices.position();
        }

        int indexCount() {
            return indexes.position();
        }
    }

    public enum RenderLayer {
        POST,
        BLUR,
        HUD,
        ESP,
        POST_SCREEN,
        OVERLAY,
    }

    private final Map<RenderLayer, LayerData> layers = new HashMap<>();
    private RenderLayer currentLayer = RenderLayer.OVERLAY;

    public int FBO, FBO_TEX; int FBO_WIDTH = 0, FBO_HEIGHT = 0;
    int FBO_RBO = 0;

    private float globalAlpha = 1.0f;
    float x, y, z, blurRadius, width, height, texU, texV, texW = 1f, texH = 1f, thickness, msdfRange, scissorX, scissorY, scissorWidth, scissorHeight;
    Color color1 = Color.WHITE, color2 = Color.WHITE, color3 = Color.WHITE, color4 = Color.WHITE;
    ShaderUse using = ShaderUse.RECTANGLE;
    Vector4f round = new Vector4f();
    Vector2f smoothness = new Vector2f();
    float texId;
    float hatch;
    float useCircle;
    boolean extraColor;
    private static final int MAX_QUADS = 1_000;

    int[] textures = new int[16];

    @Setter
    public Shader shader;


    private static final Unsafe UNSAFE;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Unsafe instance", e);
        }
    }

    private ArrayList<Identifier> prepareTasks = new ArrayList<>();

    public CRenderSystem() {
        atlas = new TextureAtlas(5000, 5000);

        try {
            atlas.add(TextureUse.SFMEDIUM, Identifier.of("godweer", "fonts/sfmedium.png"));
            atlas.add(TextureUse.ICONS, Identifier.of("godweer", "fonts/icons.png"));
            atlas.add(TextureUse.EMOJIS, Identifier.of("godweer", "fonts/emojis.png"));
            atlas.add(TextureUse.ICONS_NURIK, Identifier.of("godweer", "fonts/icons_nurik.png"));
        } catch (Exception e) {}

        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        ebo = GL15.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) MAX_QUADS * 4 * SIZE * Float.BYTES, GL15.GL_STREAM_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, (long) MAX_QUADS * 6 * Integer.BYTES, GL15.GL_STREAM_DRAW);
        addAttribute(3, GL30.GL_FLOAT, false);
        addAttribute(2, GL30.GL_FLOAT, true);
        addAttribute(4, GL30.GL_FLOAT, false);
        addAttribute(2, GL30.GL_FLOAT, false);
        addAttribute(4, GL30.GL_FLOAT, false);
        addAttribute(1, GL30.GL_FLOAT, false);
        addAttribute(2, GL30.GL_FLOAT, false);
        addAttribute(1, GL30.GL_FLOAT, false);
        addAttribute(1, GL30.GL_FLOAT, false);
        addAttribute(1, GL30.GL_FLOAT, false);
        addAttribute(4, GL30.GL_FLOAT, false);
        addAttribute(2, GL30.GL_FLOAT, false);
        addAttribute(2, GL30.GL_FLOAT, true);
        addAttribute(1, GL30.GL_FLOAT, false);
        addAttribute(1, GL30.GL_FLOAT, false);
        createAttributes();
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        for (RenderLayer layer : RenderLayer.values()) {
            layers.put(layer, new LayerData(MAX_QUADS));
        }
        shader = new Shader();
        FBO = GL30.glGenFramebuffers();
        FBO_TEX = GL11.glGenTextures();
        FBO_RBO = GL30.glGenRenderbuffers();

        cFramebuf = new CFramebuf(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);

    }

    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (width == FBO_WIDTH && height == FBO_HEIGHT) return;

        FBO_WIDTH = width;
        FBO_HEIGHT = height;

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, FBO);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, FBO_TEX);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, FBO_WIDTH, FBO_HEIGHT, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, FBO_TEX, 0);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, FBO_RBO);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, FBO_WIDTH, FBO_HEIGHT);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, FBO_RBO);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            throw new RuntimeException("FBO incomplete on resize, status: 0x" + Integer.toHexString(status));
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public CRenderSystem layer(RenderLayer layer) {
        this.currentLayer = layer;
        return this;
    }
    public RenderLayer layer() {
        return currentLayer;
    }

    public CRenderSystem layerTex(int texId) {
        this.layers.get(currentLayer).texId = texId;
        return this;
    }

    public CRenderSystem alpha(float alpha) {
        this.globalAlpha = Math.min(alpha, 1.f);
        return this;
    }
    public float alpha() {
        return this.globalAlpha;
    }

    public CRenderSystem hatch(float hatch) {
        this.hatch = hatch;
        return this;
    }

    public CRenderSystem scissor(float x, float y, float width, float height) {
        int h = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int f = (int) MinecraftClient.getInstance().getWindow().getScaleFactor();

        this.scissorX = x * f;
        this.scissorY = (h - y - height) * f;
        this.scissorWidth = width * f;
        this.scissorHeight = height * f;
        return this;
    }

    public CRenderSystem disableScissor() {
        int w = MinecraftClient.getInstance().getWindow().getFramebufferWidth();
        int h = MinecraftClient.getInstance().getWindow().getFramebufferHeight();

        this.scissorX = 0;
        this.scissorY = 0;
        this.scissorWidth = w;
        this.scissorHeight = h;
        return this;
    }

    public CRenderSystem useCircle(float value) {
        this.useCircle = value;
        return this;
    }
    public float useCircle() {
        return this.useCircle;
    }

    public void extraColor(boolean value) {
        extraColor = value;
    }
    public boolean extraColor() {
        return extraColor;
    }

    public void push(float x, float y, float width, float height) {

        if (!scissorStack.isEmpty()) {
            push2(
                    x,
                    y,
                    width,
                    height);
            return;
        }
        int f = MinecraftClient.getInstance().getWindow().getScaleFactor();
        scissorStack.push(new ScissorState(
                (int) x,
                (int) y,
                (int) width,
                (int) height));
        this.scissor(x, y, width, height);
    }

    public void push2(float x, float y, float width, float height) {
        if (scissorStack.isEmpty()) throw new IllegalStateException("push2 called without a corresponding push call");

        int f = MinecraftClient.getInstance().getWindow().getScaleFactor();
        ScissorState currentState = scissorStack.peek();
        int newX = (int) MathHelper.clamp(x, currentState.x, currentState.x + currentState.width);
        int newY = (int) MathHelper.clamp(y, currentState.y, currentState.y + currentState.height);
        int newWidth = (int) (MathHelper.clamp(x + width, newX, currentState.x + currentState.width) - newX);
        int newHeight = (int) (MathHelper.clamp(y + height, newY, currentState.y + currentState.height) - newY);

        scissorStack.push(new ScissorState(newX, newY, newWidth, newHeight));
        this.scissor(newX, newY, newWidth, newHeight);
    }

    public void pop() {
        if (scissorStack.isEmpty()) throw new IllegalStateException("pop called without a corresponding push call");

        scissorStack.pop();
        this.disableScissor();
        if (!scissorStack.isEmpty()) {
            ScissorState currentState = scissorStack.peek();
            this.scissor(currentState.x, currentState.y, currentState.width, currentState.height);
        }
    }

    public CRenderSystem line(float x1, float y1, float x2, float y2) {
        this.x = x1;
        this.y = y1;
        this.width = x2 - x1;
        this.height = y2 - y1;
        return this;
    }

    public CRenderSystem rect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public CRenderSystem z(float z) {
        this.z = z;
        return this;
    }

    public CRenderSystem msdfRange(float range) { this.msdfRange = range; return this; }
    public CRenderSystem blur(float blurRadius) { this.blurRadius = blurRadius; return this; }
    public CRenderSystem color(Color color1, Color color2, Color color3, Color color4) { this.color1 = color1; this.color2 = color2; this.color3 = color3; this.color4 = color4; return this; }
    public CRenderSystem uv(float u, float v, float width, float height) { this.texU = u; this.texV = v; this.texW = width; this.texH = height; return this; }
    public CRenderSystem round(float r1, float r2, float r3, float r4) { this.round = new Vector4f(r1, r2, r3, r4); return this; }
    public CRenderSystem shader(ShaderUse shaderUse) { this.using = shaderUse; return this; }
    public CRenderSystem smoothness(float s1, float s2) { this.smoothness.x = s1; this.smoothness.y = s2; return this; }
    public CRenderSystem thickness(float thickness) { this.thickness = thickness; return this; }
    public CRenderSystem texture(int texId) { this.texId = texId; return this; }

    private void ensureCapacityForLayer(LayerData layer, int additionalFloats, int additionalIndices) {
        if (layer.vertices.remaining() < additionalFloats) {
            int needed = layer.vertices.position() + additionalFloats;
            int newCap = Math.max(layer.vertices.capacity() * 2, needed);
            FloatBuffer newBuf = BufferUtils.createFloatBuffer(newCap);
            layer.vertices.flip();
            newBuf.put(layer.vertices);
            layer.vertices = newBuf;
        }
        if (layer.indexes.remaining() < additionalIndices) {
            int needed = layer.indexes.position() + additionalIndices;
            int newCap = Math.max(layer.indexes.capacity() * 2, needed);
            IntBuffer newBuf = BufferUtils.createIntBuffer(newCap);
            layer.indexes.flip();
            newBuf.put(layer.indexes);
            layer.indexes = newBuf;
        }
    }
    public void build() {

        LayerData layer = layers.get(currentLayer);
        ensureCapacityForLayer(layer, 4 * SIZE, 6);
        int base = layer.quadCount * 4;
        int u_shader = using.ordinal();
        Matrix4f matrix = Client.RENDERER.getStack().peek().getPositionMatrix();
        this.x /= (float) window.getScaledWidth() / window.getWidth();
        this.y /= (float) window.getScaledHeight() / window.getHeight();
        this.width /= (float) window.getScaledWidth() / window.getWidth();
        this.height /= (float) window.getScaledHeight() / window.getHeight();
        Vector3f a0 = new Vector3f(x, y, z);
        Vector3f a1 = new Vector3f(x, y + height, z);
        Vector3f a2 = new Vector3f(x + width, y + height, z);
        Vector3f a3 = new Vector3f(x + width, y, z);
        matrix.transformPosition(a0);
        matrix.transformPosition(a1);
        matrix.transformPosition(a2);
        matrix.transformPosition(a3);

        float sc = (float) window.getScaledWidth() / window.getWidth() + (float) window.getScaledHeight() / window.getHeight();

        round.mul(1 / sc);

        Vector4f b0 = new Vector4f(this.scissorX, this.scissorY, z, 0).mul(matrix);
        Vector4f b3 = new Vector4f(scissorWidth, scissorHeight, z, 0).mul(matrix);

        float colorFactor = extraColor ? 100F : 255F;

        layer.vertices.put(new float[]{
                a0.x, a0.y, a0.z, texU, texV, color1.getRed() / colorFactor, color1.getGreen() / colorFactor, color1.getBlue() / colorFactor, (color1.getAlpha() / 255F) * globalAlpha, width, height,
                round.x, round.y, round.z, round.w, u_shader, smoothness.x, smoothness.y,
                thickness, msdfRange, blurRadius, this.scissorX, this.scissorY, this.scissorWidth, this.scissorHeight, this.x, this.y, 0, 0, hatch, useCircle,
                a1.x, a1.y, a1.z, texU, texV + texH, color2.getRed() / colorFactor, color2.getGreen() / colorFactor, color2.getBlue() / colorFactor, (color2.getAlpha() / 255F) * globalAlpha, width, height,
                round.x, round.y, round.z, round.w, u_shader, smoothness.x, smoothness.y,
                thickness, msdfRange, blurRadius, this.scissorX, this.scissorY, this.scissorWidth, this.scissorHeight, this.x, this.y, 0, 1, hatch, useCircle,
                a2.x, a2.y, a2.z, texU + texW, texV + texH, color3.getRed() / colorFactor, color3.getGreen() / colorFactor, color3.getBlue() / colorFactor, (color3.getAlpha() / 255F) * globalAlpha, width, height,
                round.x, round.y, round.z, round.w, u_shader, smoothness.x, smoothness.y,
                thickness, msdfRange, blurRadius, this.scissorX, this.scissorY, this.scissorWidth, this.scissorHeight, this.x, this.y, 1, 1, hatch, useCircle,
                a3.x, a3.y, a3.z, texU + texW, texV, color4.getRed() / colorFactor, color4.getGreen() / colorFactor, color4.getBlue() / colorFactor, (color4.getAlpha() / 255f) * globalAlpha, width, height,
                round.x, round.y, round.z, round.w, u_shader, smoothness.x, smoothness.y,
                thickness, msdfRange, blurRadius, this.scissorX, this.scissorY, this.scissorWidth, this.scissorHeight, this.x, this.y, 1, 0, hatch, useCircle,
        });

        layer.indexes.put(new int[]{
                base, base + 1, base + 3,
                base + 1, base + 2, base + 3
        });
        layer.quadCount++;
        texU = 0f; texV = 0f; texW = 1f; texH = 1f;
    }

    public CRenderSystem addPrepare(Identifier identifier) {
        prepareTasks.add(identifier);
        return this;
    }

    public void prepare() {
        textures[0] = atlas.getGlId();

        for (Identifier prepared: prepareTasks) {
            if (!getAtlas().has(prepared.toString()))
                getAtlas().addTextureFromImage(prepared.toString(), prepared);
        }
        prepareTasks.clear();
    }

    public void render(RenderLayer renderLayer) {
        Window window = MinecraftClient.getInstance().getWindow();
        MinecraftClient mc = MinecraftClient.getInstance();
        LayerData layer = layers.get(renderLayer);
        int currentVao = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        float f = mc.getRenderTickCounter().getTickProgress(true);
        Matrix4f projFov = this.createProjectionMatrix(mc.gameRenderer.getFov(mc.gameRenderer.getCamera(), f, false));
        Matrix4f projFb = this.createProjectionMatrix((float) mc.getFramebuffer().textureWidth, (float) mc.getFramebuffer().textureHeight);
        projFov.translate(0, 0, -0.7146f);
        int[] samplers = new int[textures.length];
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(layer.texId);
        GlStateManager._texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        GlStateManager._texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_NEAREST);
        GlStateManager._glBindVertexArray(vao);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GlStateManager._glBufferData(GL15.GL_ARRAY_BUFFER, (long) layer.vertices.capacity() * Float.BYTES, GL15.GL_DYNAMIC_DRAW);
        GlStateManager._glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, (long) layer.indexes.capacity() * Integer.BYTES, GL15.GL_DYNAMIC_DRAW);
        layer.vertices.flip();
        layer.indexes.flip();
        long vAddr = MemoryUtil.memAddress(layer.vertices);
        long vSizeBytes = (long) layer.vertices.limit() * Float.BYTES;
        org.lwjgl.opengl.GL15C.nglBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vSizeBytes, vAddr);
        long iAddr = MemoryUtil.memAddress(layer.indexes);
        long iSizeBytes = (long) layer.indexes.limit() * Integer.BYTES;
        org.lwjgl.opengl.GL15C.nglBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, iSizeBytes, iAddr);
        shader.bind();
        Matrix4f projOrtho = createProjectionMatrix(window.getFramebufferWidth(), window.getFramebufferHeight());
        shader.uploadMatrix(projOrtho, new Matrix4f().identity());
        int locTextures = GlStateManager._glGetUniformLocation(shader.getId(), "tex");
        GlStateManager._glUniform1i(locTextures, 0);
        int locRes = GlStateManager._glGetUniformLocation(shader.getId(), "iResolution");
        if (locRes >= 0) GL30.glUniform2f(locRes, window.getScaledWidth(), window.getScaledHeight());
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDrawElements(GL11.GL_TRIANGLES, (int) layer.indexes.limit(), GL11.GL_UNSIGNED_INT, 0);
        layer.clear();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GlStateManager._glBindVertexArray(0);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        if (currentVao != 0) GlStateManager._glBindVertexArray(currentVao);
        Client.RENDERER.getCrenderSystem().disableScissor();
    }

    public void postRender() {
        layers.values().forEach(LayerData::clearTimeout);
    }

    private Matrix4f createProjectionMatrix(float f, float g) {
        return new Matrix4f().setOrtho(0.0F, f, g, 0.0F, 1000.0F, -11000.0F);
    }
    private Matrix4f createProjectionMatrix(float fovDeg) {
        return new Matrix4f().perspective(fovDeg * (float) (Math.PI / 180.0), 1f, 0.05f, 1000f);
    }

    public static CRenderSystem getInstance() {
        return INSTANCE;
    }

    private static class ScissorState { int x, y, width, height; ScissorState(int x,int y,int w,int h){this.x=x;this.y=y;this.width=w;this.height=h;} }
}
