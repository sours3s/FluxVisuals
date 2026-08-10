package ru.fluxvisuals.api.render;

import lombok.experimental.UtilityClass;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

@UtilityClass
public class VertexUtility {
    public void quad(VertexConsumer v,
                     MatrixStack.Entry mat,
                     float x1, float y1, float z1,
                     float x2, float y2, float z2,
                     float x3, float y3, float z3,
                     float x4, float y4, float z4,
                     Color color1, Color color2, Color color3, Color color4) {
        v.vertex(mat, x1, y1, z1).color(color1.getRGB())
                .vertex(mat, x2, y2, z2).color(color2.getRGB())
                .vertex(mat, x3, y3, z3).color(color3.getRGB())
                .vertex(mat, x4, y4, z4).color(color4.getRGB());
    }

    public void quadTextured(VertexConsumer v,
                             MatrixStack.Entry mat,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             Color color1, Color color2, Color color3, Color color4) {
        v.vertex(mat, x1, y1, z1).color(color1.getRGB()).texture(0, 0)
                .vertex(mat, x2, y2, z2).color(color2.getRGB()).texture(0, 1)
                .vertex(mat, x3, y3, z3).color(color3.getRGB()).texture(1, 1)
                .vertex(mat, x4, y4, z4).color(color4.getRGB()).texture(1, 0);
    }
}
