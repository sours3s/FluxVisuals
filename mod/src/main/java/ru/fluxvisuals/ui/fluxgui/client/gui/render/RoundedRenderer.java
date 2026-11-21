package ru.fluxvisuals.ui.fluxgui.client.gui.render;

import ru.fluxvisuals.ui.fluxgui.compat.Gui;
import ru.fluxvisuals.ui.fluxgui.compat.Gui$Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fc;

public class RoundedRenderer {
    private static final int MIN_SEGMENTS = 4;
    private static final int SEGMENTS = 12;
    private static final float[][][] ARC_COS = new float[4][13][13];
    private static final float[][][] ARC_SIN = new float[4][13][13];

    private static float edgeFeather(DrawContext context) {
        return 0.55F;
    }

    private static float edgeFeather(DrawContext context, float width, float height) {
        Matrix3x2fc matrix = context.getMatrices();
        float determinant = Math.abs(matrix.m00() * matrix.m11() - matrix.m01() * matrix.m10());
        float scale = (float)Math.sqrt(determinant);
        float baseFeather = scale > 1.0E-4F ? 0.55F / scale : 0.55F;
        float maxFeather = Math.min(width, height) * 0.2F;
        return Math.max(0.08F, Math.min(baseFeather, maxFeather));
    }

    public static void drawRoundedRect(DrawContext context, float x, float y, float width, float height, float radius, int color) {
        drawRoundedRect(context, x, y, width, height, radius, color, true, true, true, true);
    }

    public static void drawRoundedRect(
        DrawContext context,
        float x,
        float y,
        float width,
        float height,
        float radius,
        int color,
        boolean roundTopLeft,
        boolean roundTopRight,
        boolean roundBottomRight,
        boolean roundBottomLeft
    ) {
        if (!(width <= 0.0F) && !(height <= 0.0F) && (color >>> 24 & 0xFF) != 0) {
            if (radius <= 0.0F) {
                context.fill((int)x, (int)y, (int)(x + width), (int)(y + height), color);
            } else {
                float alpha = (color >> 24 & 0xFF) / 255.0F;
                float red = (color >> 16 & 0xFF) / 255.0F;
                float green = (color >> 8 & 0xFF) / 255.0F;
                float blue = (color & 0xFF) / 255.0F;
                float feather = edgeFeather(context, width, height);
                float maximumRadius = Math.min(width, height) * 0.5F;
                radius = Math.min(radius, maximumRadius);
                if (radius >= maximumRadius - 0.001F) {
                    radius = Math.max(0.0F, maximumRadius - Math.min(0.28F, feather * 0.45F));
                }

                float innerR = Math.max(0.0F, radius - feather);
                int estimatedSegments = segmentsForRadius(radius + feather);
                Gui$Color gc = Gui.color(context, estimatedSegments * 48 + 96);
                float centerX = x + width / 2.0F;
                float centerY = y + height / 2.0F;
                gc.fanVertex(centerX, centerY, red, green, blue, alpha);
                if (roundTopRight) {
                    appendCorner(gc, x + width - radius, y + radius, innerR, 0, red, green, blue, alpha);
                } else {
                    gc.fanVertex(x + width, y, red, green, blue, alpha);
                }

                if (roundTopLeft) {
                    appendCorner(gc, x + radius, y + radius, innerR, -1, red, green, blue, alpha);
                } else {
                    gc.fanVertex(x, y, red, green, blue, alpha);
                }

                if (roundBottomLeft) {
                    appendCorner(gc, x + radius, y + height - radius, innerR, -2, red, green, blue, alpha);
                } else {
                    gc.fanVertex(x, y + height, red, green, blue, alpha);
                }

                if (roundBottomRight) {
                    appendCorner(gc, x + width - radius, y + height - radius, innerR, -3, red, green, blue, alpha);
                } else {
                    gc.fanVertex(x + width, y + height, red, green, blue, alpha);
                }

                if (roundTopRight) {
                    float startPx = x + width - radius + innerR;
                    float startPy = y + radius;
                    gc.fanVertex(startPx, startPy, red, green, blue, alpha);
                } else {
                    gc.fanVertex(x + width, y, red, green, blue, alpha);
                }

                appendSmoothAA(gc, x, y, width, height, radius, color, color, false, feather);
                gc.submit(context);
            }
        }
    }

    private static void appendCorner(Gui$Color gc, float cx, float cy, float r, int quadrant, float red, float green, float blue, float alpha) {
        int segments = segmentsForRadius(r);

        for (int i = 0; i <= segments; i++) {
            float px = cx + arcCos(quadrant, segments, i) * r;
            float py = cy + arcSin(quadrant, segments, i) * r;
            gc.fanVertex(px, py, red, green, blue, alpha);
        }
    }

    public static void drawAnimatedGradientRoundedRect(DrawContext context, float x, float y, float width, float height, float radius, int color1, int color2) {
        float innerR = Math.max(0.0F, radius - 0.5F);
        Gui$Color gc = Gui.color(context);
        float centerX = x + width / 2.0F;
        float centerY = y + height / 2.0F;
        appendAnimVertex(gc, centerX, centerY, color1, color2, 1.0F);
        float trCx = x + width - radius;
        float trCy = y + radius;

        for (int i = 0; i <= 12; i++) {
            float angle = (float)(0.0 - (Math.PI / 2) * i / 12.0);
            appendAnimVertex(gc, trCx + MathHelper.cos(angle) * innerR, trCy + MathHelper.sin(angle) * innerR, color1, color2, 1.0F);
        }

        float tlCx = x + radius;
        float tlCy = y + radius;

        for (int i = 0; i <= 12; i++) {
            float angle = (float)((-Math.PI / 2) - (Math.PI / 2) * i / 12.0);
            appendAnimVertex(gc, tlCx + MathHelper.cos(angle) * innerR, tlCy + MathHelper.sin(angle) * innerR, color1, color2, 1.0F);
        }

        float blCx = x + radius;
        float blCy = y + height - radius;

        for (int i = 0; i <= 12; i++) {
            float angle = (float)(-Math.PI - (Math.PI / 2) * i / 12.0);
            appendAnimVertex(gc, blCx + MathHelper.cos(angle) * innerR, blCy + MathHelper.sin(angle) * innerR, color1, color2, 1.0F);
        }

        float brCx = x + width - radius;
        float brCy = y + height - radius;

        for (int i = 0; i <= 12; i++) {
            float angle = (float)((-Math.PI * 3.0 / 2.0) - (Math.PI / 2) * i / 12.0);
            appendAnimVertex(gc, brCx + MathHelper.cos(angle) * innerR, brCy + MathHelper.sin(angle) * innerR, color1, color2, 1.0F);
        }

        appendAnimVertex(gc, trCx + innerR, trCy, color1, color2, 1.0F);
        gc.submit(context);
        drawSmoothAA(context, x, y, width, height, radius, color1, color2, true, edgeFeather(context));
    }

