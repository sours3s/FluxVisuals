package ru.fluxvisuals.module.impl.visuals;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import ru.fluxvisuals.mixin.accessor.PostEffectPassAccessor;
import ru.fluxvisuals.mixin.accessor.PostEffectProcessorAccessor;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Донорский MotionBlur: постобработка по матрицам камеры предыдущего и текущего кадра.
 */
@IModule(name = "MotionBlur", description = "Размытие движения на основе постобработки", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class MotionBlur extends Module {
    private static final Identifier ID = Identifier.ofVanilla("motion_blur");
    private static final String UNIFORM_BLOCK = "MotionBlurUniforms";

    private static final Matrix4f CURRENT_MODEL_VIEW = new Matrix4f();
    private static final Matrix4f CURRENT_PROJECTION = new Matrix4f();
    private static final Vector3f CURRENT_CAMERA = new Vector3f();
    private static final Matrix4f PREVIOUS_MODEL_VIEW = new Matrix4f();
    private static final Matrix4f PREVIOUS_PROJECTION = new Matrix4f();
    private static final Vector3f PREVIOUS_CAMERA = new Vector3f();
    private static final Matrix4f INVERSE_MODEL_VIEW = new Matrix4f();
    private static final Matrix4f INVERSE_PROJECTION = new Matrix4f();

    private static final AtomicInteger RELOAD_COUNT = new AtomicInteger();
    private static volatile boolean RELOADING;

    private static boolean enabled;
    private static boolean hasCurrentFrame;
    private static boolean hasPreviousFrame;
    private static float strength = 1.0F;
    private static int samples = 32;
    private static boolean useDepth = true;
    private static boolean centered = false;
    private static boolean thirdPerson = false;
    private static boolean hzCompensation = true;
    private static long frameTime = 16_666_666L;
    private static long lastNanoTime;
    private static float hzMultiplier = 1.0F;
    private static ObjectAllocator allocator;
    private static PostEffectProcessor processor;
    private static boolean processorFailed;
    private static int processorReload = -1;

    private final SliderSetting strengthSetting = new SliderSetting("Сила", 1.0F, 0.0F, 3.0F, 0.05F, false);
    private final SliderSetting samplesSetting = new SliderSetting("Сэмплы", 32.0F, 4.0F, 128.0F, 1.0F, false);
    private final BooleanSetting depthSetting = new BooleanSetting("По глубине", true);
    private final BooleanSetting centeredSetting = new BooleanSetting("По центру", false);
    private final BooleanSetting thirdPersonSetting = new BooleanSetting("От третьего лица", false);
    private final BooleanSetting hzSetting = new BooleanSetting("Компенсация Гц", true);

    public MotionBlur() {
        this.strengthSetting.description = "Длина смаза за один кадр";
        this.samplesSetting.description = "Предел выборок на пиксель: выше — плавнее, но дороже";
        this.depthSetting.description = "Смазывать по глубине сцены, а не только от поворота камеры";
        this.centeredSetting.description = "След в обе стороны от пикселя, а не только назад";
        this.thirdPersonSetting.description = "Смазывать картинку и в виде от третьего лица";
        this.hzSetting.description = "Усиливать след, когда FPS выше частоты монитора";
        this.addSettings(new Setting[]{
                this.strengthSetting, this.samplesSetting, this.depthSetting,
                this.centeredSetting, this.thirdPersonSetting, this.hzSetting
        });
    }

    private static MinecraftClient client() {
        return MinecraftClient.getInstance();
    }

    private static int displayRefreshRate() {
        MinecraftClient client = client();
        if (client == null) {
            return 60;
        }
        try {
            Window window = client.getWindow();
            if (window == null || window.getHandle() == 0L) {
                return 60;
            }
            long monitor = GLFW.glfwGetPrimaryMonitor();
            if (monitor == 0L) {
                return 60;
            }
            GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
            return mode != null && mode.refreshRate() > 0 ? mode.refreshRate() : 60;
        } catch (Throwable ignored) {
            return 60;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        enabled = true;
        frameTime = 1_000_000_000L / displayRefreshRate();
        lastNanoTime = 0L;
        hzMultiplier = 1.0F;
        this.syncSettings();
    }

    @Override
    public void onDisable() {
        enabled = false;
        hasCurrentFrame = false;
        hasPreviousFrame = false;
        super.onDisable();
    }

    /** Вызывается из MinecraftClient.tick. */
    public static void tick() {
        if (enabled) {
            hasCurrentFrame = true;
            MotionBlur instance = instance();
            if (instance != null) {
                instance.syncSettings();
            }
        }
    }

    private static MotionBlur instance() {
        if (!enabled || client() == null || client().world == null) {
            return null;
        }
        if (ru.fluxvisuals.client.FluxVisualsClient.get == null
                || ru.fluxvisuals.client.FluxVisualsClient.get.manager == null) {
            return null;
        }
        return ru.fluxvisuals.client.FluxVisualsClient.get.manager.get(MotionBlur.class);
    }

    private void syncSettings() {
        strength = this.strengthSetting.get();
        samples = Math.max(1, (int) this.samplesSetting.get());
        useDepth = this.depthSetting.get();
        centered = this.centeredSetting.get();
        thirdPerson = this.thirdPersonSetting.get();
        hzCompensation = this.hzSetting.get();
    }

    private static void updateHzMultiplier() {
        long now = System.nanoTime();
        long delta = now - lastNanoTime;
        lastNanoTime = now;
        if (delta > 0L && delta < frameTime) {
            hzMultiplier = Math.min((float) frameTime / (float) delta, 8.0F);
        } else {
            hzMultiplier = 1.0F;
        }
    }

    private static boolean shouldRender() {
        MinecraftClient client = client();
        return enabled
                && hasCurrentFrame
                && hasPreviousFrame
                && strength > 0.0F
                && (thirdPerson || client != null && client.options.getPerspective().isFirstPerson());
    }

    private static float effectiveStrength() {
        return hzCompensation ? strength * hzMultiplier : strength;
    }

    /** Хук HEAD WorldRenderer.render: запоминаем текущие матрицы, проекцию и позицию камеры. */
    public static void captureFrame(ObjectAllocator objectAllocator, Matrix4f modelView, Matrix4f projection, Camera camera) {
        MinecraftClient client = client();
        if (!enabled || camera == null || client == null || client.gameRenderer.isRenderingPanorama()) {
            hasCurrentFrame = false;
            hasPreviousFrame = false;
            lastNanoTime = 0L;
            return;
        }

        allocator = objectAllocator;
        updateHzMultiplier();

        if (hasCurrentFrame) {
            PREVIOUS_MODEL_VIEW.set(CURRENT_MODEL_VIEW);
            PREVIOUS_PROJECTION.set(CURRENT_PROJECTION);
            PREVIOUS_CAMERA.set(CURRENT_CAMERA);
            hasPreviousFrame = true;
        }

        Vec3d pos = camera.getCameraPos();
        CURRENT_MODEL_VIEW.set(modelView);
        CURRENT_PROJECTION.set(projection);
        CURRENT_CAMERA.set((float) (pos.x % 30000.0), (float) (pos.y % 30000.0), (float) (pos.z % 30000.0));
        hasCurrentFrame = true;

        if (hasPreviousFrame && CURRENT_CAMERA.distanceSquared(PREVIOUS_CAMERA) > 16.0F) {
            hasPreviousFrame = false;
        }
    }

    /** Вызывается при перезагрузке ресурсов: инвалидируем кэш постпроцессора. */
    public static void onReloadStart() {
        RELOADING = true;
        RELOAD_COUNT.incrementAndGet();
    }

    public static void onReloadComplete() {
        RELOADING = false;
    }

    private static PostEffectProcessor getProcessor(ShaderLoader loader) {
        int reload = RELOAD_COUNT.get();
        if (reload != processorReload) {
            processorReload = reload;
            processor = null;
            processorFailed = false;
        }
        if (processor == null && !processorFailed) {
            try {
                processor = loader.loadPostEffect(ID, DefaultFramebufferSet.MAIN_ONLY);
            } catch (Throwable ignored) {
                processor = null;
            }
            processorFailed = processor == null;
        }
        return processor;
    }

    private static void updateUniforms(PostEffectProcessor postEffectProcessor) {
        List<PostEffectPass> passes = ((PostEffectProcessorAccessor) postEffectProcessor).fluxvisuals$passes();
        if (passes.isEmpty()) {
            return;
        }
        GpuBuffer buffer = ((PostEffectPassAccessor) passes.get(0)).fluxvisuals$uniformBuffers().get(UNIFORM_BLOCK);
        if (buffer == null) {
            return;
        }

        INVERSE_MODEL_VIEW.set(CURRENT_MODEL_VIEW).invert();
        INVERSE_PROJECTION.set(CURRENT_PROJECTION).invert();

        try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(buffer, false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putMat4f(INVERSE_MODEL_VIEW)
                    .putMat4f(INVERSE_PROJECTION)
                    .putMat4f(PREVIOUS_MODEL_VIEW)
                    .putMat4f(PREVIOUS_PROJECTION)
                    .putVec3(CURRENT_CAMERA)
                    .putVec3(PREVIOUS_CAMERA)
                    .putFloat(effectiveStrength())
                    .putInt(samples)
                    .putInt(centered ? 1 : 0)
                    .putInt(useDepth ? 1 : 0);
        } catch (Throwable ignored) {
        }
    }

    /** Хук TAIL WorldRenderer.render: применяем постобработку к готовому кадру. */
    public static void renderWorld(Framebuffer framebuffer, ObjectAllocator objectAllocator) {
        if (!shouldRender() || RELOADING || framebuffer == null) {
            return;
        }
        ObjectAllocator owner = objectAllocator != null ? objectAllocator : allocator;
        if (owner == null) {
            return;
        }

        MinecraftClient client = client();
        if (client == null || client.gameRenderer.isRenderingPanorama()) {
            return;
        }
        ShaderLoader loader = client.getShaderLoader();
        if (loader == null) {
            return;
        }

        PostEffectProcessor postEffectProcessor = getProcessor(loader);
        if (postEffectProcessor == null) {
            return;
        }

        try {
            updateUniforms(postEffectProcessor);
            FrameGraphBuilder graph = new FrameGraphBuilder();
            DefaultFramebufferSet set = new DefaultFramebufferSet();
            set.mainFramebuffer = graph.createObjectNode("main", framebuffer);
            postEffectProcessor.render(graph, framebuffer.textureWidth, framebuffer.textureHeight, set);
            graph.run(owner);
        } catch (Throwable ignored) {
        }
    }
}
