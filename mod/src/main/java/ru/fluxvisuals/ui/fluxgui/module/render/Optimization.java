package ru.fluxvisuals.ui.fluxgui.module.render;

/** Разрешение частоты захвата кадра для стеклянного фона GUI. */
public final class Optimization {
    private static volatile long captureCoalesceNanos = 16_000_000L;

    private Optimization() {
    }

    public static long glassCaptureCoalesceNanos() {
        return captureCoalesceNanos;
    }

    public static void setGlassCaptureCoalesceNanos(long nanos) {
        captureCoalesceNanos = nanos;
    }
}