    public static void drawVerticalGradientRoundedRect(
        DrawContext context, float x, float y, float width, float height, float radius, int colorTop, int colorBottom
    ) {
        float innerR = Math.max(0.0F, radius - 0.5F);
        Gui$Color gc = Gui.color(context);
        float centerX = x + width / 2.0F;
        float centerY = y + height / 2.0F;
        appendVertGradVertex(gc, centerX, centerY, y, height, colorTop, colorBottom);
        float trCx = x + width - radius;
        float trCy = y + radius;

        for (int i = 0; i <= 12; i++) {
            float angle = (float)(0.0 - (Math.PI / 2) * i / 12.0);
            appendVertGradVertex(gc, trCx + MathHelper.cos(angle) * innerR, trCy + MathHelper.sin(angle) * innerR, y, height, colorTop, colorBottom);
        }

        float tlCx = x + radius;
        float tlCy = y + radius;

        for (int i = 0; i <= 12; i++) {
            float angle = (float)((-Math.PI / 2) - (Math.PI / 2) * i / 12.0);
            appendVertGradVertex(gc, tlCx + MathHelper.cos(angle) * innerR, tlCy + MathHelper.sin(angle) * innerR, y, height, colorTop, colorBottom);
        }

        float blCx = x + radius;
        float blCy = y + height - radius;

        for (int i = 0; i <= 12; i++) {
            float angle = (float)(-Math.PI - (Math.PI / 2) * i / 12.0);
            appendVertGradVertex(gc, blCx + MathHelper.cos(angle) * innerR, blCy + MathHelper.sin(angle) * innerR, y, height, colorTop, colorBottom);
        }

        float brCx = x + width - radius;
        float brCy = y + height - radius;

        for (int i = 0; i <= 12; i++) {
            float angle = (float)((-Math.PI * 3.0 / 2.0) - (Math.PI / 2) * i / 12.0);
            appendVertGradVertex(gc, brCx + MathHelper.cos(angle) * innerR, brCy + MathHelper.sin(angle) * innerR, y, height, colorTop, colorBottom);
        }

        appendVertGradVertex(gc, trCx + innerR, trCy, y, height, colorTop, colorBottom);
        gc.submit(context);
        drawVerticalGradientSmoothAA(context, x, y, width, height, radius, colorTop, colorBottom);
    }

