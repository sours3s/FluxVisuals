package ru.fluxvisuals.ui.fluxgui.util.render;

import ru.fluxvisuals.ui.fluxgui.FluxVisuals;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Matrix3x2f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class RawTextureRenderer {
    public static final int FLOATS_PER_VERTEX = 8;
    static final int ROUNDED_SEGMENTS = 20;
    static final String VERTEX_SHADER = "#version 150\nin vec2 Position;\nin vec2 UV0;\nin vec4 Color;\nout vec2 texCoord;\nout vec4 vertexColor;\nvoid main() {\n    texCoord = UV0;\n    vertexColor = Color;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}\n";
    static final String FRAGMENT_SHADER = "#version 150\nuniform sampler2D Sampler0;\nin vec2 texCoord;\nin vec4 vertexColor;\nout vec4 fragColor;\nvoid main() {\n    vec4 sampled = texture(Sampler0, texCoord);\n    fragColor = vec4(sampled.rgb * vertexColor.rgb, vertexColor.a);\n}\n";
    static int program;
    static int vao;
    static int vbo;
    static boolean initializationFailed;

    RawTextureRenderer() {
    }

    public static boolean isSupported() {
        return !FabricLoader.getInstance().isModLoaded("vulkanmod") && !initializationFailed;
    }

    public static boolean draw(DrawContext context, int texture, float[] vertices, int vertexCount, boolean deriveScreenUv) {
        if (context != null && texture > 0 && vertices != null && vertexCount >= 3 && vertices.length >= vertexCount * 8 && isSupported()) {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                int scaledWidth = client.getWindow().getScaledWidth();
                int scaledHeight = client.getWindow().getScaledHeight();
                if (scaledWidth > 0 && scaledHeight > 0) {
                    Matrix3x2f matrix = new Matrix3x2f(context.getMatrices());
                    float[] transformed = new float[vertexCount * 8];

                    for (int i = 0; i < vertexCount; i++) {
                        int offset = i * 8;
                        float x = vertices[offset];
                        float y = vertices[offset + 1];
                        float screenX = matrix.m00() * x + matrix.m10() * y + matrix.m20();
                        float screenY = matrix.m01() * x + matrix.m11() * y + matrix.m21();
                        transformed[offset] = screenX / scaledWidth * 2.0F - 1.0F;
                        transformed[offset + 1] = 1.0F - screenY / scaledHeight * 2.0F;
                        transformed[offset + 2] = deriveScreenUv ? screenX / scaledWidth : vertices[offset + 2];
                        transformed[offset + 3] = deriveScreenUv ? 1.0F - screenY / scaledHeight : vertices[offset + 3];
                        System.arraycopy(vertices, offset + 4, transformed, offset + 4, 4);
                    }

                    return drawPrepared(texture, transformed, vertexCount, () -> applyGuiScissor(context, client));
                } else {
                    return false;
                }
            } catch (Throwable var16) {
                initializationFailed = true;
                FluxVisuals.LOGGER.error("Raw GUI texture rendering is unavailable", var16);
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean drawScreenSpace(int texture, float[] vertices, int vertexCount) {
        if (texture > 0 && vertices != null && vertexCount >= 3 && vertices.length >= vertexCount * 8 && isSupported()) {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                int scaledWidth = client.getWindow().getScaledWidth();
                int scaledHeight = client.getWindow().getScaledHeight();
                if (scaledWidth > 0 && scaledHeight > 0) {
                    float[] transformed = new float[vertexCount * 8];

                    for (int i = 0; i < vertexCount; i++) {
                        int offset = i * 8;
                        float screenX = vertices[offset];
                        float screenY = vertices[offset + 1];
                        transformed[offset] = screenX / scaledWidth * 2.0F - 1.0F;
                        transformed[offset + 1] = 1.0F - screenY / scaledHeight * 2.0F;
                        System.arraycopy(vertices, offset + 2, transformed, offset + 2, 6);
                    }

                    return drawPrepared(texture, transformed, vertexCount, () -> GL11.glDisable(3089));
                } else {
                    return false;
                }
            } catch (Throwable var11) {
                initializationFailed = true;
                FluxVisuals.LOGGER.error("Raw post-GUI texture rendering is unavailable", var11);
                return false;
            }
        } else {
            return false;
        }
    }

    static boolean drawPrepared(int texture, float[] transformed, int vertexCount, Runnable scissorSetup) {
        RawTextureRenderer$GlState state = new RawTextureRenderer$GlState();

        int sampler;
        try {
            ensureInitialized();
            if (program != 0) {
                GL11.glEnable(3042);
                GL20.glBlendFuncSeparate(770, 771, 1, 771);
                GL11.glDisable(2929);
                GL11.glDisable(2884);
                GL11.glDepthMask(false);
                scissorSetup.run();
                GL20.glUseProgram(program);
                GL30.glBindVertexArray(vao);
                GL15.glBindBuffer(34962, vbo);
                GL15.glBufferData(34962, transformed, 35040);
                GL13.glActiveTexture(33984);
                GL11.glBindTexture(3553, texture);
                sampler = GL20.glGetUniformLocation(program, "Sampler0");
                if (sampler >= 0) {
                    GL20.glUniform1i(sampler, 0);
                }

                GL11.glDrawArrays(4, 0, vertexCount);
                return true;
            }

            sampler = 0;
        } finally {
            state.restore();
        }

        return sampler != 0;
    }

    public static boolean drawScreenMappedRounded(DrawContext context, int texture, float x, float y, float width, float height, float radius, float alpha) {
        if (!(width <= 0.0F) && !(height <= 0.0F) && !(alpha <= 0.0F)) {
            float safeRadius = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
            float[] boundary;
            if (safeRadius < 0.01F) {
                boundary = new float[]{x, y, x + width, y, x + width, y + height, x, y + height};
            } else {
                boundary = new float[168];
                int cursor = 0;
                cursor = appendArc(boundary, cursor, x + width - safeRadius, y + safeRadius, safeRadius, 0.0F, (float) (-Math.PI / 2));
                cursor = appendArc(boundary, cursor, x + safeRadius, y + safeRadius, safeRadius, (float) (-Math.PI / 2), (float) -Math.PI);
                cursor = appendArc(boundary, cursor, x + safeRadius, y + height - safeRadius, safeRadius, (float) -Math.PI, (float) (-Math.PI * 3.0 / 2.0));
                appendArc(boundary, cursor, x + width - safeRadius, y + height - safeRadius, safeRadius, (float) (-Math.PI * 3.0 / 2.0), (float) (-Math.PI * 2));
            }

            int boundaryCount = boundary.length / 2;
            int vertexCount = boundaryCount * 3;
            float[] vertices = new float[vertexCount * 8];
            float centerX = x + width * 0.5F;
            float centerY = y + height * 0.5F;
            float a = Math.max(0.0F, Math.min(1.0F, alpha));
            int cursor = 0;

            for (int i = 0; i < boundaryCount; i++) {
                int next = (i + 1) % boundaryCount;
                cursor = putVertex(vertices, cursor, centerX, centerY, 0.0F, 0.0F, a);
                cursor = putVertex(vertices, cursor, boundary[i * 2], boundary[i * 2 + 1], 0.0F, 0.0F, a);
                cursor = putVertex(vertices, cursor, boundary[next * 2], boundary[next * 2 + 1], 0.0F, 0.0F, a);
            }

            return draw(context, texture, vertices, vertexCount, true);
        } else {
            return false;
        }
    }

    static int appendArc(float[] target, int cursor, float centerX, float centerY, float radius, float start, float end) {
        for (int i = 0; i <= 20; i++) {
            float progress = i / 20.0F;
            float angle = start + (end - start) * progress;
            target[cursor++] = centerX + (float)Math.cos(angle) * radius;
            target[cursor++] = centerY + (float)Math.sin(angle) * radius;
        }

        return cursor;
    }

    public static int putVertex(float[] target, int cursor, float x, float y, float u, float v, float alpha) {
        target[cursor++] = x;
        target[cursor++] = y;
        target[cursor++] = u;
        target[cursor++] = v;
        target[cursor++] = 1.0F;
        target[cursor++] = 1.0F;
        target[cursor++] = 1.0F;
        target[cursor++] = alpha;
        return cursor;
    }

    static void ensureInitialized() {
        if (program == 0 && !initializationFailed) {
            int vertex = compile(
                35633,
                "#version 150\nin vec2 Position;\nin vec2 UV0;\nin vec4 Color;\nout vec2 texCoord;\nout vec4 vertexColor;\nvoid main() {\n    texCoord = UV0;\n    vertexColor = Color;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}\n"
            );
            int fragment = compile(
                35632,
                "#version 150\nuniform sampler2D Sampler0;\nin vec2 texCoord;\nin vec4 vertexColor;\nout vec4 fragColor;\nvoid main() {\n    vec4 sampled = texture(Sampler0, texCoord);\n    fragColor = vec4(sampled.rgb * vertexColor.rgb, vertexColor.a);\n}\n"
            );
            int linked = GL20.glCreateProgram();
            GL20.glAttachShader(linked, vertex);
            GL20.glAttachShader(linked, fragment);
            GL20.glBindAttribLocation(linked, 0, "Position");
            GL20.glBindAttribLocation(linked, 1, "UV0");
            GL20.glBindAttribLocation(linked, 2, "Color");
            GL20.glLinkProgram(linked);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            if (GL20.glGetProgrami(linked, 35714) == 0) {
                String log = GL20.glGetProgramInfoLog(linked);
                GL20.glDeleteProgram(linked);
                throw new IllegalStateException("Unable to link raw GUI texture shader: " + log);
            }

            int newVao = GL30.glGenVertexArrays();
            int newVbo = GL15.glGenBuffers();
            GL30.glBindVertexArray(newVao);
            GL15.glBindBuffer(34962, newVbo);
            int stride = 32;
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, 5126, false, stride, 0L);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, 5126, false, stride, 8L);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 4, 5126, false, stride, 16L);
            program = linked;
            vao = newVao;
            vbo = newVbo;
        }
    }

    static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, 35713) == 0) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Unable to compile raw GUI texture shader: " + log);
        } else {
            return shader;
        }
    }

    static void applyGuiScissor(DrawContext context, MinecraftClient client) {
        ScreenRect scissor = context.scissorStack.peekLast();
        if (scissor == null) {
            GL11.glDisable(3089);
        } else {
            int scaledWidth = Math.max(1, client.getWindow().getScaledWidth());
            int scaledHeight = Math.max(1, client.getWindow().getScaledHeight());
            int framebufferWidth = Math.max(1, client.getWindow().getFramebufferWidth());
            int framebufferHeight = Math.max(1, client.getWindow().getFramebufferHeight());
            int left = Math.max(0, Math.round((float)(scissor.getLeft() * framebufferWidth) / scaledWidth));
            int bottom = Math.max(0, Math.round((float)((scaledHeight - scissor.getBottom()) * framebufferHeight) / scaledHeight));
            int width = Math.max(0, Math.round((float)(scissor.comp_1196() * framebufferWidth) / scaledWidth));
            int height = Math.max(0, Math.round((float)(scissor.comp_1197() * framebufferHeight) / scaledHeight));
            GL11.glEnable(3089);
            GL11.glScissor(left, bottom, width, height);
        }
    }
}
