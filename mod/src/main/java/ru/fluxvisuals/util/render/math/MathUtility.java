package ru.fluxvisuals.util.render.math;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.MinecraftClient;

import java.util.Random;

@Environment(EnvType.CLIENT)
public class MathUtility {
    public static final float TO_RADIANS_F = (float)Math.PI / 180f;

    private static final Random random = new Random();

    public static boolean mouseIn(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static float fastAnim(float current, float target, float multiple) {
        float deltaTime = deltaTime();
        float clampedDelta = MathHelper.clamp(deltaTime * multiple, 0.05F, 1f);
        return (1f - clampedDelta) * current + clampedDelta * target;
    }

    public static float deltaTime() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.getCurrentFps() > 0 ? 1F / client.getCurrentFps() : 1F;
    }

    public static float linear(float source, float target, float amount) {
        if (Float.isNaN(source) || Float.isNaN(target) || Float.isNaN(amount)) return 0;
        return source + (target - source) * amount;
    }

    public static float approach(float current, float target, float speed) {
        return current < target ? Math.min(current + speed, target) : Math.max(current - speed, target);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}