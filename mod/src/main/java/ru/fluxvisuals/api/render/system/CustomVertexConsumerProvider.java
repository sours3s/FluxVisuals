package ru.fluxvisuals.api.render.system;

import ru.fluxvisuals.api.render.system.ClientPipelines;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public class CustomVertexConsumerProvider implements VertexConsumerProvider {
    private final VertexConsumerProvider.Immediate parent;
    private final VertexConsumerProvider.Immediate plainDrawer = VertexConsumerProvider.immediate(new BufferAllocator(1536));
    private int red = 255;
    private int green = 255;
    private int blue = 255;
    private int alpha = 255;

    public CustomVertexConsumerProvider(VertexConsumerProvider.Immediate parent) {
        this.parent = parent;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer renderLayer) {
        RenderLayer colorLayer = ClientPipelines.QUAD;

        VertexConsumer vertexConsumer = this.parent.getBuffer(renderLayer);
        VertexConsumer vertexConsumer2 = this.plainDrawer.getBuffer(colorLayer);

        CustomVertexConsumerProvider.OutlineVertexConsumer outlineVertexConsumer = new CustomVertexConsumerProvider.OutlineVertexConsumer(
                vertexConsumer2, this.red, this.green, this.blue, this.alpha
        );

        return VertexConsumers.union(outlineVertexConsumer, vertexConsumer);
    }

    public void setColor(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public void draw() {
        this.plainDrawer.draw();
    }

    @Environment(EnvType.CLIENT)
    record OutlineVertexConsumer(VertexConsumer delegate, int argbColor) implements VertexConsumer {
        public OutlineVertexConsumer(VertexConsumer delegate, int red, int green, int blue, int alpha) {
            this(delegate, ColorHelper.getArgb(alpha, red, green, blue));
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            this.delegate.vertex(x, y, z).color(this.argbColor);
            return this;
        }

        @Override
        public VertexConsumer color(int argb) {
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            this.delegate.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            this.delegate.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            this.delegate.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.delegate.normal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer lineWidth(float width) {
            this.delegate.lineWidth(width);
            return this;
        }
    }
}