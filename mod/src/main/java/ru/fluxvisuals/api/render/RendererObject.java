package ru.fluxvisuals.api.render;

import ru.fluxvisuals.MinecraftHolder;
import lombok.Getter;
import ru.fluxvisuals.vse.utils.math.MathUtility;

@Getter
public abstract class RendererObject implements MinecraftHolder {
    protected float x;
    protected float y;
    protected float width;
    protected float height;

    public RendererObject bound(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        if (height >= 0)
            this.height = height;
        return this;
    }

    public boolean hover(int mouseX, int mouseY) {
        return MathUtility.mouseIn(x, y, width, height, mouseX, mouseY);
    }

    public abstract void render(int mouseX, int mouseY);
    public boolean click(int mouseX, int mouseY, int button) { return false; }
    public void chartyped(char ch, int keyCode) {}
    public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    public void release(int button) {}
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {return false;}
    public void mouseDragged(int mouseX, int mouseY) { }
}