    public static void drawFourCornerGradientRoundedRect(
        DrawContext context,
        float x,
        float y,
        float width,
        float height,
        float radius,
        int colorTopLeft,
        int colorTopRight,
        int colorBottomRight,
        int colorBottomLeft
    ) {
        if (!(width <= 0.0F) && !(height <= 0.0F)) {
            float innerR = Math.max(0.0F, radius - 0.5F);
            Gui$Color gc = Gui.color(context);
            float centerX = x + width / 2.0F;
            float centerY = y + height / 2.0F;
            appendFourCornerVertex(gc, centerX, centerY, x, y, width, height, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
            float[][] corners = new float[][]{
                {x + width - radius, y + radius, 0.0F},
                {x + radius, y + radius, -1.0F},
                {x + radius, y + height - radius, -2.0F},
                {x + width - radius, y + height - radius, -3.0F}
            };

            for (float[] corner : corners) {
                for (int i = 0; i <= 12; i++) {
                    float angle = (float)(corner[2] * Math.PI / 2.0 - (Math.PI / 2) * i / 12.0);
                    float px = corner[0] + MathHelper.cos(angle) * innerR;
                    float py = corner[1] + MathHelper.sin(angle) * innerR;
                    appendFourCornerVertex(gc, px, py, x, y, width, height, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
                }
            }

            appendFourCornerVertex(
                gc, x + width - radius + innerR, y + radius, x, y, width, height, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft
            );
            gc.submit(context);
            float feather = Math.min(Math.min(width, height) * 0.12F, Math.max(0.65F, edgeFeather(context, width, height) * 2.4F));
            drawSmoothAA(context, x, y, width, height, radius, colorTopLeft, colorTopLeft, false, feather);
        }
    }

    private static void appendFourCornerVertex(
        Gui$Color gc,
        float px,
        float py,
        float x,
        float y,
        float width,
        float height,
        int colorTopLeft,
        int colorTopRight,
        int colorBottomRight,
        int colorBottomLeft
    ) {
        float u = MathHelper.clamp((px - x) / Math.max(0.001F, width), 0.0F, 1.0F);
        float v = MathHelper.clamp((py - y) / Math.max(0.001F, height), 0.0F, 1.0F);
        int top = interpolateColor(colorTopLeft, colorTopRight, u);
        int bottom = interpolateColor(colorBottomLeft, colorBottomRight, u);
        gc.fanVertex(px, py, interpolateColor(top, bottom, v));
    }

    public static void drawRotatingGradientRoundedRect(
        DrawContext context, float x, float y, float width, float height, float radius, int colorDark, int colorLight, float speed
    ) {
        if (!(width <= 0.0F) && !(height <= 0.0F)) {
            if (colorLuminance(colorDark) > colorLuminance(colorLight)) {
                int swap = colorDark;
                colorDark = colorLight;
                colorLight = swap;
            }

            int edgeBase = interpolateColor(colorDark, colorLight, 0.5F);
            int edgeBaseAlpha = Math.round((edgeBase >>> 24 & 0xFF) * 0.16F);
            drawRoundedRect(context, x, y, width, height, radius, edgeBaseAlpha << 24 | edgeBase & 16777215);
            float innerR = Math.max(0.0F, radius - 0.5F);
            float centerX = x + width / 2.0F;
            float centerY = y + height / 2.0F;
            float halfWidth = Math.max(0.5F, width / 2.0F);
            float halfHeight = Math.max(0.5F, height / 2.0F);
            float speedMultiplier = Math.max(1.0F, speed);
            float angle = (float)((Math.PI / 2) + System.currentTimeMillis() % 140000L / 14000.0 * speedMultiplier * Math.PI * 2.0);
            float dirX = MathHelper.cos(angle);
            float dirY = MathHelper.sin(angle);
            Gui$Color gc = Gui.color(context);
            appendRotatingGradientVertex(gc, centerX, centerY, centerX, centerY, halfWidth, halfHeight, dirX, dirY, colorDark, colorLight);
            float[][] corners = new float[][]{
                {x + width - radius, y + radius, 0.0F},
                {x + radius, y + radius, -1.0F},
                {x + radius, y + height - radius, -2.0F},
                {x + width - radius, y + height - radius, -3.0F}
            };

            for (float[] corner : corners) {
                for (int i = 0; i <= 12; i++) {
                    float vertexAngle = (float)(corner[2] * Math.PI / 2.0 - (Math.PI / 2) * i / 12.0);
                    float px = corner[0] + MathHelper.cos(vertexAngle) * innerR;
                    float py = corner[1] + MathHelper.sin(vertexAngle) * innerR;
                    appendRotatingGradientVertex(gc, px, py, centerX, centerY, halfWidth, halfHeight, dirX, dirY, colorDark, colorLight);
                }
            }

            appendRotatingGradientVertex(
                gc, x + width - radius + innerR, y + radius, centerX, centerY, halfWidth, halfHeight, dirX, dirY, colorDark, colorLight
            );
            gc.submit(context);
        }
    }

    private static void appendRotatingGradientVertex(
        Gui$Color gc,
        float px,
        float py,
        float centerX,
        float centerY,
        float halfWidth,
        float halfHeight,
        float dirX,
        float dirY,
        int colorDark,
        int colorLight
    ) {
        float nx = (px - centerX) / halfWidth;
        float ny = (py - centerY) / halfHeight;
        float projectionRange = Math.max(0.001F, Math.abs(dirX) + Math.abs(dirY));
        float directionalFactor = MathHelper.clamp(0.5F + 0.5F * (nx * dirX + ny * dirY) / projectionRange, 0.0F, 1.0F);
        directionalFactor = directionalFactor * directionalFactor * (3.0F - 2.0F * directionalFactor);
        float edgeWeight = MathHelper.clamp((Math.max(Math.abs(nx), Math.abs(ny)) - 0.28F) / 0.72F, 0.0F, 1.0F);
        edgeWeight = edgeWeight * edgeWeight * (3.0F - 2.0F * edgeWeight);
        float factor = 0.1F + (directionalFactor - 0.1F) * edgeWeight;
        gc.fanVertex(px, py, interpolateColor(colorDark, colorLight, factor));
    }

    private static float colorLuminance(int color) {
        return (color >> 16 & 0xFF) * 0.2126F + (color >> 8 & 0xFF) * 0.7152F + (color & 0xFF) * 0.0722F;
    }

    private static void drawVerticalGradientSmoothAA(
        DrawContext context, float x, float y, float width, float height, float radius, int colorTop, int colorBottom
    ) {
        float feather = Math.min(Math.min(width, height) * 0.12F, Math.max(0.65F, edgeFeather(context, width, height) * 2.4F));
        float rIn = Math.max(0.0F, radius - feather);
        float rOut = radius + feather;
        Gui$Color gc = Gui.color(context, segmentsForRadius(rOut) * 32 + 80);
        float trCx = x + width - radius;
        float trCy = y + radius;
        float tlCx = x + radius;
        float tlCy = y + radius;
        float blCx = x + radius;
        float blCy = y + height - radius;
        float brCx = x + width - radius;
        float brCy = y + height - radius;
        appendVerticalGradientAAArc(gc, trCx, trCy, rIn, rOut, 0, y, height, colorTop, colorBottom);
        appendVerticalGradientAAEdge(gc, trCx, trCy - rIn, trCx, trCy - rOut, tlCx, tlCy - rIn, tlCx, tlCy - rOut, y, height, colorTop, colorBottom);
        appendVerticalGradientAAArc(gc, tlCx, tlCy, rIn, rOut, -1, y, height, colorTop, colorBottom);
        appendVerticalGradientAAEdge(gc, tlCx - rIn, tlCy, tlCx - rOut, tlCy, blCx - rIn, blCy, blCx - rOut, blCy, y, height, colorTop, colorBottom);
        appendVerticalGradientAAArc(gc, blCx, blCy, rIn, rOut, -2, y, height, colorTop, colorBottom);
        appendVerticalGradientAAEdge(gc, blCx, blCy + rIn, blCx, blCy + rOut, brCx, brCy + rIn, brCx, brCy + rOut, y, height, colorTop, colorBottom);
        appendVerticalGradientAAArc(gc, brCx, brCy, rIn, rOut, -3, y, height, colorTop, colorBottom);
        appendVerticalGradientAAEdge(gc, brCx + rIn, brCy, brCx + rOut, brCy, trCx + rIn, trCy, trCx + rOut, trCy, y, height, colorTop, colorBottom);
        appendVerticalGradientAAVertex(gc, trCx + rIn, trCy, y, height, colorTop, colorBottom, 1.0F);
        appendVerticalGradientAAVertex(gc, trCx + rOut, trCy, y, height, colorTop, colorBottom, 0.0F);
        gc.submit(context);
    }

    private static void appendVerticalGradientAAArc(
        Gui$Color gc, float cx, float cy, float rIn, float rOut, int quadrant, float startY, float height, int colorTop, int colorBottom
    ) {
        for (int i = 0; i <= 12; i++) {
            double angle = quadrant * Math.PI / 2.0 - (Math.PI / 2) * i / 12.0;
            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);
            appendVerticalGradientAAVertex(gc, cx + cos * rIn, cy + sin * rIn, startY, height, colorTop, colorBottom, 1.0F);
            appendVerticalGradientAAVertex(gc, cx + cos * rOut, cy + sin * rOut, startY, height, colorTop, colorBottom, 0.0F);
        }
    }

    private static void appendVerticalGradientAAEdge(
        Gui$Color gc,
        float inX1,
        float inY1,
        float outX1,
        float outY1,
        float inX2,
        float inY2,
        float outX2,
        float outY2,
        float startY,
        float height,
        int colorTop,
        int colorBottom
    ) {
        appendVerticalGradientAAVertex(gc, inX2, inY2, startY, height, colorTop, colorBottom, 1.0F);
        appendVerticalGradientAAVertex(gc, outX2, outY2, startY, height, colorTop, colorBottom, 0.0F);
    }

    private static void appendVerticalGradientAAVertex(
        Gui$Color gc, float px, float py, float startY, float height, int colorTop, int colorBottom, float alphaMult
    ) {
        float factor = (py - startY) / height;
        factor = MathHelper.clamp(factor, 0.0F, 1.0F);
        int a1 = colorTop >> 24 & 0xFF;
        int r1 = colorTop >> 16 & 0xFF;
        int g1 = colorTop >> 8 & 0xFF;
        int b1 = colorTop & 0xFF;
        int a2 = colorBottom >> 24 & 0xFF;
        int r2 = colorBottom >> 16 & 0xFF;
        int g2 = colorBottom >> 8 & 0xFF;
        int b2 = colorBottom & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * factor);
        int r = Math.round(r1 + (r2 - r1) * factor);
        int g = Math.round(g1 + (g2 - g1) * factor);
        int b = Math.round(b1 + (b2 - b1) * factor);
        float finalAlpha = a / 255.0F * alphaMult;
        gc.stripVertex(px, py, r / 255.0F, g / 255.0F, b / 255.0F, finalAlpha);
    }

