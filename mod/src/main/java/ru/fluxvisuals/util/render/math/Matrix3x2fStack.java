package ru.fluxvisuals.util.render.math;

import org.joml.Matrix4f;
import org.joml.Matrix3x2f;

import java.util.Stack;

public class Matrix3x2fStack {
    private final Stack<Matrix4f> stack = new Stack<>();

    public Matrix3x2fStack() {
        stack.push(new Matrix4f().identity());
    }

    public Matrix4f peek() {
        return stack.peek();
    }

    public void pushMatrix() {
        stack.push(new Matrix4f(stack.peek()));
    }

    public void popMatrix() {
        if (stack.size() > 1) {
            stack.pop();
        }
    }

    public void translate(float x, float y) {
        Matrix4f current = stack.peek();
        current.translate(x, y, 0);
    }

    public void translate(float x, float y, float z) {
        Matrix4f current = stack.peek();
        current.translate(x, y, z);
    }

    public void scale(float x, float y) {
        Matrix4f current = stack.peek();
        current.scale(x, y, 1);
    }

    public void scale(float x, float y, float z) {
        Matrix4f current = stack.peek();
        current.scale(x, y, z);
    }

    public void identity() {
        stack.peek().identity();
    }

    public void mul(Matrix4f matrix) {
        stack.peek().mul(matrix);
    }

    public void rotateZ(float angle) {
        stack.peek().rotateZ(angle);
    }
}