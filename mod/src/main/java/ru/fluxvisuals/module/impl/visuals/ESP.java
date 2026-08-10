package ru.fluxvisuals.module.impl.visuals;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.BufferAllocator;

import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import java.util.OptionalDouble;
import org.joml.Matrix4f;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.render.WorldRenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.render.world.WorldRenderUtil;

@IModule(name = "ESP", description = "Draws highlighted bounding boxes around targets in line of sight", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class ESP extends Module {
    private static final int QUAD_BUFFER_SIZE_BYTES = 1024;
    public static MultiBooleanSetting targets = new MultiBooleanSetting("Targets",
            new BooleanSetting("Players", true), new BooleanSetting("Mobs", true));
    private static final String PIPELINE_NAMESPACE = "fluxvisuals";
    private static final RenderPipeline BOX_FILL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_COLOR_SNIPPET })
                    .withLocation(Identifier.of("minecraft", "rendertype_lequal_depth_test"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build());
    private static final RenderPipeline BOX_LINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_COLOR_SNIPPET })
                    .withLocation(Identifier.of("minecraft", "rendertype_lines"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build());
    private static final RenderLayer BOX_FILL_LAYER = RenderLayer.of(
            "fluxvisuals_esp_box_fill",
            RenderSetup.builder(BOX_FILL_PIPELINE)
                    .expectedBufferSize(1024)
                    .translucent()
                    .build());
    private static final RenderLayer BOX_LINE_LAYER = RenderLayer.of(
            "fluxvisuals_esp_box_line",
            RenderSetup.builder(BOX_LINE_PIPELINE)
                    .expectedBufferSize(1024)
                    .translucent()
                    .build());

    public ESP() {
        this.addSettings(new Setting[] { targets });
    }

    private final BufferAllocator allocator = new BufferAllocator(262144);
    private final Immediate immediate = VertexConsumerProvider.immediate(allocator);

    @EventInit
    public void render(WorldRenderEvent event) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        // Рисуем через WorldRenderer (тот же проход, что и BlockOutline): глубина мира уже в буфере,
        // поэтому боксы честно скрываются за стенами.
        ru.fluxvisuals.util.render.world.WorldRenderer wr = event.worldRenderer();
        for (Entity ent : mc.world.getEntities()) {
            if (this.shouldRender(ent)) {
                // Не рисуем сущность, если её реально не видно (скрыта блоками).
                if (!this.isVisible(ent, event.frameDepth())) {
                    continue;
                }
                this.renderBox(event.matrixStack(), wr.bufferSource(), ent, event.frameDepth());
            }
        }
    }

    /** Луч из глаз игрока в несколько точек сущности: если до любой из них нет блока — сущность видна. */
    private boolean isVisible(Entity target, float partialTicks) {
        if (mc.world == null || mc.player == null) {
            return true;
        }
        Vec3d eye = mc.player.getCameraPosVec(partialTicks);
        Vec3d[] points = new Vec3d[] {
                new Vec3d(target.getX(), target.getY() + 0.1, target.getZ()),
                target.getBoundingBox().getCenter(),
                new Vec3d(target.getX(), target.getEyeY(), target.getZ())
        };
        for (Vec3d point : points) {
            RaycastContext ctx = new RaycastContext(eye, point,
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player);
            net.minecraft.util.hit.BlockHitResult hit = mc.world.raycast(ctx);
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
            // Луч дошёл до точки у самой цели (попал в её бокс) — сущность не за стеной.
            if (hit.getPos().squaredDistanceTo(point) < 1.0) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldRender(Entity entity) {
        if (entity == mc.player) {
            return false;
        } else if (entity instanceof PlayerEntity) {
            return targets.get("Players");
        } else {
            return entity instanceof LivingEntity ? targets.get("Mobs") : false;
        }
    }

    private void renderBox(MatrixStack matrices, Immediate immediate, Entity target, float partialTicks) {
        if (target != null) {
            Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
            double x = target.lastRenderX + (target.getX() - target.lastRenderX) * partialTicks;
            double y = target.lastRenderY + (target.getY() - target.lastRenderY) * partialTicks;
            double z = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * partialTicks;
            Box boundingBox = target.getBoundingBox();
            double padding = 0.08;
            // positionMatrix = вращение камеры + проекция (без трансляции), поэтому координаты
            // должны быть camera-relative (мировые минус позиция камеры).
            double minX = boundingBox.minX - target.getX() + x - padding - cameraPos.x;
            double minY = boundingBox.minY - target.getY() + y - padding - cameraPos.y;
            double minZ = boundingBox.minZ - target.getZ() + z - padding - cameraPos.z;
            double maxX = boundingBox.maxX - target.getX() + x + padding - cameraPos.x;
            double maxY = boundingBox.maxY - target.getY() + y + padding - cameraPos.y;
            double maxZ = boundingBox.maxZ - target.getZ() + z + padding - cameraPos.z;
            float alphaPC = 1.0F;
            int fadeColor = target instanceof AbstractClientPlayerEntity p
                    && FluxVisualsClient.get.friendManager.isFriend(p.getNameForScoreboard()) ? ColorUtil.GREEN : ColorUtil.fade();
            int baseColor = ColorUtil.multAlpha(fadeColor, alphaPC);
            int color1 = ColorUtil.multDark(baseColor, 0.1F);
            int color2 = ColorUtil.multDark(baseColor, 1.0F);
            int color3 = ColorUtil.multDark(baseColor, 0.1F);
            int color4 = ColorUtil.multDark(baseColor, 1.0F);
            int[] gradientColors = new int[] {
                    ColorUtil.gradient(color1, color2, 0, 7),
                    ColorUtil.gradient(color2, color3, 90, 7),
                    ColorUtil.gradient(color3, color4, 180, 7),
                    ColorUtil.gradient(color4, color1, 270, 7)
            };
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            VertexConsumer fillBuffer = immediate.getBuffer(BOX_FILL_LAYER);
            WorldRenderUtil.drawBoxFill(fillBuffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, gradientColors, 85);
            VertexConsumer lineBuffer = immediate.getBuffer(BOX_LINE_LAYER);
            WorldRenderUtil.drawBoxOutline(lineBuffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, gradientColors, 255,
                    0.15, 0.08);
        }
    }
}