    private static void drawSmoothAA(
        DrawContext context, float x, float y, float width, float height, float radius, int c1, int c2, boolean animated, float feather
    ) {
        Gui$Color gc = Gui.color(context, segmentsForRadius(radius + feather) * 32 + 80);
        appendSmoothAA(gc, x, y, width, height, radius, c1, c2, animated, feather);
        gc.submit(context);
    }

    private static void appendSmoothAA(Gui$Color gc, float x, float y, float width, float height, float radius, int c1, int c2, boolean animated, float feather) {
        float rIn = Math.max(0.0F, radius - feather);
        float rOut = radius + feather;
        float trCx = x + width - radius;
        float trCy = y + radius;
        float tlCx = x + radius;
        float tlCy = y + radius;
        float blCx = x + radius;
        float blCy = y + height - radius;
        float brCx = x + width - radius;
        float brCy = y + height - radius;
        appendAAArc(gc, trCx, trCy, rIn, rOut, 0, c1, c2, animated);
        appendAAEdge(gc, trCx, trCy - rIn, trCx, trCy - rOut, tlCx, tlCy - rIn, tlCx, tlCy - rOut, c1, c2, animated);
        appendAAArc(gc, tlCx, tlCy, rIn, rOut, -1, c1, c2, animated);
        appendAAEdge(gc, tlCx - rIn, tlCy, tlCx - rOut, tlCy, blCx - rIn, blCy, blCx - rOut, blCy, c1, c2, animated);
        appendAAArc(gc, blCx, blCy, rIn, rOut, -2, c1, c2, animated);
        appendAAEdge(gc, blCx, blCy + rIn, blCx, blCy + rOut, brCx, brCy + rIn, brCx, brCy + rOut, c1, c2, animated);
        appendAAArc(gc, brCx, brCy, rIn, rOut, -3, c1, c2, animated);
        appendAAEdge(gc, brCx + rIn, brCy, brCx + rOut, brCy, trCx + rIn, trCy, trCx + rOut, trCy, c1, c2, animated);
        appendAAVertex(gc, trCx + rIn, trCy, c1, c2, 1.0F, animated);
        appendAAVertex(gc, trCx + rOut, trCy, c1, c2, 0.0F, animated);
    }

    private static void appendAAArc(Gui$Color gc, float cx, float cy, float rIn, float rOut, int quadrant, int c1, int c2, boolean animated) {
        int segments = segmentsForRadius(Math.max(rIn, rOut));

        for (int i = 0; i <= segments; i++) {
            float cos = arcCos(quadrant, segments, i);
            float sin = arcSin(quadrant, segments, i);
            appendAAVertex(gc, cx + cos * rIn, cy + sin * rIn, c1, c2, 1.0F, animated);
            appendAAVertex(gc, cx + cos * rOut, cy + sin * rOut, c1, c2, 0.0F, animated);
        }
    }

    private static void appendAAEdge(
        Gui$Color gc, float inX1, float inY1, float outX1, float outY1, float inX2, float inY2, float outX2, float outY2, int c1, int c2, boolean animated
    ) {
        appendAAVertex(gc, inX2, inY2, c1, c2, 1.0F, animated);
        appendAAVertex(gc, outX2, outY2, c1, c2, 0.0F, animated);
    }

    private static void appendAAVertex(Gui$Color gc, float px, float py, int c1, int c2, float alphaMult, boolean animated) {
        if (animated) {
            appendAnimVertexStrip(gc, px, py, c1, c2, alphaMult);
        } else {
            float baseAlpha = (c1 >> 24 & 0xFF) / 255.0F;
            float r = (c1 >> 16 & 0xFF) / 255.0F;
            float g = (c1 >> 8 & 0xFF) / 255.0F;
            float b = (c1 & 0xFF) / 255.0F;
            gc.stripVertex(px, py, r, g, b, baseAlpha * alphaMult);
        }
    }

    private static void appendAnimVertex(Gui$Color gc, float px, float py, int c1, int c2, float alphaMult) {
        gc.fanVertex(px, py, animColor(px, py, c1, c2, alphaMult));
    }

