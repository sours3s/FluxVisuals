package ru.fluxvisuals.ui.gui.clickgui.externall.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ItemReplacerSpecialRenderer implements SpecialModelRenderer<ItemStack> {

    private static final Map<Identifier, ItemReplacerSpecialRenderer> CACHE = new ConcurrentHashMap<>();

    private final Identifier textureId;

    public static ItemReplacerSpecialRenderer of(Identifier textureId) {
        return CACHE.computeIfAbsent(textureId, ItemReplacerSpecialRenderer::new);
    }

    public ItemReplacerSpecialRenderer(Identifier textureId) {
        this.textureId = textureId;
    }

    @Override
    public void render(ItemStack stack, ItemDisplayContext displayContext, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, boolean glint, int seed) {
        RenderLayer layer = RenderLayers.entityCutoutNoCull(textureId);
        queue.submitCustom(matrices, layer, (entry, consumer) -> {

            putQuad(consumer, entry,
                    0.0f, 0.0f, 0.53125f, 0.0f, 1.0f,
                    1.0f, 0.0f, 0.53125f, 1.0f, 1.0f,
                    1.0f, 1.0f, 0.53125f, 1.0f, 0.0f,
                    0.0f, 1.0f, 0.53125f, 0.0f, 0.0f,
                    overlay, light, 0, 0, 1);

            putQuad(consumer, entry,
                    1.0f, 0.0f, 0.46875f, 1.0f, 1.0f,
                    0.0f, 0.0f, 0.46875f, 0.0f, 1.0f,
                    0.0f, 1.0f, 0.46875f, 0.0f, 0.0f,
                    1.0f, 1.0f, 0.46875f, 1.0f, 0.0f,
                    overlay, light, 0, 0, -1);

            putQuad(consumer, entry,
                    0.0f, 1.0f, 0.53125f, 0.0f, 0.0f,
                    1.0f, 1.0f, 0.53125f, 1.0f, 0.0f,
                    1.0f, 1.0f, 0.46875f, 1.0f, 0.0f,
                    0.0f, 1.0f, 0.46875f, 0.0f, 0.0f,
                    overlay, light, 0, 1, 0);

            putQuad(consumer, entry,
                    0.0f, 0.0f, 0.46875f, 0.0f, 1.0f,
                    1.0f, 0.0f, 0.46875f, 1.0f, 1.0f,
                    1.0f, 0.0f, 0.53125f, 1.0f, 1.0f,
                    0.0f, 0.0f, 0.53125f, 0.0f, 1.0f,
                    overlay, light, 0, -1, 0);

            putQuad(consumer, entry,
                    0.0f, 0.0f, 0.46875f, 0.0f, 1.0f,
                    0.0f, 0.0f, 0.53125f, 0.0f, 1.0f,
                    0.0f, 1.0f, 0.53125f, 0.0f, 0.0f,
                    0.0f, 1.0f, 0.46875f, 0.0f, 0.0f,
                    overlay, light, -1, 0, 0);

            putQuad(consumer, entry,
                    1.0f, 0.0f, 0.53125f, 1.0f, 1.0f,
                    1.0f, 0.0f, 0.46875f, 1.0f, 1.0f,
                    1.0f, 1.0f, 0.46875f, 1.0f, 0.0f,
                    1.0f, 1.0f, 0.53125f, 1.0f, 0.0f,
                    overlay, light, 1, 0, 0);
        });
    }

    private static void putQuad(VertexConsumer consumer, MatrixStack.Entry entry,
                                float x0, float y0, float z0, float u0, float v0,
                                float x1, float y1, float z1, float u1, float v1,
                                float x2, float y2, float z2, float u2, float v2,
                                float x3, float y3, float z3, float u3, float v3,
                                int overlay, int light, float nx, float ny, float nz) {
        consumer.vertex(entry, x0, y0, z0).color(255, 255, 255, 255).texture(u0, v0).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        consumer.vertex(entry, x1, y1, z1).color(255, 255, 255, 255).texture(u1, v1).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        consumer.vertex(entry, x2, y2, z2).color(255, 255, 255, 255).texture(u2, v2).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        consumer.vertex(entry, x3, y3, z3).color(255, 255, 255, 255).texture(u3, v3).overlay(overlay).light(light).normal(entry, nx, ny, nz);
    }

    @Override
    public void collectVertices(Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(0.0f, 0.0f, 0.46875f));
        consumer.accept(new Vector3f(1.0f, 1.0f, 0.53125f));
    }

    @Override
    public ItemStack getData(ItemStack stack) {
        return stack;
    }
}

