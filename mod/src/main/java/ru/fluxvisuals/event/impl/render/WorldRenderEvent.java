package ru.fluxvisuals.event.impl.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class WorldRenderEvent {
    private final MinecraftClient client;
    private final GameRenderer gameRenderer;
    private final Camera camera;
    private final RenderTickCounter tickCounter;
    private final float frameDepth;
    private final Matrix4f positionMatrix;
    private final Matrix4f projectionMatrix;

    public WorldRenderEvent() {
        this.client = MinecraftClient.getInstance();
        this.gameRenderer = null;
        this.camera = null;
        this.tickCounter = null;
        this.frameDepth = 0;
        this.positionMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
    }

    public WorldRenderEvent(MinecraftClient client, GameRenderer gameRenderer, Camera camera, RenderTickCounter tickCounter, Matrix4f positionMatrix, Matrix4f projectionMatrix, float frameDepth) {
        this.client = client;
        this.gameRenderer = gameRenderer;
        this.camera = camera;
        this.tickCounter = tickCounter;
        this.frameDepth = frameDepth;
        this.positionMatrix = positionMatrix;
        this.projectionMatrix = projectionMatrix;
    }

    public MinecraftClient client() { return client; }
    public GameRenderer gameRenderer() { return gameRenderer; }
    public Camera getCamera() { return camera; }
    public RenderTickCounter getTickCounter() { return tickCounter; }
    public float frameDepth() { return frameDepth; }
    public Matrix4f getPositionMatrix() { return positionMatrix; }
    public Matrix4f getProjectionMatrix() { return projectionMatrix; }
    public MatrixStack matrixStack() {
        MatrixStack ms = new MatrixStack();
        if (positionMatrix != null) ms.multiplyPositionMatrix(positionMatrix);
        return ms;
    }
}