    private static void appendAnimVertexStrip(Gui$Color gc, float px, float py, int c1, int c2, float alphaMult) {
        gc.stripVertex(px, py, animColor(px, py, c1, c2, alphaMult));
    }

    private static int animColor(float px, float py, int c1, int c2, float alphaMult) {
        double time = System.currentTimeMillis() / 25.0;
        double scale = 0.003;
        double noise = Math.sin(px * scale + time * 0.03) + Math.cos(py * scale + time * 0.02) + Math.sin((px + py) * scale * 0.5 - time * 0.01);
        float factor = (float)((noise + 2.0) / 4.0);
        factor = MathHelper.clamp(factor, 0.0F, 1.0F);
        int a1 = c1 >> 24 & 0xFF;
        int r1 = c1 >> 16 & 0xFF;
        int g1 = c1 >> 8 & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = c2 >> 24 & 0xFF;
        int r2 = c2 >> 16 & 0xFF;
        int g2 = c2 >> 8 & 0xFF;
        int b2 = c2 & 0xFF;
        int a = (int)(a1 + (a2 - a1) * factor);
        int r = (int)(r1 + (r2 - r1) * factor);
        int g = (int)(g1 + (g2 - g1) * factor);
        int b = (int)(b1 + (b2 - b1) * factor);
        int finalAlpha = MathHelper.clamp(Math.round(a * alphaMult), 0, 255);
        return finalAlpha << 24 | r << 16 | g << 8 | b;
    }

    private static void appendVertGradVertex(Gui$Color gc, float px, float py, float startY, float height, int c1, int c2) {
        float factor = (py - startY) / height;
        factor = MathHelper.clamp(factor, 0.0F, 1.0F);
        int a1 = c1 >> 24 & 0xFF;
        int r1 = c1 >> 16 & 0xFF;
        int g1 = c1 >> 8 & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = c2 >> 24 & 0xFF;
        int r2 = c2 >> 16 & 0xFF;
        int g2 = c2 >> 8 & 0xFF;
        int b2 = c2 & 0xFF;
        int a = (int)(a1 + (a2 - a1) * factor);
        int r = (int)(r1 + (r2 - r1) * factor);
        int g = (int)(g1 + (g2 - g1) * factor);
        int b = (int)(b1 + (b2 - b1) * factor);
        gc.fanVertex(px, py, r / 255.0F, g / 255.0F, b / 255.0F, a / 255.0F);
    }

    public static void drawGradientRoundedOutline(
        DrawContext context, float x, float y, float width, float height, float radius, float thickness, int color1, int color2
    ) {
        Gui$Color gc = Gui.color(context, 256);
        float trCenterX = x + width - radius;
        float trCenterY = y + radius;
        float tlCenterX = x + radius;
        float tlCenterY = y + radius;
        float blCenterX = x + radius;
        float blCenterY = y + height - radius;
        float brCenterX = x + width - radius;
        float brCenterY = y + height - radius;
        float arcLength = (float)(Math.PI * radius / 2.0);
        float topLength = width - 2.0F * radius;
        float leftLength = height - 2.0F * radius;
        float bottomLength = width - 2.0F * radius;
        float rightLength = height - 2.0F * radius;
        float totalPerimeter = arcLength * 4.0F + topLength + leftLength + bottomLength + rightLength;
        if (totalPerimeter <= 0.0F) {
            totalPerimeter = 1.0F;
        }

        float currentDist = 0.0F;

        for (int i = 0; i <= 12; i++) {
            float angle = (float)(0.0 - (Math.PI / 2) * i / 12.0);
            float dist = currentDist + arcLength * i / 12.0F;
            emitGradientOutlineVertices(gc, trCenterX, trCenterY, radius, thickness, angle, color1, color2, dist / totalPerimeter);
        }

        currentDist += arcLength;
        int topSegments = 1;

        for (int i = 1; i <= topSegments; i++) {
            float d = topLength * i / topSegments;
            float currX = trCenterX - d;
            float dist = currentDist + d;
            emitGradientOutlineVertices(gc, currX, trCenterY, radius, thickness, (float) (-Math.PI / 2), color1, color2, dist / totalPerimeter);
        }

        currentDist += topLength;

        for (int i = 0; i <= 12; i++) {
            float angle = (float)((-Math.PI / 2) - (Math.PI / 2) * i / 12.0);
            float dist = currentDist + arcLength * i / 12.0F;
            emitGradientOutlineVertices(gc, tlCenterX, tlCenterY, radius, thickness, angle, color1, color2, dist / totalPerimeter);
        }

        currentDist += arcLength;
        int leftSegments = 1;

        for (int i = 1; i <= leftSegments; i++) {
            float d = leftLength * i / leftSegments;
            float currY = tlCenterY + d;
            float dist = currentDist + d;
            emitGradientOutlineVertices(gc, tlCenterX, currY, radius, thickness, (float) -Math.PI, color1, color2, dist / totalPerimeter);
        }

        currentDist += leftLength;

        for (int i = 0; i <= 12; i++) {
            float angle = (float)(-Math.PI - (Math.PI / 2) * i / 12.0);
            float dist = currentDist + arcLength * i / 12.0F;
            emitGradientOutlineVertices(gc, blCenterX, blCenterY, radius, thickness, angle, color1, color2, dist / totalPerimeter);
        }

        currentDist += arcLength;
        int bottomSegments = 1;

        for (int i = 1; i <= bottomSegments; i++) {
            float d = bottomLength * i / bottomSegments;
            float currX = blCenterX + d;
            float dist = currentDist + d;
            emitGradientOutlineVertices(gc, currX, blCenterY, radius, thickness, (float) (-Math.PI * 3.0 / 2.0), color1, color2, dist / totalPerimeter);
        }

        currentDist += bottomLength;

        for (int i = 0; i <= 12; i++) {
            float angle = (float)((-Math.PI * 3.0 / 2.0) - (Math.PI / 2) * i / 12.0);
            float dist = currentDist + arcLength * i / 12.0F;
            emitGradientOutlineVertices(gc, brCenterX, brCenterY, radius, thickness, angle, color1, color2, dist / totalPerimeter);
        }

        currentDist += arcLength;
        int rightSegments = 1;

        for (int i = 1; i <= rightSegments; i++) {
            float d = rightLength * i / rightSegments;
            float currY = brCenterY - d;
            float dist = currentDist + d;
            emitGradientOutlineVertices(gc, brCenterX, currY, radius, thickness, (float) (-Math.PI * 2), color1, color2, dist / totalPerimeter);
        }

        gc.submit(context);
    }

