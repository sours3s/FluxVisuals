package ru.fluxvisuals.ui.fluxgui.util.animation;

import net.minecraft.util.math.MathHelper;

public class AnimationUtil {
    private static final float MAX_STEP_SECONDS = 0.25F;

    public static float smoothRate(float target, float current, float rate, float deltaSeconds) {
        if (Math.abs(current - target) < 5.0E-4F) {
            return target;
        }

        float dt = MathHelper.clamp(deltaSeconds, 0.0F, 0.25F);
        float t = 1.0F - (float)Math.exp(-rate * dt);
        return t >= 0.99999F ? target : current + (target - current) * t;
    }

    public static float smoothScroll(float target, float current, float rate, float deltaSeconds) {
        float dt = MathHelper.clamp(deltaSeconds, 0.0F, 0.25F);
        float t = 1.0F - (float)Math.exp(-rate * dt);
        return Math.abs(current - target) < 0.75F ? target : current + (target - current) * t;
    }

    public static float smoothLerp(float target, float current, float factorPerFrameAt60, float deltaSeconds) {
        if (Math.abs(current - target) < 5.0E-4F) {
            return target;
        }

        float dt = MathHelper.clamp(deltaSeconds, 0.0F, 0.25F);
        float f = MathHelper.clamp(factorPerFrameAt60, 1.0E-4F, 0.999F);
        float steps = dt * 60.0F;
        float t = 1.0F - (float)Math.pow(1.0F - f, steps);
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        return current + (target - current) * t;
    }

    public static float animate(float target, float current, float speed) {
        return Math.abs(current - target) < 5.0E-4F ? target : current + (target - current) * speed;
    }

    public static float easeOutBack(float x) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float f = x - 1.0F;
        return 1.0F + c3 * f * f * f + c1 * f * f;
    }
}
