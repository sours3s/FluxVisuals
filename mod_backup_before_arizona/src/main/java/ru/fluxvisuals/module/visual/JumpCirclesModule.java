package ru.fluxvisuals.module.visual;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.Animation;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.Theme;
import ru.fluxvisuals.render.WorldRenderUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Расширяющееся кольцо у ног при прыжке. */
public class JumpCirclesModule extends Module {
    private final Setting color = Setting.color("Color", "Цвет кольца", Theme.accent());
    private boolean wasOnGround = true;
    private final List<Circle> circles = new ArrayList<>();

    public JumpCirclesModule() {
        super("JumpCircles", "Кольца при прыжке", Category.VISUAL);
        addSetting(color);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        boolean onGround = p.isOnGround();
        if (wasOnGround && !onGround) {
            circles.add(new Circle(WorldRenderUtils.entityPos(p).add(0, 0.05, 0)));
        }
        wasOnGround = onGround;
    }

    @Override
    public void onRender3D(MatrixStack ms, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        long now = System.currentTimeMillis();
        Iterator<Circle> it = circles.iterator();
        while (it.hasNext()) {
            Circle c = it.next();
            double age = (now - c.born) / 1000.0;
            if (age > c.lifetime) { it.remove(); continue; }
            float radius = (float) (0.3 + age * 3.0);
            float alpha = (float) (1.0 - age / c.lifetime);
            int col = ColorUtils.mulAlpha(color.getColor(), alpha);
            WorldRenderUtils.drawRing3D(ms, consumers, camera, c.pos, radius, 0f, col, 40);
        }
    }

    private static class Circle {
        final Vec3d pos;
        final long born = System.currentTimeMillis();
        final double lifetime = 0.8;
        Circle(Vec3d pos) { this.pos = pos; }
    }
}
