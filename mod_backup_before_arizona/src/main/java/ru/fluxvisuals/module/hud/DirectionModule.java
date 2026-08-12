package ru.fluxvisuals.module.hud;

/** Компас: сторона света и градусы. */
public class DirectionModule extends SimpleHudModule {
    public DirectionModule() {
        super("Direction", "Компас и направление");
    }

    @Override
    protected String getText() {
        var p = mc().player;
        if (p == null) return null;
        float yaw = p.getYaw() % 360f;
        if (yaw < 0) yaw += 360f;
        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int idx = ((int) Math.floor((yaw + 22.5f) / 45f)) & 7;
        return "Facing " + dirs[idx] + " " + (int) yaw + "°";
    }
}
