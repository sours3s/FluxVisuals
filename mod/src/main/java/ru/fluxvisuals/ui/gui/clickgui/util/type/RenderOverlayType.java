package ru.fluxvisuals.ui.gui.clickgui.util.type;

public class RenderOverlayType {
    public static RenderOverlayType CAMERA_HURT;
    public static RenderOverlayType FIRE_OVERLAY;
    public static RenderOverlayType GLOWING;
    public static RenderOverlayType LAVA_OVERLAY;
    public static RenderOverlayType SCOREBOARD;
    public static RenderOverlayType TOTEM_POP;
    public static RenderOverlayType WITHER_HEARTS;

    public boolean isBossBar() {
        return false;
    }

    public boolean isCameraHurt() {
        return false;
    }

    public boolean isFireOverlay() {
        return false;
    }

    public boolean isGlowing() {
        return false;
    }

    public boolean isLavaOverlay() {
        return false;
    }

    public boolean isScoreboard() {
        return false;
    }

    public boolean isTotemPop() {
        return false;
    }

    public boolean isWitherHearts() {
        return false;
    }
}