    private static void emitGradientOutlineVertices(
        Gui$Color gc, float cx, float cy, float radius, float thickness, float angle, int c1, int c2, float progress
    ) {
        float cos = MathHelper.cos(angle);
        float sin = MathHelper.sin(angle);
        long time = System.currentTimeMillis();
        double speed = 0.003;
        double freq = 5.0;
        float factor = (float)(Math.sin(time * speed - progress * Math.PI * 2.0 * freq) + 1.0) / 2.0F;
        int color = interpolateColor(c1, c2, factor);
        float a = (color >> 24 & 0xFF) / 255.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float outX = cx + cos * radius;
        float outY = cy + sin * radius;
        float inX = cx + cos * (radius - thickness);
        float inY = cy + sin * (radius - thickness);
        gc.stripVertex(outX, outY, r, g, b, a);
        gc.stripVertex(inX, inY, r, g, b, a);
    }

    private static int interpolateColor(int c1, int c2, float factor) {
        int a1 = c1 >> 24 & 0xFF;
        int r1 = c1 >> 16 & 0xFF;
        int g1 = c1 >> 8 & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = c2 >> 24 & 0xFF;
        int r2 = c2 >> 16 & 0xFF;
        int g2 = c2 >> 8 & 0xFF;
        int b2 = c2 & 0xFF;
        int a = (int)(a1 + (a2 - a1) * factor);
        int r = (int)(r1 + (r2 - r1) * factor);
        int g = (int)(g1 + (g2 - g1) * factor);
        int b = (int)(b1 + (b2 - b1) * factor);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static void drawRoundedOutline(DrawContext context, float x, float y, float width, float height, float radius, float thickness, int color) {
        if (!(width <= 0.0F) && !(height <= 0.0F)) {
            float alpha = (color >> 24 & 0xFF) / 255.0F;
            float red = (color >> 16 & 0xFF) / 255.0F;
            float green = (color >> 8 & 0xFF) / 255.0F;
            float blue = (color & 0xFF) / 255.0F;
            float r = Math.min(radius, Math.min(width, height) * 0.5F);
            float t = Math.min(Math.max(thickness, 0.5F), Math.max(0.5F, r * 0.98F));
            Gui$Color gc = Gui.color(context, segmentsForRadius(r) * 32 + 80);
            float trCenterX = x + width - r;
            float trCenterY = y + r;
            float tlCenterX = x + r;
            float tlCenterY = y + r;
            float blCenterX = x + r;
            float blCenterY = y + height - r;
            float brCenterX = x + width - r;
            float brCenterY = y + height - r;
            float topLength = Math.max(0.0F, width - 2.0F * r);
            float leftLength = Math.max(0.0F, height - 2.0F * r);
            float bottomLength = Math.max(0.0F, width - 2.0F * r);
            float rightLength = Math.max(0.0F, height - 2.0F * r);
            int os = segmentsForRadius(r);

            for (int i = 0; i <= os; i++) {
                float angle = (float)(0.0 - (Math.PI / 2) * i / os);
                emitOutlineVertices(gc, trCenterX, trCenterY, r, t, angle, red, green, blue, alpha);
            }

            int topSegments = 1;

            for (int i = 1; i <= topSegments; i++) {
                float d = topLength * i / topSegments;
                float currX = trCenterX - d;
                emitOutlineVertices(gc, currX, trCenterY, r, t, (float) (-Math.PI / 2), red, green, blue, alpha);
            }

            for (int i = 0; i <= os; i++) {
                float angle = (float)((-Math.PI / 2) - (Math.PI / 2) * i / os);
                emitOutlineVertices(gc, tlCenterX, tlCenterY, r, t, angle, red, green, blue, alpha);
            }

            int leftSegments = 1;

            for (int i = 1; i <= leftSegments; i++) {
                float d = leftLength * i / leftSegments;
                float currY = tlCenterY + d;
                emitOutlineVertices(gc, tlCenterX, currY, r, t, (float) -Math.PI, red, green, blue, alpha);
            }

            for (int i = 0; i <= os; i++) {
                float angle = (float)(-Math.PI - (Math.PI / 2) * i / os);
                emitOutlineVertices(gc, blCenterX, blCenterY, r, t, angle, red, green, blue, alpha);
            }

            int bottomSegments = 1;

            for (int i = 1; i <= bottomSegments; i++) {
                float d = bottomLength * i / bottomSegments;
                float currX = blCenterX + d;
                emitOutlineVertices(gc, currX, blCenterY, r, t, (float) (-Math.PI * 3.0 / 2.0), red, green, blue, alpha);
            }

            for (int i = 0; i <= os; i++) {
                float angle = (float)((-Math.PI * 3.0 / 2.0) - (Math.PI / 2) * i / os);
                emitOutlineVertices(gc, brCenterX, brCenterY, r, t, angle, red, green, blue, alpha);
            }

            int rightSegments = 1;

            for (int i = 1; i <= rightSegments; i++) {
                float d = rightLength * i / rightSegments;
                float currY = brCenterY - d;
                emitOutlineVertices(gc, brCenterX, currY, r, t, (float) (-Math.PI * 2), red, green, blue, alpha);
            }

            gc.submit(context);
        }
    }

    public static void drawSmoothRoundedOutline(DrawContext context, float x, float y, float width, float height, float radius, float thickness, int color) {
        drawRoundedOutline(context, x, y, width, height, radius, thickness, color);
        float feather = Math.min(Math.min(width, height) * 0.12F, Math.max(0.85F, edgeFeather(context, width, height) * 3.0F));
        drawSmoothAA(context, x, y, width, height, radius, color, color, false, feather);
    }

    public static void drawAntialiasedRoundedOutline(DrawContext context, float x, float y, float width, float height, float radius, float thickness, int color) {
        if (!(width <= 0.0F) && !(height <= 0.0F) && (color >>> 24 & 0xFF) != 0) {
            float r = Math.min(radius, Math.min(width, height) * 0.5F);
            float t = Math.min(Math.max(thickness, 0.5F), Math.max(0.5F, r * 0.98F));
            float feather = Math.min(t * 0.42F, edgeFeather(context, width, height));
            int transparent = color & 16777215;
            drawRoundedBand(context, x, y, width, height, r, r + feather, r, transparent, color);
            drawRoundedOutline(context, x, y, width, height, r, t, color);
            drawRoundedBand(context, x, y, width, height, r, Math.max(0.0F, r - t), Math.max(0.0F, r - t - feather), color, transparent);
        }
    }

    private static void drawRoundedBand(
        DrawContext context,
        float x,
        float y,
        float width,
        float height,
        float centerRadius,
        float outerRadius,
        float innerRadius,
        int outerColor,
        int innerColor
    ) {
        float trX = x + width - centerRadius;
        float trY = y + centerRadius;
        float tlX = x + centerRadius;
        float tlY = y + centerRadius;
        float blX = x + centerRadius;
        float blY = y + height - centerRadius;
        float brX = x + width - centerRadius;
        float brY = y + height - centerRadius;
        int segments = segmentsForRadius(Math.max(outerRadius, innerRadius));
        Gui$Color strip = Gui.color(context, segments * 32 + 80);
        appendBandArc(strip, trX, trY, outerRadius, innerRadius, 0, outerColor, innerColor, segments);
        appendBandVertex(strip, tlX, tlY, outerRadius, innerRadius, (float) (-Math.PI / 2), outerColor, innerColor);
        appendBandArc(strip, tlX, tlY, outerRadius, innerRadius, -1, outerColor, innerColor, segments);
        appendBandVertex(strip, blX, blY, outerRadius, innerRadius, (float) -Math.PI, outerColor, innerColor);
        appendBandArc(strip, blX, blY, outerRadius, innerRadius, -2, outerColor, innerColor, segments);
        appendBandVertex(strip, brX, brY, outerRadius, innerRadius, (float) (-Math.PI * 3.0 / 2.0), outerColor, innerColor);
        appendBandArc(strip, brX, brY, outerRadius, innerRadius, -3, outerColor, innerColor, segments);
        appendBandVertex(strip, trX, trY, outerRadius, innerRadius, (float) (-Math.PI * 2), outerColor, innerColor);
        strip.submit(context);
    }

    private static void appendBandArc(
        Gui$Color strip, float cx, float cy, float outerRadius, float innerRadius, int quadrant, int outerColor, int innerColor, int segments
    ) {
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(quadrant * Math.PI / 2.0 - (Math.PI / 2) * i / segments);
            appendBandVertex(strip, cx, cy, outerRadius, innerRadius, angle, outerColor, innerColor);
        }
    }

