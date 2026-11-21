package ru.fluxvisuals.ui.fluxgui.module.render;

import java.util.ArrayList;
import java.util.List;
import ru.fluxvisuals.ui.fluxgui.setting.ColorSetting;

/** Хранилище меток мира для GUI FluxVisuals. */
public final class Markers {
    public static final char[] MARK_ICON_CHARS = new char[]{'a', 'b', 'c', 'd', 'e', 'f'};
    public static final ColorSetting iconBackgroundColor = new ColorSetting("Фон иконки", 0x80000000);

    private final List<Markers$MarkerData> markers = new ArrayList<>();

    public List<Markers$MarkerData> getMarkers() {
        return markers;
    }

    public void addMarker(Markers$MarkerData marker) {
        markers.add(marker);
    }

    public static int opaqueColor(int color) {
        return 0xFF000000 | color;
    }

    public static int getIconBackgroundColor() {
        return iconBackgroundColor.getValue();
    }

    public static int getIconBackgroundGradientTop(int color) {
        return color;
    }

    public static int getIconBackgroundGradientBottom(int color) {
        return color;
    }
}
