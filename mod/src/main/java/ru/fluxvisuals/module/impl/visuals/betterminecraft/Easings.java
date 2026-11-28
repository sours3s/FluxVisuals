package ru.fluxvisuals.module.impl.visuals.betterminecraft;

public final class Easings {
    public static final Easing LINEAR = value -> value;
    public static final Easing QUAD_OUT = value -> 1.0 - Math.pow(1.0 - value, 2.0);
    public static final Easing CUBIC_OUT = value -> 1.0 - Math.pow(1.0 - value, 3.0);
    public static final Easing QUART_OUT = value -> 1.0 - Math.pow(1.0 - value, 4.0);
    
    private Easings() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