    private static void appendBandVertex(Gui$Color strip, float cx, float cy, float outerRadius, float innerRadius, float angle, int outerColor, int innerColor) {
        float cos = MathHelper.cos(angle);
        float sin = MathHelper.sin(angle);
        strip.stripVertex(cx + cos * outerRadius, cy + sin * outerRadius, outerColor);
        strip.stripVertex(cx + cos * innerRadius, cy + sin * innerRadius, innerColor);
    }

    public static void drawArc(
        DrawContext context, float centerX, float centerY, float innerRadius, float outerRadius, float startDegrees, float sweepDegrees, int color
    ) {
        if (!(sweepDegrees <= 0.0F) && !(outerRadius <= innerRadius)) {
            float alpha = (color >> 24 & 0xFF) / 255.0F;
            float red = (color >> 16 & 0xFF) / 255.0F;
            float green = (color >> 8 & 0xFF) / 255.0F;
            float blue = (color & 0xFF) / 255.0F;
            float feather = arcFeather(context, outerRadius);
            float solidOuter = Math.max(innerRadius, outerRadius - feather);
            float solidInner = Math.min(solidOuter, innerRadius + feather);
            int segments = Math.max(8, (int)Math.ceil(Math.abs(sweepDegrees) / 10.0F));
            float start = (float)Math.toRadians(startDegrees);
            float sweep = (float)Math.toRadians(sweepDegrees);
            Gui$Color batch = Gui.color(context, segments * 12);
            int solidColor = color;
            int transparentColor = color & 16777215;
            float fadedInner = Math.max(0.0F, innerRadius - feather);
            float fadedOuter = outerRadius + feather;

            for (int i = 0; i < segments; i++) {
                float angle0 = start + sweep * i / segments;
                float angle1 = start + sweep * (i + 1) / segments;
                float cos0 = MathHelper.cos(angle0);
                float sin0 = MathHelper.sin(angle0);
                float cos1 = MathHelper.cos(angle1);
                float sin1 = MathHelper.sin(angle1);
                float inner0X = centerX + cos0 * solidInner;
                float inner0Y = centerY + sin0 * solidInner;
                float inner1X = centerX + cos1 * solidInner;
                float inner1Y = centerY + sin1 * solidInner;
                float outer0X = centerX + cos0 * solidOuter;
                float outer0Y = centerY + sin0 * solidOuter;
                float outer1X = centerX + cos1 * solidOuter;
                float outer1Y = centerY + sin1 * solidOuter;
                batch.quad(inner0X, inner0Y, outer0X, outer0Y, outer1X, outer1Y, inner1X, inner1Y, solidColor);
                batch.vertex(outer0X, outer0Y, solidColor)
                    .vertex(centerX + cos0 * fadedOuter, centerY + sin0 * fadedOuter, transparentColor)
                    .vertex(centerX + cos1 * fadedOuter, centerY + sin1 * fadedOuter, transparentColor)
                    .vertex(outer1X, outer1Y, solidColor);
                batch.vertex(centerX + cos0 * fadedInner, centerY + sin0 * fadedInner, transparentColor)
                    .vertex(inner0X, inner0Y, solidColor)
                    .vertex(inner1X, inner1Y, solidColor)
                    .vertex(centerX + cos1 * fadedInner, centerY + sin1 * fadedInner, transparentColor);
            }

            batch.submit(context);
        }
    }

