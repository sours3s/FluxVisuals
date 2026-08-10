package ru.fluxvisuals.vse.utils.math;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Rectangle {
    private float x, y, width, height;
    public Rectangle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    public Rectangle() {}

    public Rectangle center(float x, float y) {
        this.x = x - this.width / 2;
        this.y = y - this.height / 2;
        return this;
    }
    public Rectangle bound(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public boolean hovered(int mouseX, int mouseY) {
        return MathUtility.mouseIn(x, y, width, height, mouseX, mouseY);
    }
}
