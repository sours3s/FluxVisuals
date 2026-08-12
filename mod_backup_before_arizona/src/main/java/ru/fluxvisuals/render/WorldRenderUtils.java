package ru.fluxvisuals.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

/** 3D-отрисовка в мире (координаты мира → камера). Все методы используют RenderLayers.LINES (POSITION_COLOR_NORMAL_LINE_WIDTH). */
public final class WorldRenderUtils {
    private static final float LINE_W = 2f;

    private WorldRenderUtils() {}

    /** Позиция сущности (getPos в 1.21.11 отсутствует). */
    public static Vec3d entityPos(Entity entity) {
        return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
    }

    public static void drawLine3D(MatrixStack ms, VertexConsumerProvider vcp, Camera camera, Vec3d from, Vec3d to, int color) {
        VertexConsumer vc = vcp.getBuffer(RenderLayers.LINES);
        Matrix4f mat = ms.peek().getPositionMatrix();
        Vec3d c = camera.getCameraPos();
        float x1 = (float) (from.x - c.x), y1 = (float) (from.y - c.y), z1 = (float) (from.z - c.z);
        float x2 = (float) (to.x - c.x), y2 = (float) (to.y - c.y), z2 = (float) (to.z - c.z);
        int r = ColorUtils.red(color), g = ColorUtils.green(color), b = ColorUtils.blue(color), a = ColorUtils.alpha(color);
        vc.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(0f, 1f, 0f).lineWidth(LINE_W);
        vc.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(0f, 1f, 0f).lineWidth(LINE_W);
    }

    /** Горизонтальное кольцо в плоскости XZ вокруг центра. */
    public static void drawRing3D(MatrixStack ms, VertexConsumerProvider vcp, Camera camera, Vec3d center, float radius, float phase, int color, int segments) {
        VertexConsumer vc = vcp.getBuffer(RenderLayers.LINES);
        Matrix4f mat = ms.peek().getPositionMatrix();
        Vec3d c = camera.getCameraPos();
        int r = ColorUtils.red(color), g = ColorUtils.green(color), b = ColorUtils.blue(color), a = ColorUtils.alpha(color);
        float cy = (float) (center.y - c.y);
        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * 2 * Math.PI / segments) + phase;
            float a2 = (float) ((i + 1) * 2 * Math.PI / segments) + phase;
            float x1 = (float) (center.x + radius * Math.cos(a1) - c.x);
            float z1 = (float) (center.z + radius * Math.sin(a1) - c.z);
            float x2 = (float) (center.x + radius * Math.cos(a2) - c.x);
            float z2 = (float) (center.z + radius * Math.sin(a2) - c.z);
            vc.vertex(mat, x1, cy, z1).color(r, g, b, a).normal(0f, 1f, 0f).lineWidth(LINE_W);
            vc.vertex(mat, x2, cy, z2).color(r, g, b, a).normal(0f, 1f, 0f).lineWidth(LINE_W);
        }
    }

    /** Билборд-текст в мире (всегда повёрнут к камере). */
    public static void drawBillboardText(MatrixStack ms, VertexConsumerProvider vcp, Camera camera, TextRenderer font,
                                         String text, Vec3d pos, int color) {
        ms.push();
        Vec3d camPos = camera.getCameraPos();
        ms.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        float w = font.getWidth(text);
        font.draw(text, -w / 2f, 0f, color, true, ms.peek().getPositionMatrix(),
                vcp, TextRenderer.TextLayerType.NORMAL, 0x00000000, 0xF000F0);
        ms.pop();
    }

    /** Билборд-полоса: заполненный прямоугольник в мире из вертикальных линий (нет GUI-quad в 1.21.11). */
    public static void drawBillboardBar(MatrixStack ms, VertexConsumerProvider vcp, Camera camera,
                                         Vec3d pos, float width, float height, int color) {
        ms.push();
        Vec3d camPos = camera.getCameraPos();
        ms.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        Matrix4f mat = ms.peek().getPositionMatrix();
        int r = ColorUtils.red(color), g = ColorUtils.green(color), b = ColorUtils.blue(color), a = ColorUtils.alpha(color);
        VertexConsumer vc = vcp.getBuffer(RenderLayers.LINES);
        int segs = Math.max(1, (int) (width / 1.5f));
        float hw = width / 2f;
        for (int i = 0; i <= segs; i++) {
            float x = -hw + width * i / (float) segs;
            vc.vertex(mat, x, 0f, 0f).color(r, g, b, a).normal(0f, 1f, 0f).lineWidth(1f);
            vc.vertex(mat, x, height, 0f).color(r, g, b, a).normal(0f, 1f, 0f).lineWidth(1f);
        }
        ms.pop();
    }

    /** Каркас AABB-бокса (12 рёбер). */
    public static void drawBoxOutline3D(MatrixStack ms, VertexConsumerProvider vcp, Camera camera, Box box, int color) {
        Vec3d[] corners = {
                new Vec3d(box.minX, box.minY, box.minZ), new Vec3d(box.maxX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.maxZ), new Vec3d(box.minX, box.minY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.minZ), new Vec3d(box.maxX, box.maxY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.maxZ), new Vec3d(box.minX, box.maxY, box.maxZ)
        };
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for (int[] e : edges) {
            drawLine3D(ms, vcp, camera, corners[e[0]], corners[e[1]], color);
        }
    }
}
