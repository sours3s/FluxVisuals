package ru.fluxvisuals.module.hud;

/** Счётчик кадров в секунду. */
public class FpsModule extends SimpleHudModule {
    private int frames;
    private long lastTime;
    private int fps;

    public FpsModule() {
        super("FPS", "Счётчик кадров в секунду");
    }

    @Override
    protected String getText() {
        frames++;
        long now = System.currentTimeMillis();
        if (now - lastTime >= 500) {
            fps = (int) (frames * 1000.0 / (now - lastTime));
            frames = 0;
            lastTime = now;
        }
        return "FPS " + fps;
    }
}