    private static int segmentsForRadius(float radius) {
        return MathHelper.clamp((int)Math.ceil(Math.max(0.0F, radius)), 4, 12);
    }

    private static float arcCos(int quadrant, int segments, int index) {
        return ARC_COS[MathHelper.clamp(-quadrant, 0, 3)][segments][index];
    }

    private static float arcSin(int quadrant, int segments, int index) {
        return ARC_SIN[MathHelper.clamp(-quadrant, 0, 3)][segments][index];
    }

    private static float arcFeather(DrawContext context, float radius) {
        Matrix3x2fc matrix = context.getMatrices();
        float determinant = Math.abs(matrix.m00() * matrix.m11() - matrix.m01() * matrix.m10());
        float scale = (float)Math.sqrt(determinant);
        float feather = scale > 1.0E-4F ? 0.65F / scale : 0.65F;
        return Math.min(feather, Math.max(0.2F, radius * 0.22F));
    }

    private static void appendArcEdge(
        DrawContext context,
        float centerX,
        float centerY,
        float solidRadius,
        float fadedRadius,
        float start,
        float sweep,
        int segments,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        Gui$Color gc = Gui.color(context, segments * 8 + 16);

        for (int i = 0; i <= segments; i++) {
            float angle = start + sweep * i / segments;
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            gc.stripVertex(centerX + cos * solidRadius, centerY + sin * solidRadius, red, green, blue, alpha);
            gc.stripVertex(centerX + cos * fadedRadius, centerY + sin * fadedRadius, red, green, blue, 0.0F);
        }

        gc.submit(context);
    }

    public static void drawSmoothCircleSelectionRing(DrawContext context, float cx, float cy, float midRadius, int color) {
        float baseA = (color >> 24 & 0xFF) / 255.0F;
        if (baseA <= 0.0F) {
            baseA = 1.0F;
        }

        float rCol = (color >> 16 & 0xFF) / 255.0F;
        float gCol = (color >> 8 & 0xFF) / 255.0F;
        float bCol = (color & 0xFF) / 255.0F;
        int segments = MathHelper.clamp((int)Math.ceil(midRadius * 4.0F), 32, 96);
        float coreHalf = 0.36F;
        float rIn = Math.max(0.18F, midRadius - coreHalf);
        float rOut = midRadius + coreHalf;
        float rFeather = rOut + 0.38F;
        Gui$Color gc = Gui.color(context);

        for (int i = 0; i <= segments; i++) {
            double ang = (Math.PI * 2) * i / segments;
            float c = MathHelper.cos((float)ang);
            float s = MathHelper.sin((float)ang);
            gc.stripVertex(cx + c * rIn, cy + s * rIn, rCol, gCol, bCol, baseA * 0.92F);
            gc.stripVertex(cx + c * rOut, cy + s * rOut, rCol, gCol, bCol, baseA);
        }

        gc.submit(context);
        Gui$Color gc2 = Gui.color(context, segments * 8 + 16);

        for (int i = 0; i <= segments; i++) {
            double ang = (Math.PI * 2) * i / segments;
            float co = MathHelper.cos((float)ang);
            float si = MathHelper.sin((float)ang);
            gc2.stripVertex(cx + co * rOut, cy + si * rOut, rCol, gCol, bCol, baseA);
            gc2.stripVertex(cx + co * rFeather, cy + si * rFeather, rCol, gCol, bCol, 0.0F);
        }

        gc2.submit(context);
    }

    public static void drawSmoothFilledCircle(DrawContext context, float cx, float cy, float radius, int color) {
        if (!(radius <= 0.0F) && (color >>> 24 & 0xFF) != 0) {
            int segments = MathHelper.clamp((int)Math.ceil(radius * 6.0F), 24, 64);
            float feather = arcFeather(context, radius);
            float solidRadius = Math.max(0.25F, radius - feather);
            Gui$Color fill = Gui.color(context, segments * 4 + 8);
            fill.fanVertex(cx, cy, color);

            for (int index = 0; index <= segments; index++) {
                float angle = (float)((Math.PI * 2) * index / segments);
                fill.fanVertex(cx + MathHelper.cos(angle) * solidRadius, cy + MathHelper.sin(angle) * solidRadius, color);
            }

            fill.submit(context);
            int transparent = color & 16777215;
            Gui$Color edge = Gui.color(context, segments * 8 + 16);

            for (int index = 0; index <= segments; index++) {
                float angle = (float)((Math.PI * 2) * index / segments);
                float cos = MathHelper.cos(angle);
                float sin = MathHelper.sin(angle);
                edge.stripVertex(cx + cos * solidRadius, cy + sin * solidRadius, color);
                edge.stripVertex(cx + cos * radius, cy + sin * radius, transparent);
            }

            edge.submit(context);
        }
    }

    private static void emitOutlineVertices(Gui$Color gc, float cx, float cy, float radius, float thickness, float angle, float r, float g, float b, float a) {
        float cos = MathHelper.cos(angle);
        float sin = MathHelper.sin(angle);
        float outX = cx + cos * radius;
        float outY = cy + sin * radius;
        float inX = cx + cos * (radius - thickness);
        float inY = cy + sin * (radius - thickness);
        gc.stripVertex(outX, outY, r, g, b, a);
        gc.stripVertex(inX, inY, r, g, b, a);
    }

    static {
        for (int quadrantIndex = 0; quadrantIndex < 4; quadrantIndex++) {
            int quadrant = -quadrantIndex;

            for (int segments = 4; segments <= 12; segments++) {
                for (int i = 0; i <= segments; i++) {
                    double angle = quadrant * Math.PI / 2.0 - (Math.PI / 2) * i / segments;
                    ARC_COS[quadrantIndex][segments][i] = (float)Math.cos(angle);
                    ARC_SIN[quadrantIndex][segments][i] = (float)Math.sin(angle);
                }
            }
        }
    }
}
