package ru.fluxvisuals.module.visual;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.config.Setting;
import ru.fluxvisuals.module.Category;
import ru.fluxvisuals.module.Module;
import ru.fluxvisuals.render.ColorUtils;
import ru.fluxvisuals.render.Theme;
import ru.fluxvisuals.render.WorldRenderUtils;

import java.util.ArrayDeque;
import java.util.Deque;

/** Позиционный след за игроком. */
public class TrailModule extends Module {
    private final Setting color = Setting.color("Color", "Цвет следа", Theme.accent());
    private final Setting length = Setting.int_("Length", "Длина следа", 30, 5, 80);
    private final Deque<Vec3d> positions = new ArrayDeque<>();
    private int tickCount;

    public TrailModule() {
        super("Trail", "Позиционный след", Category.VISUAL);
        addSetting(color);
        addSetting(length);
    }

    @Override
    public void onTick() {
        tickCount++;
        if (tickCount % 2 != 0) return; // каждые 2 тика
        var p = mc().player;
        if (p == null) return;
        positions.addLast(WorldRenderUtils.entityPos(p).add(0, 0.05, 0));
        while (positions.size() > length.getInt()) positions.pollFirst();
    }

    @Override
    public void onRender3D(MatrixStack ms, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        if (positions.size() < 2) return;
        int maxLen = positions.size();
        int i = 0;
        Vec3d prev = null;
        for (Vec3d pos : positions) {
            if (prev != null) {
                float alpha = (float) i / maxLen;
                int col = ColorUtils.mulAlpha(color.getColor(), alpha * 0.7f);
                WorldRenderUtils.drawLine3D(ms, consumers, camera, prev, pos, col);
            }
            prev = pos;
            i++;
        }
    }
}
