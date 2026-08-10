package ru.fluxvisuals.vse.utils.math;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.Random;

import static ru.fluxvisuals.MinecraftHolder.mc;
import static ru.fluxvisuals.MinecraftHolder.window;

@UtilityClass
public class MathUtility {
    public static final Matrix4f lastProjMat = new Matrix4f();
    public static final Matrix4f lastModMat = new Matrix4f();
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();
    public static MatrixStack worldStack = new MatrixStack();

    public static final double TO_DEGREES = 180d / Math.PI;
    public static final double TO_RADIANS = Math.PI / 180d;
    public static final float TO_RADIANS_F = (float)Math.PI / 180f;

    public static boolean mouseIn(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public double squared(double value) {
        return value * value;
    }

    public float squaref(float value) {
        return value * value;
    }

    public float clampRot(float delta, float value) {
        return Math.min(Math.abs(delta), value);
    }

    public float getMaxOrMin(float value, boolean isMax) {
        return isMax ? Math.max(1, value) : Math.min(1, value);
    }

    public float getMaxOrMin(float value, float value2, boolean isMax) {
        return isMax ? Math.max(value, value2) : Math.min(value, value2);
    }

    TimeUtility timeUtility = new TimeUtility();

    public float logicalRandom(float first, float second, int MS, float chance) {
        return timeUtility.reached(MS, true) && Math.random() > chance ? first : second;
    }

    public static float fastAnim(float end, float start, float multiple) {
        float clampedDelta = MathHelper.clamp(deltaTime() * multiple, 0.05F, 1f);
        return (1f - clampedDelta) * end + clampedDelta * start;
    }

    public static float deltaTime() {
        return MinecraftClient.getInstance().getCurrentFps() > 0 ? 1F / MinecraftClient.getInstance().getCurrentFps() : 1F;
    }

    public static float linearFps(float source, float target, float amount) {
        return linear(source, target, amount * (1F / Math.max(1, MinecraftClient.getInstance().getCurrentFps())));
    }

    public static float linear(float source, float target, float amount) {
        if (Float.isNaN(source) || Float.isNaN(target) || Float.isNaN(amount)) return 0;
        return source + (target - source) * amount;
    }

    public static void scale(MatrixStack stack, float x, float y, float scale) {
        scale(stack, x, y, scale, scale);
    }


    public static void scale(MatrixStack stack, float x, float y, float scaleX, float scaleY) {
        stack.translate(scaledX(x), scaledY(y), 0);
        stack.scale(scaleX, scaleY, 1);
        stack.translate(-scaledX(x), -scaledY(y), 0);
    }

    public static float scaledX(float f) {
        return f / ((float) window.getScaledWidth() / window.getWidth());
    }

    public static float scaledY(float f) {
        return f / ((float) window.getScaledHeight() / window.getHeight());
    }

    public static float approach(float current, float target, float speed) {
        return current < target ? Math.min(current + speed, target) : Math.max(current - speed, target);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    private static float last = 0;
    private static final Random random = new Random();

    public static boolean randomBoolean() {
        return random.nextBoolean();
    }

    public static float randomFloat() {
        return random.nextFloat();
    }

    public static double random(double min, double max) {
        if (min == max) return min;
        return random.nextDouble() * (max - min) + min;
    }

    public static float random(float min, float max, int MS) {
        if (min == max) return min;
        if (last == 0) {
            last = (float) random.nextDouble(min, max);
        }
        if (timeUtility.reached(MS, true)) {
            last = (float) random.nextDouble(min, max);
        }

        return last;
    }

    public static int random(int min, int max) {
        if (min == max) return min;
        return random.nextInt(min, max);
    }

    public static float gaussian(float min, float max) {
        if (min == max) return min;

        float mean = (min + max) / 2;
        float stdDev = (max - min) / 6;

        float value;
        do {
            value = (float) (random.nextGaussian() * stdDev + mean);
        } while (value < min || value > max);

        return value;
    }

    public static long random(long min, long max) {
        if (min == max) return min;
        return random.nextLong(min, max);
    }

    public static float random(float min, float max) {
        if (min == max) return min;
        return (float) (random.nextDouble() * (max - min) + min);
    }
    
    public static float randomValue(float min, float max) {
        return random(min, max);
    }
    
    public static float step(float value, float step) {
        return Math.round(value / step) * step;
    }

    public static float delta(float a, float b) {
        a = Math.abs(a);
        b = Math.abs(b);
        return Math.max(a, b) - Math.min(a, b);
    }

    public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        int displayHeight = mc.getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        double deltaX = pos.x - camera.getCameraPos().x;
        double deltaY = pos.y - camera.getCameraPos().y;
        double deltaZ = pos.z - camera.getCameraPos().z;

        Vector4f transformedCoordinates = new Vector4f((float) deltaX, (float) deltaY, (float) deltaZ, 1.f).mul(lastWorldSpaceMatrix);
        Matrix4f matrixProj = new Matrix4f(lastProjMat);
        Matrix4f matrixModel = new Matrix4f(lastModMat);
        matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);

        return new Vec3d(target.x / getScaleFactor(), (displayHeight - target.y) / getScaleFactor(), target.z);
    }

    public static double getScaleFactor() {
        return mc.getWindow().getScaleFactor();
    }

    public static Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);

        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    public static Vec3d clampToBox(Vec3d vec3d, Box box) {
        return new Vec3d(
                MathHelper.clamp(vec3d.x, box.minX, box.maxX),
                MathHelper.clamp(vec3d.y, box.minY, box.maxY),
                MathHelper.clamp(vec3d.z, box.minZ, box.maxZ)
        );
    }

    public static double forwardX(float yaw, double speed) {
        return Math.sin((yaw + 90f) * TO_RADIANS) * speed;
    }

    public static double forwardZ(float yaw, double speed) {
        return Math.cos((yaw + 90f) * TO_RADIANS) * speed;
    }

    public static Vec3d copy(Vec3d vec3d) {
        return new Vec3d(vec3d.x, vec3d.y, vec3d.z);
    }

    public static Vec3d strafe(float yaw, double speed) {
        return new Vec3d(-Math.sin(yaw) * speed, 0, Math.cos(yaw) * speed);
    }
}
